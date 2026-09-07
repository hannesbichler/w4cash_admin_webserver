package w4cash.product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import w4cash.LoadDatabase;

@Component
public class ProductsRepository {

	private static final String SELECT_COLUMNS = "P.ID, P.REFERENCE, P.CODE, P.NAME, P.PRICEBUY, P.PRICESELL, P.TAXCAT, P.CATEGORY, P.UNIT, "
			+
			"P.ATTRIBUTESET_ID, CASE WHEN C.PRODUCT IS NULL THEN 0 ELSE 1 END AS ACTIVE";
	// The catalog membership carries the active flag, so every read joins
	// PRODUCTS_CAT.
	private static final String SELECT_FROM = " FROM PRODUCTS P LEFT JOIN PRODUCTS_CAT C ON P.ID = C.PRODUCT";
	private static final String SELECT_ALL = "SELECT " + SELECT_COLUMNS + SELECT_FROM + " ORDER BY P.NAME";
	private static final String SELECT_BY_CATEGORY = "SELECT " + SELECT_COLUMNS + SELECT_FROM
			+ " WHERE P.CATEGORY = ? ORDER BY P.NAME";
	private static final String SELECT_BY_ID = "SELECT " + SELECT_COLUMNS + SELECT_FROM + " WHERE P.ID = ?";

	private static final String INSERT_SQL = "INSERT INTO PRODUCTS (ID, REFERENCE, CODE, NAME, PRICEBUY, PRICESELL, TAXCAT, CATEGORY, UNIT, ATTRIBUTESET_ID) "
			+
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String UPDATE_SQL = "UPDATE PRODUCTS SET REFERENCE = ?, CODE = ?, NAME = ?, PRICEBUY = ?, PRICESELL = ?, TAXCAT = ?, "
			+
			"CATEGORY = ?, UNIT = ?, ATTRIBUTESET_ID = ? WHERE ID = ?";

	private static final String DELETE_SQL = "DELETE FROM PRODUCTS WHERE ID = ?";

	// CATORDER stays NULL: it only orders the catalog buttons, and the desktop app
	// reads it as
	// NVL(CATORDER, 2147483647), so a product added here simply sorts last.
	private static final String CATALOG_INSERT_SQL = "INSERT INTO PRODUCTS_CAT (PRODUCT, CATORDER) VALUES (?, NULL)";
	private static final String CATALOG_DELETE_SQL = "DELETE FROM PRODUCTS_CAT WHERE PRODUCT = ?";
	private static final String CODES_DELETE_SQL = "DELETE FROM PRODUCTS_CODES WHERE PRODUCT = ?";
	private static final String CATALOG_EXISTS_SQL = "SELECT 1 FROM PRODUCTS_CAT WHERE PRODUCT = ?";

	private static final String LOCK_SQL = "LOCK TABLE PRODUCTS IN EXCLUSIVE MODE WAIT 5";

	// Simplified equivalent of the desktop app's
	// DataLogicSales#getNextFreeProductReferenceSent: that version finds the
	// smallest free gap per-category with a global fallback; this is a plain
	// MAX(numeric reference)+1 across all products, matching the simpler
	// next-number convention already used elsewhere in this webserver (ticket id,
	// CLOSEDCASH hostsequence).
	private static final String NEXT_NUMBER_SQL = "SELECT NVL(MAX(TO_NUMBER(REFERENCE)),0)+1 FROM PRODUCTS WHERE REGEXP_LIKE(REFERENCE, '^[0-9]+$')";

	public List<Product> findAll(String categoryId) throws SQLException {
		boolean byCategory = categoryId != null && !categoryId.isBlank();
		String sql = byCategory ? SELECT_BY_CATEGORY : SELECT_ALL;
		List<Product> result = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(sql)) {
			if (byCategory) {
				st.setString(1, categoryId);
			}
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					result.add(map(rs));
				}
			}
		}
		return result;
	}

	public Optional<Product> findById(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(SELECT_BY_ID)) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					return Optional.of(map(rs));
				}
			}
		}
		return Optional.empty();
	}

	public Product insert(Product p) throws SQLException {
		String id = UUID.randomUUID().toString();

		// Auto-fill CODE/REFERENCE with the next free number when the caller
		// doesn't supply them (mirrors the desktop product editor writing the
		// same generated value into both fields on "new product").
		boolean autoFillNumber = isBlank(p.getReference()) || isBlank(p.getCode());

		try (Connection conn = LoadDatabase.getConnection()) {
			if (!autoFillNumber) {
				try (PreparedStatement st = conn.prepareStatement(INSERT_SQL)) {
					bindInsert(st, id, p);
					st.executeUpdate();
				}
				if (p.isActive()) {
					addToCatalog(conn, id);
				}
			} else {
				// Locking the table around the read+insert stops two concurrent
				// creates racing to the same number and colliding on
				// PRODUCTS_INX_0/PRODUCTS_INX_1. This connection is exclusive to
				// the current request, so the lock now also excludes other
				// requests -- while every thread shared one Oracle session it
				// only ever excluded the desktop app.
				conn.setAutoCommit(false);
				try {
					try (PreparedStatement lock = conn.prepareStatement(LOCK_SQL)) {
						lock.execute();
					}
					String nextNumber = nextFreeNumber(conn);
					if (isBlank(p.getReference())) {
						p.setReference(nextNumber);
					}
					if (isBlank(p.getCode())) {
						p.setCode(nextNumber);
					}

					try (PreparedStatement st = conn.prepareStatement(INSERT_SQL)) {
						bindInsert(st, id, p);
						st.executeUpdate();
					}
					if (p.isActive()) {
						addToCatalog(conn, id);
					}
					conn.commit();
				} catch (SQLException e) {
					conn.rollback();
					throw e;
				}
				// autoCommit is reset by the pool when the connection is returned.
			}
		}

		p.setId(id);
		return p;
	}

	private void bindInsert(PreparedStatement st, String id, Product p) throws SQLException {
		st.setString(1, id);
		st.setString(2, p.getReference());
		st.setString(3, p.getCode());
		st.setString(4, p.getName());
		st.setDouble(5, p.getPriceBuy());
		st.setDouble(6, p.getPriceSell());
		st.setString(7, p.getTaxCatId());
		st.setString(8, p.getCategoryId());
		st.setString(9, p.getUnit());
		st.setString(10, p.getAttributeSetId());
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private String nextFreeNumber(Connection conn) throws SQLException {
		try (PreparedStatement st = conn.prepareStatement(NEXT_NUMBER_SQL);
				ResultSet rs = st.executeQuery()) {
			return rs.next() ? String.valueOf(rs.getLong(1)) : "1";
		}
	}

	public boolean update(String id, Product p) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection()) {
			try (PreparedStatement st = conn.prepareStatement(UPDATE_SQL)) {
				st.setString(1, p.getReference());
				st.setString(2, p.getCode());
				st.setString(3, p.getName());
				st.setDouble(4, p.getPriceBuy());
				st.setDouble(5, p.getPriceSell());
				st.setString(6, p.getTaxCatId());
				st.setString(7, p.getCategoryId());
				st.setString(8, p.getUnit());
				st.setString(9, p.getAttributeSetId());
				st.setString(10, id);
				if (st.executeUpdate() == 0) {
					return false;
				}
			}
			setCatalogMembership(conn, id, p.isActive());
			return true;
		}
	}

	// The flag is the presence of the PRODUCTS_CAT row, so switching it on inserts
	// that row and
	// switching it off deletes it - the same thing the desktop catalog checkbox
	// does.
	private void setCatalogMembership(Connection conn, String productId, boolean active) throws SQLException {
		if (!active) {
			try (PreparedStatement st = conn.prepareStatement(CATALOG_DELETE_SQL)) {
				st.setString(1, productId);
				st.executeUpdate();
			}
			return;
		}
		if (isInCatalog(conn, productId)) {
			return;
		}
		addToCatalog(conn, productId);
	}

	private void addToCatalog(Connection conn, String productId) throws SQLException {
		try (PreparedStatement st = conn.prepareStatement(CATALOG_INSERT_SQL)) {
			st.setString(1, productId);
			st.executeUpdate();
		}
	}

	private boolean isInCatalog(Connection conn, String productId) throws SQLException {
		try (PreparedStatement st = conn.prepareStatement(CATALOG_EXISTS_SQL)) {
			st.setString(1, productId);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	public boolean deleteById(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection()) {
			// PRODUCTS_CAT.PRODUCT is a foreign key to PRODUCTS.ID, so the catalog row of
			// an
			// active product has to go first or the delete is rejected.
			try (PreparedStatement st = conn.prepareStatement(CATALOG_DELETE_SQL)) {
				st.setString(1, id);
				st.executeUpdate();
			}
			try (PreparedStatement st = conn.prepareStatement(CODES_DELETE_SQL)) {
				st.setString(1, id);
				st.executeUpdate();
			}
			try (PreparedStatement st = conn.prepareStatement(DELETE_SQL)) {
				st.setString(1, id);
				return st.executeUpdate() > 0;
			}
		}
	}

	private Product map(ResultSet rs) throws SQLException {
		Product p = new Product();
		p.setId(rs.getString("ID"));
		p.setReference(rs.getString("REFERENCE"));
		p.setCode(rs.getString("CODE"));
		p.setName(rs.getString("NAME"));
		p.setPriceBuy(rs.getDouble("PRICEBUY"));
		p.setPriceSell(rs.getDouble("PRICESELL"));
		p.setTaxCatId(rs.getString("TAXCAT"));
		p.setCategoryId(rs.getString("CATEGORY"));
		p.setUnit(rs.getString("UNIT"));
		p.setAttributeSetId(rs.getString("ATTRIBUTESET_ID"));
		p.setActive(rs.getInt("ACTIVE") != 0);
		return p;
	}
}

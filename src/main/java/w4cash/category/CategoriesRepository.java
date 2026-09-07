package w4cash.category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import w4cash.LoadDatabase;

/**
 * Writes to the Oracle CATEGORIES table. The read path still lives in
 * {@code CategoryController}, which assembles the parent/child tree it returns.
 */
@Component
public class CategoriesRepository {

	private static final String INSERT_SQL =
			"INSERT INTO CATEGORIES (ID, NAME, PARENTID, PRINTER) VALUES (?, ?, ?, ?)";
	private static final String UPDATE_SQL =
			"UPDATE CATEGORIES SET NAME = ?, PARENTID = ?, PRINTER = ? WHERE ID = ?";
	private static final String DELETE_SQL = "DELETE FROM CATEGORIES WHERE ID = ?";
	private static final String EXISTS_SQL = "SELECT 1 FROM CATEGORIES WHERE ID = ?";
	private static final String PARENTS_SQL = "SELECT ID, PARENTID FROM CATEGORIES";
	private static final String CHILD_COUNT_SQL = "SELECT COUNT(*) FROM CATEGORIES WHERE PARENTID = ?";
	private static final String PRODUCT_COUNT_SQL = "SELECT COUNT(*) FROM PRODUCTS WHERE CATEGORY = ?";

	public Category insert(String name, String parentId, int printer) throws SQLException {
		String id = UUID.randomUUID().toString();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(INSERT_SQL)) {
			st.setString(1, id);
			st.setString(2, name);
			setParent(st, 3, parentId);
			st.setInt(4, printer);
			st.executeUpdate();
		}
		return new Category(id, name, blankToNull(parentId), printer);
	}

	public boolean update(String id, String name, String parentId, int printer) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(UPDATE_SQL)) {
			st.setString(1, name);
			setParent(st, 2, parentId);
			st.setInt(3, printer);
			st.setString(4, id);
			return st.executeUpdate() > 0;
		}
	}

	public boolean deleteById(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(DELETE_SQL)) {
			st.setString(1, id);
			return st.executeUpdate() > 0;
		}
	}

	public boolean exists(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(EXISTS_SQL)) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	public int countChildren(String id) throws SQLException {
		return count(CHILD_COUNT_SQL, id);
	}

	public int countProducts(String id) throws SQLException {
		return count(PRODUCT_COUNT_SQL, id);
	}

	/**
	 * True when making {@code parentId} the parent of {@code id} would close a
	 * loop - either directly or through the existing chain. CATEGORIES_FK_1 does
	 * not catch this, and a cycle makes the tree read return the branch as a
	 * detached root.
	 */
	public boolean wouldCycle(String id, String parentId) throws SQLException {
		if (parentId == null || parentId.isBlank()) {
			return false;
		}
		if (parentId.equals(id)) {
			return true;
		}
		Map<String, String> parents = new HashMap<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(PARENTS_SQL);
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				parents.put(rs.getString("ID"), rs.getString("PARENTID"));
			}
		}
		// Walk up from the proposed parent: reaching the category itself means the
		// edge would close a loop.
		for (String ancestor = parentId; ancestor != null; ancestor = parents.get(ancestor)) {
			if (ancestor.equals(id)) {
				return true;
			}
		}
		return false;
	}

	private int count(String sql, String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(sql)) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	// A blank parent from the client means "top level", which is a NULL PARENTID -
	// an empty string would violate CATEGORIES_FK_1.
	private void setParent(PreparedStatement st, int index, String parentId) throws SQLException {
		String value = blankToNull(parentId);
		if (value == null) {
			st.setNull(index, Types.VARCHAR);
		} else {
			st.setString(index, value);
		}
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}

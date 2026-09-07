package w4cash.taxcategory;

import java.sql.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import w4cash.LoadDatabase;

// Manages only the "default" TAXES rows for a category (PARENTID IS NULL AND
// CUSTCATEGORY IS NULL) - the simple rate-history case that PaymentController's
// resolveTaxId() actually resolves against at payment time. Customer-specific
// variants and cascaded (tax-on-tax) rows are left untouched by this repository.
@Component
public class TaxesRepository {

	private static final String SELECT_COLUMNS = "ID, NAME, RATE, VALIDFROM";
	private static final String SELECT_FOR_CATEGORY =
			"SELECT " + SELECT_COLUMNS + " FROM TAXES " +
			"WHERE CATEGORY = ? AND PARENTID IS NULL AND CUSTCATEGORY IS NULL ORDER BY VALIDFROM DESC";

	public List<TaxRateRef> findAllForCategory(String categoryId) throws SQLException {
		List<TaxRateRef> result = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(SELECT_FOR_CATEGORY)) {
			st.setString(1, categoryId);
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					result.add(map(rs));
				}
			}
		}
		return result;
	}

	public TaxRateRef insert(String categoryId, String name, double rate, String validFrom) throws SQLException {
		String id = UUID.randomUUID().toString();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"INSERT INTO TAXES (ID, NAME, CATEGORY, VALIDFROM, CUSTCATEGORY, PARENTID, RATE, RATECASCADE, RATEORDER) " +
				"VALUES (?, ?, ?, ?, NULL, NULL, ?, ?, ?)")) {
			st.setString(1, id);
			st.setString(2, name);
			st.setString(3, categoryId);
			st.setDate(4, Date.valueOf(validFrom));
			st.setDouble(5, rate);
			st.setString(6, "N");
			st.setInt(7, 0);
			st.executeUpdate();
		}
		return new TaxRateRef(id, name, rate, validFrom);
	}

	public boolean update(String categoryId, String taxId, String name, double rate, String validFrom)
			throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"UPDATE TAXES SET NAME = ?, RATE = ?, VALIDFROM = ? WHERE ID = ? AND CATEGORY = ?")) {
			st.setString(1, name);
			st.setDouble(2, rate);
			st.setDate(3, Date.valueOf(validFrom));
			st.setString(4, taxId);
			st.setString(5, categoryId);
			return st.executeUpdate() > 0;
		}
	}

	public boolean deleteById(String categoryId, String taxId) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("DELETE FROM TAXES WHERE ID = ? AND CATEGORY = ?")) {
			st.setString(1, taxId);
			st.setString(2, categoryId);
			return st.executeUpdate() > 0;
		}
	}

	private TaxRateRef map(ResultSet rs) throws SQLException {
		Date validFrom = rs.getDate("VALIDFROM");
		return new TaxRateRef(rs.getString("ID"), rs.getString("NAME"), rs.getDouble("RATE"),
				validFrom != null ? validFrom.toString() : null);
	}
}

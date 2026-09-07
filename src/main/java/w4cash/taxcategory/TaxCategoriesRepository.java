package w4cash.taxcategory;

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
public class TaxCategoriesRepository {

	public List<TaxCategoryRef> findAll() throws SQLException {
		List<TaxCategoryRef> result = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("SELECT ID, NAME FROM TAXCATEGORIES ORDER BY NAME");
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				result.add(new TaxCategoryRef(rs.getString("ID"), rs.getString("NAME")));
			}
		}
		return result;
	}

	public boolean exists(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("SELECT 1 FROM TAXCATEGORIES WHERE ID = ?")) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	public Optional<TaxCategoryRef> findById(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("SELECT ID, NAME FROM TAXCATEGORIES WHERE ID = ?")) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					return Optional.of(new TaxCategoryRef(rs.getString("ID"), rs.getString("NAME")));
				}
			}
		}
		return Optional.empty();
	}

	public TaxCategoryRef insert(String name) throws SQLException {
		String id = UUID.randomUUID().toString();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("INSERT INTO TAXCATEGORIES (ID, NAME) VALUES (?, ?)")) {
			st.setString(1, id);
			st.setString(2, name);
			st.executeUpdate();
		}
		return new TaxCategoryRef(id, name);
	}

	public boolean rename(String id, String name) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("UPDATE TAXCATEGORIES SET NAME = ? WHERE ID = ?")) {
			st.setString(1, name);
			st.setString(2, id);
			return st.executeUpdate() > 0;
		}
	}

	public boolean deleteById(String id) throws SQLException {
		// Only default-case rate rows are ever created/managed by this admin view (see
		// TaxesRepository), but clean up any of them here regardless before removing the
		// category itself to avoid an FK violation on TAXES.CATEGORY.
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement del = conn
				.prepareStatement("DELETE FROM TAXES WHERE CATEGORY = ?")) {
			del.setString(1, id);
			del.executeUpdate();
		}
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("DELETE FROM TAXCATEGORIES WHERE ID = ?")) {
			st.setString(1, id);
			return st.executeUpdate() > 0;
		}
	}
}

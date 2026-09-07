package w4cash.attribute;

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
public class AttributesRepository {

	public List<AttributeRef> findAll() throws SQLException {
		List<AttributeRef> result = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("SELECT ID, NAME FROM ATTRIBUTE ORDER BY NAME");
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				result.add(new AttributeRef(rs.getString("ID"), rs.getString("NAME")));
			}
		}
		return result;
	}

	public Optional<AttributeRef> findById(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("SELECT ID, NAME FROM ATTRIBUTE WHERE ID = ?")) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					return Optional.of(new AttributeRef(rs.getString("ID"), rs.getString("NAME")));
				}
			}
		}
		return Optional.empty();
	}

	public AttributeRef insert(String name) throws SQLException {
		String id = UUID.randomUUID().toString();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("INSERT INTO ATTRIBUTE (ID, NAME) VALUES (?, ?)")) {
			st.setString(1, id);
			st.setString(2, name);
			st.executeUpdate();
		}
		return new AttributeRef(id, name);
	}

	public boolean update(String id, String name) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("UPDATE ATTRIBUTE SET NAME = ? WHERE ID = ?")) {
			st.setString(1, name);
			st.setString(2, id);
			return st.executeUpdate() > 0;
		}
	}

	public boolean deleteById(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("DELETE FROM ATTRIBUTE WHERE ID = ?")) {
			st.setString(1, id);
			return st.executeUpdate() > 0;
		}
	}
}

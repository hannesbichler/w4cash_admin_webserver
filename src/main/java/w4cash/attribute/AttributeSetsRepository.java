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
public class AttributeSetsRepository {

	public List<AttributeSetRef> findAll() throws SQLException {
		List<AttributeSetRef> result = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("SELECT ID, NAME FROM ATTRIBUTESET ORDER BY NAME");
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				result.add(new AttributeSetRef(rs.getString("ID"), rs.getString("NAME")));
			}
		}
		return result;
	}

	public boolean setExists(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("SELECT 1 FROM ATTRIBUTESET WHERE ID = ?")) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	public Optional<AttributeSetDetail> findById(String id) throws SQLException {
		String name;
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("SELECT NAME FROM ATTRIBUTESET WHERE ID = ?")) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				if (!rs.next()) {
					return Optional.empty();
				}
				name = rs.getString("NAME");
			}
		}
		return Optional.of(new AttributeSetDetail(id, name, findAttributesForSet(id)));
	}

	private List<AttributeUseRef> findAttributesForSet(String setId) throws SQLException {
		List<AttributeUseRef> result = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"SELECT ATT.ID, ATT.NAME, AU.LINENO FROM ATTRIBUTEUSE AU " +
				"JOIN ATTRIBUTE ATT ON AU.ATTRIBUTE_ID = ATT.ID " +
				"WHERE AU.ATTRIBUTESET_ID = ? ORDER BY AU.LINENO")) {
			st.setString(1, setId);
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					result.add(new AttributeUseRef(rs.getString("ID"), rs.getString("NAME"), rs.getInt("LINENO")));
				}
			}
		}
		return result;
	}

	public AttributeSetDetail insert(String name) throws SQLException {
		String id = UUID.randomUUID().toString();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("INSERT INTO ATTRIBUTESET (ID, NAME) VALUES (?, ?)")) {
			st.setString(1, id);
			st.setString(2, name);
			st.executeUpdate();
		}
		return new AttributeSetDetail(id, name, List.of());
	}

	public boolean rename(String id, String name) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("UPDATE ATTRIBUTESET SET NAME = ? WHERE ID = ?")) {
			st.setString(1, name);
			st.setString(2, id);
			return st.executeUpdate() > 0;
		}
	}

	public boolean deleteById(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement del = conn
				.prepareStatement("DELETE FROM ATTRIBUTEUSE WHERE ATTRIBUTESET_ID = ?")) {
			del.setString(1, id);
			del.executeUpdate();
		}
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
				.prepareStatement("DELETE FROM ATTRIBUTESET WHERE ID = ?")) {
			st.setString(1, id);
			return st.executeUpdate() > 0;
		}
	}

	public boolean existsAttributeUse(String setId, String attributeId) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"SELECT 1 FROM ATTRIBUTEUSE WHERE ATTRIBUTESET_ID = ? AND ATTRIBUTE_ID = ?")) {
			st.setString(1, setId);
			st.setString(2, attributeId);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	public void addAttributeToSet(String setId, String attributeId) throws SQLException {
		int nextLineno = nextLineno(setId);
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"INSERT INTO ATTRIBUTEUSE (ID, ATTRIBUTESET_ID, ATTRIBUTE_ID, LINENO) VALUES (?, ?, ?, ?)")) {
			st.setString(1, UUID.randomUUID().toString());
			st.setString(2, setId);
			st.setString(3, attributeId);
			st.setInt(4, nextLineno);
			st.executeUpdate();
		}
	}

	private int nextLineno(String setId) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"SELECT NVL(MAX(LINENO),0) + 1 FROM ATTRIBUTEUSE WHERE ATTRIBUTESET_ID = ?")) {
			st.setString(1, setId);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 1;
			}
		}
	}

	public boolean removeAttributeFromSet(String setId, String attributeId) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"DELETE FROM ATTRIBUTEUSE WHERE ATTRIBUTESET_ID = ? AND ATTRIBUTE_ID = ?")) {
			st.setString(1, setId);
			st.setString(2, attributeId);
			return st.executeUpdate() > 0;
		}
	}

	public void reorder(String setId, List<String> attributeIdsInOrder) throws SQLException {
		// ATTUSE_LINE is a UNIQUE index on (ATTRIBUTESET_ID, LINENO), enforced per
		// statement. Writing final positions directly can collide mid-batch with
		// another row's still-unmoved LINENO (e.g. swapping two attributes), so stage
		// into negative, guaranteed-free values first, then apply the final order.
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"UPDATE ATTRIBUTEUSE SET LINENO = ? WHERE ATTRIBUTESET_ID = ? AND ATTRIBUTE_ID = ?")) {
			int lineno = 1;
			for (String attributeId : attributeIdsInOrder) {
				st.setInt(1, -lineno++);
				st.setString(2, setId);
				st.setString(3, attributeId);
				st.addBatch();
			}
			st.executeBatch();

			lineno = 1;
			for (String attributeId : attributeIdsInOrder) {
				st.setInt(1, lineno++);
				st.setString(2, setId);
				st.setString(3, attributeId);
				st.addBatch();
			}
			st.executeBatch();
		}
	}
}

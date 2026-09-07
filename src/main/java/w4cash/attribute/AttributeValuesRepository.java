package w4cash.attribute;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import w4cash.LoadDatabase;

@Component
public class AttributeValuesRepository {

	public List<AttributeValueRef> findAllForAttribute(String attributeId) throws SQLException {
		List<AttributeValueRef> result = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"SELECT ID, VALUE, LINENO FROM ATTRIBUTEVALUE WHERE ATTRIBUTE_ID = ? ORDER BY LINENO")) {
			st.setString(1, attributeId);
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					result.add(new AttributeValueRef(rs.getString("ID"), rs.getString("VALUE"), rs.getInt("LINENO")));
				}
			}
		}
		return result;
	}

	public AttributeValueRef insert(String attributeId, String value) throws SQLException {
		String id = UUID.randomUUID().toString();
		int lineno = nextLineno(attributeId);
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"INSERT INTO ATTRIBUTEVALUE (ID, ATTRIBUTE_ID, LINENO, VALUE) VALUES (?, ?, ?, ?)")) {
			st.setString(1, id);
			st.setString(2, attributeId);
			st.setInt(3, lineno);
			st.setString(4, value);
			st.executeUpdate();
		}
		return new AttributeValueRef(id, value, lineno);
	}

	private int nextLineno(String attributeId) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"SELECT NVL(MAX(LINENO),0) + 1 FROM ATTRIBUTEVALUE WHERE ATTRIBUTE_ID = ?")) {
			st.setString(1, attributeId);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 1;
			}
		}
	}

	public boolean update(String attributeId, String valueId, String value) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"UPDATE ATTRIBUTEVALUE SET VALUE = ? WHERE ID = ? AND ATTRIBUTE_ID = ?")) {
			st.setString(1, value);
			st.setString(2, valueId);
			st.setString(3, attributeId);
			return st.executeUpdate() > 0;
		}
	}

	public boolean deleteById(String attributeId, String valueId) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"DELETE FROM ATTRIBUTEVALUE WHERE ID = ? AND ATTRIBUTE_ID = ?")) {
			st.setString(1, valueId);
			st.setString(2, attributeId);
			return st.executeUpdate() > 0;
		}
	}

	public void reorder(String attributeId, List<String> valueIdsInOrder) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
				"UPDATE ATTRIBUTEVALUE SET LINENO = ? WHERE ID = ? AND ATTRIBUTE_ID = ?")) {
			int lineno = 1;
			for (String valueId : valueIdsInOrder) {
				st.setInt(1, lineno++);
				st.setString(2, valueId);
				st.setString(3, attributeId);
				st.addBatch();
			}
			st.executeBatch();
		}
	}
}

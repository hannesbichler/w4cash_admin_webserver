package w4cash.floors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.springframework.stereotype.Component;

import w4cash.LoadDatabase;

/**
 * Writes to the Oracle FLOORS table. The read path stays in
 * {@code FloorController}, which serialises the rows straight to the client.
 */
@Component
public class FloorsRepository {

	private static final String INSERT_SQL = "INSERT INTO FLOORS (ID, NAME, SORTORDER) VALUES (?, ?, ?)";
	private static final String UPDATE_SQL = "UPDATE FLOORS SET NAME = ?, SORTORDER = ? WHERE ID = ?";
	private static final String DELETE_SQL = "DELETE FROM FLOORS WHERE ID = ?";
	private static final String EXISTS_SQL = "SELECT 1 FROM FLOORS WHERE ID = ?";
	private static final String PLACE_COUNT_SQL = "SELECT COUNT(*) FROM PLACES WHERE FLOOR = ?";

	public Floor insert(String name, Integer sortOrder) throws SQLException {
		String id = UUID.randomUUID().toString();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(INSERT_SQL)) {
			st.setString(1, id);
			st.setString(2, name);
			setSortOrder(st, 3, sortOrder);
			st.executeUpdate();
		}
		return new Floor(id, name, sortOrder);
	}

	public boolean update(String id, String name, Integer sortOrder) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(UPDATE_SQL)) {
			st.setString(1, name);
			setSortOrder(st, 2, sortOrder);
			st.setString(3, id);
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

	/** Tables sitting on this floor; PLACES_FK_1 blocks deleting a floor that still has any. */
	public int countPlaces(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(PLACE_COUNT_SQL)) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	// SORTORDER is nullable; an unset position stays NULL rather than becoming 0, which would
	// jump the floor to the front of the list.
	private void setSortOrder(PreparedStatement st, int index, Integer sortOrder) throws SQLException {
		if (sortOrder == null) {
			st.setNull(index, Types.INTEGER);
		} else {
			st.setInt(index, sortOrder);
		}
	}
}

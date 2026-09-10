package w4cash.places;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import w4cash.LoadDatabase;

/**
 * Writes to the Oracle PLACES table - the tables (places) laid out on a floor.
 * The read path stays in {@code PlaceController}.
 */
@Component
public class PlacesRepository {

	private static final Logger logger = LoggerFactory.getLogger(PlacesRepository.class);

	private static final String INSERT_SQL =
			"INSERT INTO PLACES (ID, NAME, X, Y, FLOOR, WIDTH, HEIGHT, FONTSIZE, FONTCOLOR) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_SQL =
			"UPDATE PLACES SET NAME = ?, X = ?, Y = ?, FLOOR = ?, WIDTH = ?, HEIGHT = ?, FONTSIZE = ?, " +
			"FONTCOLOR = ? WHERE ID = ?";
	private static final String DELETE_SQL = "DELETE FROM PLACES WHERE ID = ?";
	private static final String EXISTS_SQL = "SELECT 1 FROM PLACES WHERE ID = ?";
	private static final String NAME_TAKEN_SQL = "SELECT 1 FROM PLACES WHERE NAME = ? AND ID <> ?";
	private static final String FLOOR_EXISTS_SQL = "SELECT 1 FROM FLOORS WHERE ID = ?";
	private static final String OPEN_TICKET_SQL = "SELECT 1 FROM SHAREDTICKETS WHERE ID = ?";

	public Place insert(Place place) throws SQLException {
		String id = UUID.randomUUID().toString();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(INSERT_SQL)) {
			st.setString(1, id);
			bindColumns(st, place, 2);
			st.executeUpdate();
		}
		place.setId_(id);
		return place;
	}

	public boolean update(String id, Place place) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(UPDATE_SQL)) {
			bindColumns(st, place, 1);
			st.setString(9, id);
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
		return any(EXISTS_SQL, id);
	}

	public boolean floorExists(String floorId) throws SQLException {
		return any(FLOOR_EXISTS_SQL, floorId);
	}

	/**
	 * PLACES_NAME_INX is a unique index on NAME, so a duplicate would come back as
	 * ORA-00001; checking first turns that into a message the admin can act on.
	 * {@code id} is the row being edited and is excluded from the check.
	 */
	public boolean nameTaken(String name, String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(NAME_TAKEN_SQL)) {
			st.setString(1, name);
			st.setString(2, id == null ? "" : id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	/**
	 * True when an unsent ticket is parked on this table. SHAREDTICKETS.ID is the
	 * place id; there is no foreign key, so deleting the table would strand the
	 * ticket where neither the POS nor this admin can reach it.
	 */
	public boolean hasOpenTicket(String id) throws SQLException {
		return any(OPEN_TICKET_SQL, id);
	}

	private boolean any(String sql, String value) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(sql)) {
			st.setString(1, value);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	// NAME, X, Y, FLOOR, WIDTH, HEIGHT, FONTSIZE, FONTCOLOR starting at `first`.
	private void bindColumns(PreparedStatement st, Place place, int first) throws SQLException {
		logger.info(
				"bindColumns firstIndex={} name={} x={} y={} floorId={} width={} height={} fontSize={} fontColor={}",
				first, place.getName(), place.getX(), place.getY(), place.getFloorId(),
				place.getWidth(), place.getHeight(), place.getFontSize(), place.getFontColor());
		st.setString(first, place.getName());
		st.setInt(first + 1, place.getX());
		st.setInt(first + 2, place.getY());
		st.setString(first + 3, place.getFloorId());
		setNullableInt(st, first + 4, place.getWidth());
		setNullableInt(st, first + 5, place.getHeight());
		setNullableInt(st, first + 6, place.getFontSize());
		if (place.getFontColor() == null || place.getFontColor().isBlank()) {
			st.setNull(first + 7, Types.VARCHAR);
		} else {
			st.setString(first + 7, place.getFontColor());
		}
	}

	// WIDTH, HEIGHT and FONTSIZE are nullable: unset means "use the POS default size",
	// which is not the same as 0.
	private void setNullableInt(PreparedStatement st, int index, Integer value) throws SQLException {
		if (value == null) {
			st.setNull(index, Types.INTEGER);
		} else {
			st.setInt(index, value);
		}
	}
}

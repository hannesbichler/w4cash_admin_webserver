package w4cash.role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.stereotype.Component;

import w4cash.LoadDatabase;

/**
 * Writes to the Oracle ROLES table. The read path stays in
 * {@code RoleController}, which serialises the rows straight to the client.
 *
 * <p>
 * Nothing here touches ROLES.PERMISSIONS. A new role is inserted with it NULL,
 * which {@code AppUser#fillPermissions} treats as "no permissions beyond the
 * baseline every user has" - so a role created here grants nothing until the
 * POS is used to say what it may do. A rename leaves the existing BLOB alone.
 */
@Component
public class RolesRepository {

	private static final String INSERT_SQL = "INSERT INTO ROLES (ID, NAME) VALUES (?, ?)";
	private static final String UPDATE_SQL = "UPDATE ROLES SET NAME = ? WHERE ID = ?";
	private static final String DELETE_SQL = "DELETE FROM ROLES WHERE ID = ?";
	private static final String EXISTS_SQL = "SELECT 1 FROM ROLES WHERE ID = ?";
	private static final String NAME_TAKEN_SQL = "SELECT 1 FROM ROLES WHERE UPPER(NAME) = UPPER(?) AND ID <> ?";
	private static final String PERSON_COUNT_SQL = "SELECT COUNT(*) FROM PEOPLE WHERE ROLE = ?";

	public Role insert(String name) throws SQLException {
		String id = UUID.randomUUID().toString();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(INSERT_SQL)) {
			st.setString(1, id);
			st.setString(2, name);
			st.executeUpdate();
		}
		return new Role(id, name);
	}

	public boolean update(String id, String name) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(UPDATE_SQL)) {
			st.setString(1, name);
			st.setString(2, id);
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

	/**
	 * ROLES.NAME carries a unique index, so a clash would otherwise surface as a
	 * 500. Checked case-insensitively: two roles differing only in case would be
	 * indistinguishable in the POS role picker.
	 *
	 * @param id the role being edited, excluded from the check; pass "" when
	 *           creating.
	 */
	public boolean nameTaken(String name, String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(NAME_TAKEN_SQL)) {
			st.setString(1, name);
			st.setString(2, id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	/** People holding this role; PEOPLE.ROLE blocks deleting a role still in use. */
	public int countPeople(String id) throws SQLException {
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(PERSON_COUNT_SQL)) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}
}

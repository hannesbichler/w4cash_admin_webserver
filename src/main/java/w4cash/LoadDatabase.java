package w4cash;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Access point for connections to the w4cash Oracle schema.
 *
 * <p>
 * This used to hold a single static {@link Connection} shared by every request
 * thread, which made JDBC transaction state global: one request calling
 * {@code setAutoCommit(false)} enrolled every other thread's writes into its
 * transaction, and {@code LOCK TABLE ... IN EXCLUSIVE MODE} gave no mutual
 * exclusion at all between requests, because Oracle holds locks per session and
 * all threads shared one session.
 *
 * <p>
 * Connections now come from a pool ({@code w4cashDataSource}), so each caller
 * gets its own session. <b>Every caller must close the connection</b> — use
 * try-with-resources:
 *
 * <pre>
 * try (Connection c = LoadDatabase.getConnection();
 * 		PreparedStatement st = c.prepareStatement(SQL)) {
 * 	...
 * }
 * </pre>
 *
 * <p>
 * A unit of work that must be atomic (or that takes {@code LOCK TABLE}) has to
 * hold <em>one</em> connection for its whole duration and pass it down, rather
 * than opening a fresh one per statement.
 *
 * <p>
 * The static accessor is a deliberate intermediate step: it keeps this migration
 * mechanical across ~130 call sites. Injecting the {@code DataSource} (or a
 * {@code JdbcTemplate}) into each repository is the better end state.
 */
@Component("loadDatabase")
public class LoadDatabase {

	private static volatile DataSource dataSource;

	LoadDatabase(@Qualifier("w4cashDataSource") DataSource w4cashDataSource) {
		dataSource = w4cashDataSource;
	}

	/**
	 * Test seam: installs the {@link DataSource} that {@link #getConnection()}
	 * borrows from. Production wiring goes through the constructor; tests use this
	 * to inject a mock and pass {@code null} to clear it again.
	 */
	public static void setDataSource(DataSource ds) {
		dataSource = ds;
	}

	/**
	 * Borrows a connection from the w4cash Oracle pool. The caller owns it and must
	 * close it.
	 */
	public static Connection getConnection() throws SQLException {
		DataSource ds = dataSource;
		if (ds == null) {
			throw new SQLException("w4cash datasource is not initialised yet");
		}
		return ds.getConnection();
	}
}

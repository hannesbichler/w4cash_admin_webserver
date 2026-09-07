package w4cash;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.openbravo.pos.util.AltEncrypter;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Declares the application's two datasources explicitly.
 *
 * <p>
 * Both must be declared here: Boot's {@code DataSourceAutoConfiguration} is
 * {@code @ConditionalOnMissingBean(DataSource.class)}, so as soon as the Oracle
 * pool below exists the auto-configured H2 datasource that JPA/Hibernate used to
 * bind to disappears. {@link #jpaDataSource()} restores it, and stays
 * {@code @Primary} so Hibernate keeps binding to H2 exactly as before.
 */
@Configuration
public class DataSourceConfig {

	private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

	/**
	 * In-memory H2 backing the JPA entities (the mirror tables in
	 * person/place/category/floor/ticketinfo, plus chat). Previously supplied by
	 * Boot's auto-configuration; declared explicitly now, with the same in-memory
	 * semantics.
	 */
	@Bean
	@Primary
	public DataSource jpaDataSource() {
		return DataSourceBuilder.create()
				.driverClassName("org.h2.Driver")
				.url("jdbc:h2:mem:w4cash;DB_CLOSE_DELAY=-1")
				.username("sa")
				.password("")
				.build();
	}

	/**
	 * Pooled connections to the real w4cash Oracle schema.
	 *
	 * <p>
	 * Credentials come from the legacy {@code AppConfig} (loaded in
	 * {@link W4cashApplication#main}), not from {@code spring.datasource.*}.
	 *
	 * <p>
	 * {@code initializationFailTimeout=-1} keeps the previous behaviour of starting
	 * the application even when the database is unreachable: endpoints then fail
	 * per-request with a {@link java.sql.SQLException} instead of preventing
	 * startup. Unlike the old single static connection, the pool re-validates and
	 * replaces broken connections, so the server now recovers from a dropped
	 * session without a restart.
	 */
	@Bean
	public DataSource w4cashDataSource(
			@Value("${w4cash.datasource.pool-size:10}") int poolSize,
			@Value("${w4cash.datasource.connection-timeout-ms:10000}") long connectionTimeoutMs) {

		var config = W4cashApplication.APP_CONFIG;
		String url = config.getProperty("db.URL");
		String user = config.getProperty("db.user");
		String password = decryptPassword(user, config.getProperty("db.password"));
		String driver = config.getProperty("db.driver");

		HikariDataSource ds = new HikariDataSource();
		ds.setPoolName("w4cash-oracle");
		ds.setJdbcUrl(url);
		ds.setUsername(user);
		ds.setPassword(password);
		if (driver != null && !driver.isBlank()) {
			ds.setDriverClassName(driver);
		}
		ds.setMaximumPoolSize(poolSize);
		ds.setConnectionTimeout(connectionTimeoutMs);
		ds.setInitializationFailTimeout(-1);

		logger.info("Configured w4cash Oracle pool: url={}, user={}, maxPoolSize={}", url, user, poolSize);
		return ds;
	}

	/** Mirrors the {@code crypt:} handling the old LoadDatabase did inline. */
	private String decryptPassword(String user, String password) {
		if (user != null && password != null && password.startsWith("crypt:")) {
			AltEncrypter cypher = new AltEncrypter("cypherkey" + user);
			return cypher.decrypt(password.substring(6));
		}
		return password;
	}
}

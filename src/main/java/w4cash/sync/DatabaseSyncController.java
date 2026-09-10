package w4cash.sync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "", "/api" })
class DatabaseSyncController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSyncController.class);

    @PostMapping("/database-sync/test-connection")
    ResponseEntity<String> testConnection(@RequestBody RemoteDbProperties body) {
        if (body == null || isBlank(body.getHost()) || isBlank(body.getDatabase()) || isBlank(body.getUser())
                || isBlank(body.getPassword())) {
            return ResponseEntity.badRequest().body("host, port, database, user and password are required");
        }
        if (body.getPort() <= 0) {
            return ResponseEntity.badRequest().body("port must be greater than 0");
        }

        String jdbcUrl = toOracleJdbcUrl(body.getHost().trim(), body.getPort(), body.getDatabase().trim());
        Properties props = new Properties();
        props.setProperty("user", body.getUser().trim());
        props.setProperty("password", body.getPassword());
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
        props.setProperty("oracle.jdbc.ReadTimeout", "5000");

        try (Connection conn = DriverManager.getConnection(jdbcUrl, props)) {
            if (!conn.isValid(3)) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Connection opened but is not valid");
            }
            return ResponseEntity.ok("Connection successful");
        } catch (SQLException e) {
            logger.warn("Remote database connection test failed for {}:{}", body.getHost(), body.getPort(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Connection failed: " + e.getMessage());
        }
    }

    private static String toOracleJdbcUrl(String host, int port, String database) {
        String normalizedDatabase = database.startsWith("/") ? database.substring(1) : database;
        if (normalizedDatabase.startsWith("(")) {
            return "jdbc:oracle:thin:@" + normalizedDatabase;
        }
        return "jdbc:oracle:thin:@" + host + ":" + port + "/" + normalizedDatabase;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static class RemoteDbProperties {
        private String host;
        private int port;
        private String database;
        private String user;
        private String password;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}

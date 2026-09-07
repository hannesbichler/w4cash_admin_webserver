package w4cash.print;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import w4cash.LoadDatabase;

@Component
public class PrintJobRepository {

    private static final Logger logger = LoggerFactory.getLogger(PrintJobRepository.class);

    private static final String INSERT_SQL =
            "INSERT INTO PRINT_JOBS (TABLE_ID, TABLE_NAME, PRINTER_INDEX, PRINTER_NAME, " +
            "LINE_COUNT, PRINTED_AT, SUCCESS, ERROR, CONTENT, PERSON_ID, PERSON_NAME) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM PRINT_JOBS WHERE ID = ?";

    private static final String DELETE_BY_ID =
            "DELETE FROM PRINT_JOBS WHERE ID = ?";

    private static final String UPDATE_SUCCESS =
            "UPDATE PRINT_JOBS SET SUCCESS = ?, ERROR = ? WHERE ID = ?";

    public void save(PrintJob job) {
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn.prepareStatement(INSERT_SQL)) {
            st.setString(1, job.getTableId());
            st.setString(2, job.getTableName());
            st.setInt(3, job.getPrinterIndex());
            st.setString(4, job.getPrinterName());
            st.setInt(5, job.getLineCount());
            st.setTimestamp(6, job.getPrintedAt() != null ? Timestamp.valueOf(job.getPrintedAt()) : null);
            st.setInt(7, job.isSuccess() ? 1 : 0);
            st.setString(8, job.getError());
            st.setString(9, job.getContent());
            st.setString(10, job.getPersonId());
            st.setString(11, job.getPersonName());
            st.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save PrintJob for tableId={}", job.getTableId(), e);
        }
    }

    /** Returns up to {@code size} jobs starting at row {@code page * size} (0-based page). */
    public List<PrintJob> find(String tableId, String personName, Boolean success, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();

        if (tableId != null && !tableId.isBlank()) {
            where.append(" AND (LOWER(TABLE_NAME) LIKE ? OR LOWER(TABLE_ID) LIKE ?)");
            String like = "%" + tableId.toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }
        if (personName != null && !personName.isBlank()) {
            where.append(" AND (LOWER(PERSON_NAME) LIKE ? OR LOWER(PERSON_ID) LIKE ?)");
            String like = "%" + personName.toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }
        if (success != null) {
            where.append(" AND SUCCESS = ?");
            params.add(success ? 1 : 0);
        }

        int minRow = page * size;
        int maxRow = minRow + size;
        String sql =
                "SELECT * FROM (" +
                "  SELECT a.*, ROWNUM rn FROM (" +
                "    SELECT * FROM PRINT_JOBS WHERE 1=1" + where + " ORDER BY PRINTED_AT DESC" +
                "  ) a WHERE ROWNUM <= ?" +
                ") WHERE rn > ?";
        params.add(maxRow);
        params.add(minRow);

        return queryDynamic(sql, params);
    }

    public Optional<PrintJob> findById(long id) {
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn.prepareStatement(SELECT_BY_ID)) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find PrintJob id={}", id, e);
        }
        return Optional.empty();
    }

    public boolean existsById(long id) {
        return findById(id).isPresent();
    }

    public void updateSuccess(long id, boolean success, String error) {
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn.prepareStatement(UPDATE_SUCCESS)) {
            st.setInt(1, success ? 1 : 0);
            st.setString(2, error);
            st.setLong(3, id);
            st.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update success for PrintJob id={}", id, e);
        }
    }

    public void deleteById(long id) {
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn.prepareStatement(DELETE_BY_ID)) {
            st.setLong(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete PrintJob id={}", id, e);
        }
    }

    private List<PrintJob> queryDynamic(String sql, List<Object> params) {
        List<PrintJob> result = new ArrayList<>();
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String s) st.setString(i + 1, s);
                else if (p instanceof Integer n) st.setInt(i + 1, n);
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to query PrintJobs", e);
        }
        return result;
    }

    private PrintJob map(ResultSet rs) throws SQLException {
        PrintJob job = new PrintJob();
        job.setId(rs.getLong("ID"));
        job.setTableId(rs.getString("TABLE_ID"));
        job.setTableName(rs.getString("TABLE_NAME"));
        job.setPrinterIndex(rs.getInt("PRINTER_INDEX"));
        job.setPrinterName(rs.getString("PRINTER_NAME"));
        job.setLineCount(rs.getInt("LINE_COUNT"));
        Timestamp ts = rs.getTimestamp("PRINTED_AT");
        if (ts != null) job.setPrintedAt(ts.toLocalDateTime());
        job.setSuccess(rs.getInt("SUCCESS") == 1);
        job.setError(rs.getString("ERROR"));
        Clob clob = rs.getClob("CONTENT");
        if (clob != null) job.setContent(clob.getSubString(1, (int) clob.length()));
        job.setPersonId(rs.getString("PERSON_ID"));
        job.setPersonName(rs.getString("PERSON_NAME"));
        return job;
    }
}

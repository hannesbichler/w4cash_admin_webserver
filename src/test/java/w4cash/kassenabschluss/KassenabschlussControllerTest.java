package w4cash.kassenabschluss;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import w4cash.LoadDatabase;
import javax.sql.DataSource;

@WebMvcTest(KassenabschlussController.class)
class KassenabschlussControllerTest {

    @Autowired
    MockMvc mockMvc;

    private Connection mockConnection;

    private PreparedStatement mockSessionStmt;
    private ResultSet mockSessionRs;

    private PreparedStatement mockTicketCountStmt;
    private ResultSet mockTicketCountRs;

    private PreparedStatement mockCashTotalStmt;
    private ResultSet mockCashTotalRs;

    private PreparedStatement mockPaperTotalStmt;
    private ResultSet mockPaperTotalRs;

    private PreparedStatement mockCashInOutStmt;
    private ResultSet mockCashInOutRs;

    private PreparedStatement mockCardTotalStmt;
    private ResultSet mockCardTotalRs;

    private PreparedStatement mockFreeTotalStmt;
    private ResultSet mockFreeTotalRs;

    private PreparedStatement mockPaymentLinesStmt;
    private ResultSet mockPaymentLinesRs;

    private PreparedStatement mockSalesStmt;
    private ResultSet mockSalesRs;

    private PreparedStatement mockTaxTotalStmt;
    private ResultSet mockTaxTotalRs;

    private PreparedStatement mockTaxBreakdownStmt;
    private ResultSet mockTaxBreakdownRs;

    private PreparedStatement mockLockStmt;
    private PreparedStatement mockUpdateStmt;

    @BeforeEach
    void setUp() throws Exception {
        mockConnection = mock(Connection.class);

        mockSessionStmt = mock(PreparedStatement.class);
        mockSessionRs = mock(ResultSet.class);
        mockTicketCountStmt = mock(PreparedStatement.class);
        mockTicketCountRs = mock(ResultSet.class);
        mockCashTotalStmt = mock(PreparedStatement.class);
        mockCashTotalRs = mock(ResultSet.class);
        mockPaperTotalStmt = mock(PreparedStatement.class);
        mockPaperTotalRs = mock(ResultSet.class);
        mockCashInOutStmt = mock(PreparedStatement.class);
        mockCashInOutRs = mock(ResultSet.class);
        mockCardTotalStmt = mock(PreparedStatement.class);
        mockCardTotalRs = mock(ResultSet.class);
        mockFreeTotalStmt = mock(PreparedStatement.class);
        mockFreeTotalRs = mock(ResultSet.class);
        mockPaymentLinesStmt = mock(PreparedStatement.class);
        mockPaymentLinesRs = mock(ResultSet.class);
        mockSalesStmt = mock(PreparedStatement.class);
        mockSalesRs = mock(ResultSet.class);
        mockTaxTotalStmt = mock(PreparedStatement.class);
        mockTaxTotalRs = mock(ResultSet.class);
        mockTaxBreakdownStmt = mock(PreparedStatement.class);
        mockTaxBreakdownRs = mock(ResultSet.class);
        mockLockStmt = mock(PreparedStatement.class);
        mockUpdateStmt = mock(PreparedStatement.class);

        DataSource mockDataSource = mock(DataSource.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        LoadDatabase.setDataSource(mockDataSource);

        // Stub ordering: most general first, most specific last (last match wins in Mockito).
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("FROM CLOSEDCASH"))))
                .thenReturn(mockSessionStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("COUNT(*)"))))
                .thenReturn(mockTicketCountStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("'cash','cashrefund'"))))
                .thenReturn(mockCashTotalStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("'paperin','paperout'"))))
                .thenReturn(mockPaperTotalStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("'cashin','cashout'"))))
                .thenReturn(mockCashInOutStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("'magcard','magcardrefund'"))))
                .thenReturn(mockCardTotalStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("in ('free')"))))
                .thenReturn(mockFreeTotalStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("GROUP BY PAYMENTS.DESCRIPTION"))))
                .thenReturn(mockPaymentLinesStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("LEFT JOIN TAXLINES"))))
                .thenReturn(mockSalesStmt);
        when(mockConnection.prepareStatement(
                argThat(s -> s != null && s.contains("SUM(ROUND(TAXLINES.AMOUNT") && !s.contains("TAXCATEGORIES"))))
                .thenReturn(mockTaxTotalStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.contains("TAXCATEGORIES.NAME"))))
                .thenReturn(mockTaxBreakdownStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.startsWith("LOCK TABLE"))))
                .thenReturn(mockLockStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.startsWith("UPDATE"))))
                .thenReturn(mockUpdateStmt);

        when(mockSessionStmt.executeQuery()).thenReturn(mockSessionRs);
        when(mockTicketCountStmt.executeQuery()).thenReturn(mockTicketCountRs);
        when(mockCashTotalStmt.executeQuery()).thenReturn(mockCashTotalRs);
        when(mockPaperTotalStmt.executeQuery()).thenReturn(mockPaperTotalRs);
        when(mockCashInOutStmt.executeQuery()).thenReturn(mockCashInOutRs);
        when(mockCardTotalStmt.executeQuery()).thenReturn(mockCardTotalRs);
        when(mockFreeTotalStmt.executeQuery()).thenReturn(mockFreeTotalRs);
        when(mockPaymentLinesStmt.executeQuery()).thenReturn(mockPaymentLinesRs);
        when(mockSalesStmt.executeQuery()).thenReturn(mockSalesRs);
        when(mockTaxTotalStmt.executeQuery()).thenReturn(mockTaxTotalRs);
        when(mockTaxBreakdownStmt.executeQuery()).thenReturn(mockTaxBreakdownRs);
    }

    @AfterEach
    void tearDown() {
        LoadDatabase.setDataSource(null);
    }

    private void stubOpenSession() throws SQLException {
        when(mockSessionRs.next()).thenReturn(true);
        when(mockSessionRs.getString("MONEY")).thenReturn("money1");
        when(mockSessionRs.getString("HOST")).thenReturn("tablet1");
        when(mockSessionRs.getString("HOSTSEQUENCE")).thenReturn("5");
        when(mockSessionRs.getString("DATESTART")).thenReturn("2026-07-01 08:00:00.0");
    }

    private void stubZeroTotals() throws SQLException {
        when(mockTicketCountRs.next()).thenReturn(true);
        when(mockTicketCountRs.getLong(1)).thenReturn(3L);

        when(mockCashTotalRs.next()).thenReturn(true);
        when(mockCashTotalRs.getDouble(1)).thenReturn(50.0);

        when(mockPaperTotalRs.next()).thenReturn(true);
        when(mockPaperTotalRs.getDouble(1)).thenReturn(0.0);

        when(mockCashInOutRs.next()).thenReturn(true);
        when(mockCashInOutRs.getDouble(1)).thenReturn(0.0);

        when(mockCardTotalRs.next()).thenReturn(true);
        when(mockCardTotalRs.getDouble(1)).thenReturn(20.0);

        when(mockFreeTotalRs.next()).thenReturn(true);
        when(mockFreeTotalRs.getDouble(1)).thenReturn(0.0);

        when(mockPaymentLinesRs.next()).thenReturn(true, false);
        when(mockPaymentLinesRs.getString(1)).thenReturn("cash");
        when(mockPaymentLinesRs.getString(3)).thenReturn("Cash");
        when(mockPaymentLinesRs.getDouble(2)).thenReturn(50.0);

        when(mockSalesRs.next()).thenReturn(true);
        when(mockSalesRs.getLong(1)).thenReturn(2L);
        when(mockSalesRs.getDouble(2)).thenReturn(60.0);

        when(mockTaxTotalRs.next()).thenReturn(true);
        when(mockTaxTotalRs.getDouble(1)).thenReturn(10.0);

        when(mockTaxBreakdownRs.next()).thenReturn(true, false);
        when(mockTaxBreakdownRs.getString(1)).thenReturn("Standard");
        when(mockTaxBreakdownRs.getDouble(2)).thenReturn(10.0);
        when(mockTaxBreakdownRs.getDouble(3)).thenReturn(60.0);
    }

    @Test
    void xReport_returnsTotals_whenOpenSessionExists() throws Exception {
        stubOpenSession();
        stubZeroTotals();

        mockMvc.perform(get("/kassenabschluss/tablet1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.money").value("money1"))
                .andExpect(jsonPath("$.cashTotal").value(50.0))
                .andExpect(jsonPath("$.salesCount").value(2))
                .andExpect(jsonPath("$.paymentLines[0].payment").value("cash"));
    }

    @Test
    void xReport_returns404_whenNoOpenSession() throws Exception {
        when(mockSessionRs.next()).thenReturn(false);

        mockMvc.perform(get("/kassenabschluss/tablet1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void xReport_returns500_whenDbConnectionNull() throws Exception {
        LoadDatabase.setDataSource(null);

        mockMvc.perform(get("/kassenabschluss/tablet1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void zReport_closesSessionAndReturnsTotals() throws Exception {
        stubOpenSession();
        stubZeroTotals();
        when(mockUpdateStmt.executeUpdate()).thenReturn(1);

        mockMvc.perform(post("/kassenabschluss/tablet1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.money").value("money1"))
                .andExpect(jsonPath("$.dateEnd").exists());

        verify(mockUpdateStmt).setString(2, "tablet1");
        verify(mockUpdateStmt).setString(3, "money1");
        verify(mockUpdateStmt).setTimestamp(eq(1), any(Timestamp.class));
        verify(mockConnection).commit();
    }

    @Test
    void zReport_returns404_whenNoOpenSession() throws Exception {
        when(mockSessionRs.next()).thenReturn(false);

        mockMvc.perform(post("/kassenabschluss/tablet1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void zReport_returns409_whenAlreadyClosedRace() throws Exception {
        stubOpenSession();
        stubZeroTotals();
        when(mockUpdateStmt.executeUpdate()).thenReturn(0);

        mockMvc.perform(post("/kassenabschluss/tablet1"))
                .andExpect(status().isConflict());

        verify(mockConnection).rollback();
        verify(mockConnection, never()).commit();
    }

    @Test
    void zReport_returns500AndRollsBack_onSqlException() throws Exception {
        stubOpenSession();
        when(mockTicketCountStmt.executeQuery()).thenThrow(new SQLException("boom"));

        mockMvc.perform(post("/kassenabschluss/tablet1"))
                .andExpect(status().isInternalServerError());

        verify(mockConnection).rollback();
    }

    @Test
    void zReport_alwaysClosesConnection() throws Exception {
        stubOpenSession();
        stubZeroTotals();
        when(mockUpdateStmt.executeUpdate()).thenReturn(1);

        mockMvc.perform(post("/kassenabschluss/tablet1"))
                .andExpect(status().isOk());

        // autoCommit is reset by the pool; what matters here is that the
        // connection is handed back rather than leaked.
        verify(mockConnection).close();
    }
}

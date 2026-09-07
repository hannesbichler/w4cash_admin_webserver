package w4cash.kassenabschluss;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openbravo.format.Formats;

import w4cash.LoadDatabase;

@RestController
class KassenabschlussController {
    private static final Logger logger = LoggerFactory.getLogger(KassenabschlussController.class);

    private record OpenSession(String money, String host, String hostSequence, String dateStart) {
    }

    // Non-destructive preview of the still-open session's current totals
    // (the desktop app's "X-Kassenschluss"/Printer.PartialCash report).
    @GetMapping("/kassenabschluss/{tabletId}")
    ResponseEntity<?> xReport(@PathVariable String tabletId) {
        logger.info("GET /kassenabschluss request was called for tabletId={}", tabletId);

        // One connection for the whole report so every total is read from a
        // consistent point in time rather than across N independent sessions.
        try (Connection conn = LoadDatabase.getConnection()) {
            OpenSession session = findOpenSession(conn, tabletId);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No open cash session for tabletId=" + tabletId);
            }
            return ResponseEntity.ok(computeTotals(conn, tabletId, session, null));
        } catch (SQLException e) {
            logger.error("Failed to build X-report for tabletId={}", tabletId, e);
            return ResponseEntity.internalServerError().body("Failed to build X-report");
        }
    }

    // Mirrors com.openbravo.pos.panels.JPanelCloseMoney#m_jCloseCashActionPerformed,
    // minus printing the Z-report receipt and RKSV signing/DEP-7 export (those stay
    // out of scope here, same as PaymentController already excludes RKSV signing).
    @PostMapping("/kassenabschluss/{tabletId}")
    ResponseEntity<?> zReport(@PathVariable String tabletId) {
        logger.info("POST /kassenabschluss request was called for tabletId={}", tabletId);

        Timestamp dateEndTs = new Timestamp(System.currentTimeMillis());

        try (Connection conn = LoadDatabase.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock exclusive at start, same convention as PaymentController /
                // DataLogicSales#saveTicket, guarding the DATEEND race between
                // terminals. Now that the connection is exclusive to this request
                // the lock also excludes concurrent requests to this server.
                try (PreparedStatement lock = conn
                        .prepareStatement("LOCK TABLE CLOSEDCASH IN EXCLUSIVE MODE WAIT 5")) {
                    lock.execute();
                }

                // Read the session inside the lock: doing it beforehand let a
                // concurrent close slip in between the lookup and the update.
                OpenSession session = findOpenSession(conn, tabletId);
                if (session == null) {
                    conn.rollback();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("No open cash session for tabletId=" + tabletId);
                }

                Kassenabschluss result = computeTotals(conn, tabletId, session, dateEndTs.toString());

                // AND DATEEND IS NULL guards against a concurrent close silently
                // overwriting an already-closed session's DATEEND (the desktop app's
                // JPanelCloseMoney does not have this guard).
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE CLOSEDCASH SET DATEEND = ? WHERE HOST = ? AND MONEY = ? AND DATEEND IS NULL")) {
                    upd.setTimestamp(1, dateEndTs);
                    upd.setString(2, tabletId);
                    upd.setString(3, session.money());
                    int rows = upd.executeUpdate();
                    if (rows == 0) {
                        conn.rollback();
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("Cash session was already closed by another request");
                    }
                }

                conn.commit();
                return ResponseEntity.ok(result);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            // autoCommit is reset by the pool when the connection is returned.
        } catch (SQLException e) {
            logger.error("Failed to close cash session for tabletId={}", tabletId, e);
            return ResponseEntity.internalServerError().body("Failed to close cash session: " + e.getMessage());
        }
    }

    // Mirrors ActiveCashController's session lookup exactly (same SQL), reused here
    // so both endpoints resolve MONEY the same way the tablet's active-cash GET does.
    private OpenSession findOpenSession(Connection conn, String tabletId) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(
                "SELECT MONEY, HOST, HOSTSEQUENCE, DATESTART FROM CLOSEDCASH WHERE HOST = ? AND DATEEND IS NULL")) {
            st.setString(1, tabletId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new OpenSession(rs.getString("MONEY"), rs.getString("HOST"),
                            rs.getString("HOSTSEQUENCE"), rs.getString("DATESTART"));
                }
            }
        }
        return null;
    }

    // Mirrors com.openbravo.pos.panels.PaymentsModel#loadInstance, minus the
    // 'free'-payment duplicate section and the optional "others" products-filter
    // report (see plan doc for scope). Reused by both the X-report GET (dateEnd=null)
    // and the Z-report POST (dateEnd=the timestamp about to be written).
    private Kassenabschluss computeTotals(Connection conn, String tabletId, OpenSession session, String dateEnd)
            throws SQLException {
        int currencyDecimals = Formats.getCurrencyDecimals();
        String money = session.money();

        long ticketCount = queryLong(conn,
                "SELECT COUNT(*) FROM PAYMENTS, RECEIPTS "
                        + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = ? AND PAYMENTS.PAYMENT != 'free'",
                money);

        double cashTotal = queryDouble(conn,
                "SELECT NVL(SUM(PAYMENTS.TOTAL),0) FROM PAYMENTS, RECEIPTS "
                        + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND PAYMENTS.PAYMENT in ('cash','cashrefund') AND RECEIPTS.MONEY = ?",
                money);

        double paperTotal = queryDouble(conn,
                "SELECT NVL(SUM(PAYMENTS.TOTAL),0) FROM PAYMENTS, RECEIPTS "
                        + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND PAYMENTS.PAYMENT in ('paperin','paperout') AND RECEIPTS.MONEY = ?",
                money);

        double cashInOutTotal = queryDouble(conn,
                "SELECT NVL(SUM(PAYMENTS.TOTAL),0) FROM PAYMENTS, RECEIPTS "
                        + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND PAYMENTS.PAYMENT in ('cashin','cashout') AND RECEIPTS.MONEY = ?",
                money);

        double cardTotal = queryDouble(conn,
                "SELECT NVL(SUM(PAYMENTS.TOTAL),0) FROM PAYMENTS, RECEIPTS "
                        + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND PAYMENTS.PAYMENT in ('magcard','magcardrefund') AND RECEIPTS.MONEY = ?",
                money);

        double freeTotal = queryDouble(conn,
                "SELECT NVL(SUM(PAYMENTS.TOTAL),0) FROM PAYMENTS, RECEIPTS "
                        + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND PAYMENTS.PAYMENT in ('free') AND RECEIPTS.MONEY = ?",
                money);

        List<Kassenabschluss.PaymentLine> paymentLines = new ArrayList<>();
        try (PreparedStatement st = conn.prepareStatement(
                "SELECT PAYMENTS.PAYMENT, SUM(PAYMENTS.TOTAL), PAYMENTS.DESCRIPTION "
                        + "FROM PAYMENTS, RECEIPTS "
                        + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = ? AND PAYMENTS.PAYMENT != 'free' "
                        + "GROUP BY PAYMENTS.DESCRIPTION, PAYMENTS.PAYMENT "
                        + "ORDER BY case PAYMENTS.PAYMENT when 'cash' then 1 when 'cashrefund' then 2 else 999 end, PAYMENTS.PAYMENT")) {
            st.setString(1, money);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    paymentLines.add(new Kassenabschluss.PaymentLine(
                            rs.getString(1), rs.getString(3), rs.getDouble(2)));
                }
            }
        }

        long salesCount = 0;
        double salesBase = 0.0;
        try (PreparedStatement st = conn.prepareStatement(
                "SELECT COUNT(DISTINCT RECEIPTS.ID), SUM(ROUND(NVL(TAXLINES.BASE,0),?)) "
                        + "FROM RECEIPTS LEFT JOIN TAXLINES ON RECEIPTS.ID = TAXLINES.RECEIPT "
                        + "INNER JOIN PAYMENTS ON PAYMENTS.RECEIPT = RECEIPTS.ID "
                        + "WHERE PAYMENTS.PAYMENT <> 'free' AND RECEIPTS.MONEY = ?")) {
            st.setInt(1, currencyDecimals);
            st.setString(2, money);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    salesCount = rs.getLong(1);
                    salesBase = rs.getDouble(2);
                }
            }
        }

        double salesTax = queryDoubleWithDecimals(conn,
                "SELECT SUM(ROUND(TAXLINES.AMOUNT,?)) FROM RECEIPTS, TAXLINES, PAYMENTS "
                        + "WHERE PAYMENTS.RECEIPT=RECEIPTS.ID AND PAYMENTS.PAYMENT<>'free' AND RECEIPTS.ID=TAXLINES.RECEIPT AND RECEIPTS.MONEY = ?",
                currencyDecimals, money);

        List<Kassenabschluss.TaxBreakdown> taxBreakdown = new ArrayList<>();
        try (PreparedStatement st = conn.prepareStatement(
                "SELECT TAXCATEGORIES.NAME, SUM(ROUND(TAXLINES.AMOUNT,?)), SUM(ROUND(TAXLINES.BASE,?)) "
                        + "FROM RECEIPTS, TAXLINES, TAXES, TAXCATEGORIES, PAYMENTS "
                        + "WHERE PAYMENTS.RECEIPT=RECEIPTS.ID AND PAYMENTS.PAYMENT<>'free' AND RECEIPTS.ID=TAXLINES.RECEIPT "
                        + "AND TAXLINES.TAXID=TAXES.ID AND TAXES.CATEGORY=TAXCATEGORIES.ID AND RECEIPTS.MONEY=? "
                        + "GROUP BY TAXCATEGORIES.NAME")) {
            st.setInt(1, currencyDecimals);
            st.setInt(2, currencyDecimals);
            st.setString(3, money);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    taxBreakdown.add(new Kassenabschluss.TaxBreakdown(
                            rs.getString(1), rs.getDouble(2), rs.getDouble(3)));
                }
            }
        }

        return new Kassenabschluss(tabletId, money, session.host(), session.hostSequence(),
                session.dateStart(), dateEnd, ticketCount, cashTotal, cardTotal, paperTotal,
                cashInOutTotal, freeTotal, paymentLines, salesCount, salesBase, salesTax,
                salesBase + salesTax, taxBreakdown);
    }

    private long queryLong(Connection conn, String sql, String money) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, money);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private double queryDouble(Connection conn, String sql, String money) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, money);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    private double queryDoubleWithDecimals(Connection conn, String sql, int decimals, String money)
            throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, decimals);
            st.setString(2, money);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }
}

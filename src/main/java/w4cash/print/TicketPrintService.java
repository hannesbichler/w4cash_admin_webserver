package w4cash.print;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.printer.TicketPrinterException;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;

import w4cash.LoadDatabase;
import w4cash.W4cashApplication;
import w4cash.ticketinfo.OrderItem;
import w4cash.ticketinfo.OrderLine;

@Service
@DependsOn("loadDatabase")
public class TicketPrintService {

    String printerResource = "";
    private List<TaxInfo> taxcollection;
    // protected DataLogicSystem dlSystem;
    private static final Logger logger = LoggerFactory.getLogger(TicketPrintService.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int LINE_WIDTH = 40;
    private static final String DIVIDER = "─".repeat(LINE_WIDTH);
    private static final int ESCPOS_LINE_WIDTH = 24; // 80 mm paper, Font A
    private static final Charset ESCPOS_CHARSET = Charset.forName("IBM850");

    private final PrintJobRepository printJobRepository;

    TicketPrintService(PrintJobRepository printJobRepository) {
        this.printJobRepository = printJobRepository;
        // this.dlSystem = new DataLogicSystem();
        this.taxcollection = new ArrayList<>();
        initResourcesCache();
        // this.dlSystem.resetResourcesCache();
        // initSystemData();
    }

    private void initResourcesCache() {
        // dlSystem.resetResourcesCache();
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn
                .prepareStatement("SELECT CONTENT FROM RESOURCES WHERE NAME = 'Printer.Ticket.80mm'")) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    printerResource = new String((byte[]) rs.getBytes("CONTENT"), "UTF-8");
                    break;
                }
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /*
     * private void initSystemData() {
     * //DataLogicAdmin dlAdmin = (DataLogicAdmin)
     * m_App.getBean("com.openbravo.pos.admin.DataLogicAdmin");
     * TableDefinition tresources = dlAdmin.getTableResources();
     * 
     * try {
     * List res = tresources.getListSentence().list();
     * Object o = res.get(0);
     * // try to find System.AddressLine1
     * for (int i = 0; i < res.size(); i++) {
     * if ("System.AddressLine1".compareTo(((Object[]) res.get(i))[1].toString()) ==
     * 0) {
     * SystemDataAddressLine1 = ((Formats.BYTEA.formatValue(((Object[])
     * res.get(i))[3])));
     * 
     * continue;
     * } else if ("System.AddressLine2".compareTo(((Object[])
     * res.get(i))[1].toString()) == 0) {
     * SystemDataAddressLine2 = ((Formats.BYTEA.formatValue(((Object[])
     * res.get(i))[3])));
     * continue;
     * } else if ("System.Street".compareTo(((Object[]) res.get(i))[1].toString())
     * == 0) {
     * SystemDataStreet = ((Formats.BYTEA.formatValue(((Object[]) res.get(i))[3])));
     * continue;
     * } else if ("System.City".compareTo(((Object[]) res.get(i))[1].toString()) ==
     * 0) {
     * SystemDataCity = ((Formats.BYTEA.formatValue(((Object[]) res.get(i))[3])));
     * continue;
     * } else if ("System.TAXID".compareTo(((Object[]) res.get(i))[1].toString()) ==
     * 0) {
     * SystemDataTaxid = ((Formats.BYTEA.formatValue(((Object[]) res.get(i))[3])));
     * continue;
     * } else if ("System.Thanks".compareTo(((Object[]) res.get(i))[1].toString())
     * == 0) {
     * SystemDataThanks = ((Formats.BYTEA.formatValue(((Object[]) res.get(i))[3])));
     * continue;
     * } else if ("System.Info".compareTo(((Object[]) res.get(i))[1].toString()) ==
     * 0) {
     * SystemDataInfo = ((Formats.BYTEA.formatValue(((Object[]) res.get(i))[3])));
     * continue;
     * } else if ("System.AccountBank".compareTo(((Object[])
     * res.get(i))[1].toString()) == 0) {
     * SystemDataAccountBank = ((Formats.BYTEA.formatValue(((Object[])
     * res.get(i))[3])));
     * continue;
     * } else if ("System.AccountOwner".compareTo(((Object[])
     * res.get(i))[1].toString()) == 0) {
     * SystemDataAccountOwner = ((Formats.BYTEA.formatValue(((Object[])
     * res.get(i))[3])));
     * continue;
     * } else if ("System.AccountBIC".compareTo(((Object[])
     * res.get(i))[1].toString()) == 0) {
     * SystemDataAccountBIC = ((Formats.BYTEA.formatValue(((Object[])
     * res.get(i))[3])));
     * continue;
     * } else if ("System.AccountIBAN".compareTo(((Object[])
     * res.get(i))[1].toString()) == 0) {
     * SystemDataAccountIBAN = ((Formats.BYTEA.formatValue(((Object[])
     * res.get(i))[3])));
     * continue;
     * }
     * }
     * // res.get(0);
     * } catch (BasicException e) {
     * Log.Exception(e);
     * }
     * }
     */

    @Async
    public void printOrderTicket(String tableId, @NonNull OrderItem orderItem) {
        var printerNames = queryPrinterNames();
        String tableName = queryTableName(tableId);

        // Group lines by the printer index derived from their product's category
        Map<Integer, List<OrderLine>> linesByPrinter = new HashMap<>();
        if (orderItem != null && orderItem.getLines() != null) {
            for (OrderLine line : orderItem.getLines()) {
                if ((int) line.getNewQty() == 0) {
                    continue;
                }
                int printerIndex = queryPrinterIndexForProduct(line.getProductId());
                linesByPrinter.computeIfAbsent(printerIndex, k -> new ArrayList<>()).add(line);
            }
        }

        if (linesByPrinter.isEmpty()) {
            logger.info("No printable lines for tableId={}", tableId);
            return;
        }

        String kellner = orderItem != null ? orderItem.getKellner() : "";
        String[] person = queryPersonByName(kellner);
        String personId = person[0];
        String personName = person[1];

        for (Map.Entry<Integer, List<OrderLine>> entry : linesByPrinter.entrySet()) {
            int printerIndex = entry.getKey();
            List<OrderLine> lines = entry.getValue();
            String printerName = printerNames.get(printerIndex);
            if (printerName == null || printerName.isBlank()) {
                logger.warn("No printer configured for index={} — skipping {} lines for tableId={}",
                        printerIndex, lines.size(), tableId);
                continue;
            }

            PrintService printService = findPrintService(printerName);
            if (printService == null) {
                logger.warn("PrintService '{}' not found — skipping printer index={} for tableId={}",
                        printerName, printerIndex, tableId);
                continue;
            }

            logger.info("Printing ticket for tableId={}, printer index={} ('{}'), lines={}",
                    tableId, printerIndex, printerName, lines.size());
            printTicketForLines(tableId, tableName, kellner, lines, printService, printerName, printerIndex,
                    personId, personName);
        }
    }

    private void printTicketForLines(String tableId, String tableName, String kellner,
            List<OrderLine> lines, PrintService printService, String printerName, int printerIndex,
            String personId, String personName) {
        String errorMsg = null;
        String ticketContent = buildPlainTicket(tableName, kellner, lines);
        try {
            if (isPdfPrintService(printService)) {
                if (ticketContent.isBlank()) {
                    logger.info("Ticket content is empty for tableId={} on printer '{}', skipping", tableId,
                            printerName);
                    return;
                }
                printPlainText(printService, ticketContent);
                logger.info("Ticket printed via PDF fallback for tableId={} on printer '{}'", tableId, printerName);
            } else {
                if (ticketContent.isBlank()) {
                    logger.info("Ticket content is empty for tableId={} on printer '{}', skipping", tableId,
                            printerName);
                    return;
                }
                byte[] escPosBytes = buildEscPosBytes(ticketContent);
                DocPrintJob job = printService.createPrintJob();
                Doc doc = new SimpleDoc(escPosBytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                job.print(doc, null);
                logger.info("Ticket printed for tableId={} on printer '{}'", tableId, printerName);
            }
        } catch (Exception e) {
            errorMsg = e.getMessage();
            logger.warn("Ticket print failed for tableId={} on printer '{}': {}", tableId, printerName, e.getMessage(),
                    e);
        } finally {
            printJobRepository.save(new PrintJob(
                    tableId, tableName, printerIndex, printerName,
                    lines.size(), LocalDateTime.now(), errorMsg == null, errorMsg, ticketContent,
                    personId, personName));
        }
    }

    private byte[] buildEscPosBytes(String ticketContent) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] init = { 0x1B, 0x40 }; // ESC @ – initialize printer
        byte[] fontA = { 0x1B, 0x4D, 0x00 }; // ESC M 0 – select Font A
        byte[] codepage = { 0x1B, 0x74, 0x02 }; // ESC t 2 – select PC850 code page
        byte[] boldOn = { 0x1B, 0x45, 0x01 }; // ESC E 1 – bold on
        byte[] lineSpacing = { 0x1B, 0x33, 0x18 }; // ESC 3 24 – set line spacing to 24 dots
        byte[] textSize = { 0x1D, 0x21, 0x11 }; // GS ! 17 – double-width + double-height text
        byte[] feeds = { 0x0A, 0x0A, 0x0A, 0x0A }; // 4x line feed
        byte[] cut = { 0x1D, 0x56, 0x41, 0x00 }; // GS V A 0 – partial cut
        baos.write(init, 0, init.length);
        baos.write(fontA, 0, fontA.length);
        baos.write(codepage, 0, codepage.length);
        baos.write(boldOn, 0, boldOn.length);
        baos.write(lineSpacing, 0, lineSpacing.length);
        baos.write(textSize, 0, textSize.length);

        String dividerLine = "-".repeat(ESCPOS_LINE_WIDTH);
        String normalized = ticketContent
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace(DIVIDER, dividerLine)
                .replace('─', '-');

        for (String rawLine : normalized.split("\\n", -1)) {
            for (String wrapped : wrapEscPosLine(rawLine, ESCPOS_LINE_WIDTH)) {
                byte[] lineBytes = (wrapped + "\n").getBytes(ESCPOS_CHARSET);
                baos.write(lineBytes, 0, lineBytes.length);
            }
        }

        baos.write(feeds, 0, feeds.length);
        baos.write(cut, 0, cut.length);
        return baos.toByteArray();
    }

    private List<String> wrapEscPosLine(String line, int maxWidth) {
        List<String> wrapped = new ArrayList<>();
        if (line == null) {
            wrapped.add("");
            return wrapped;
        }

        String remaining = line;
        while (remaining.length() > maxWidth) {
            int cut = maxWidth;
            int lastSpace = remaining.lastIndexOf(' ', maxWidth);
            if (lastSpace > 0) {
                cut = lastSpace;
            }
            wrapped.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut).stripLeading();
        }
        wrapped.add(remaining);
        return wrapped;
    }

    private boolean isPdfPrintService(PrintService printService) {
        String serviceName = printService.getName() == null ? "" : printService.getName().toLowerCase();
        if (serviceName.contains("pdf")) {
            logger.debug("PDF fallback triggered by printer name match: '{}'", printService.getName());
            return true;
        }

        for (DocFlavor flavor : printService.getSupportedDocFlavors()) {
            String mimeType = flavor.getMimeType();
            if (mimeType != null && mimeType.toLowerCase().contains("pdf")) {
                logger.debug("PDF fallback triggered by MIME flavor match: printer='{}', flavor='{}'",
                        printService.getName(),
                        mimeType);
                return true;
            }
        }

        logger.debug("PDF fallback not triggered: printer='{}'", printService.getName());
        return false;
    }

    private String buildPlainTicket(String tableName, String kellner, List<OrderLine> lines) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sbLines = new StringBuilder();

        for (OrderLine line : lines) {
            int qty = (int) line.getNewQty();
            if (qty == 0) {
                continue;
            }
            String name = line.getProductName() != null ? line.getProductName() : "";
            sbLines.append(qty).append("x  ").append(name).append(System.lineSeparator());
            String att = line.getAttSetInstDesc();
            if (att != null && !att.isBlank()) {
                sbLines.append("    [").append(att.trim()).append(']').append(System.lineSeparator());
            }
        }

        if (sbLines.length() > 0) {
            sb.append("Tisch: ").append(tableName).append(System.lineSeparator());
            sb.append("KellnerIn: ").append(kellner).append(System.lineSeparator());
            sb.append(LocalDateTime.now().format(TIMESTAMP)).append(System.lineSeparator());
            sb.append(DIVIDER).append(System.lineSeparator());
            sb.append(sbLines);
            sb.append(DIVIDER).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private void printPlainText(PrintService printService, String content) throws PrintException {
        DocPrintJob job = printService.createPrintJob();
        Doc doc;
        DocFlavor selectedFlavor;

        if (printService.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
            printViaPrinterJob(printService, content);
            return;
        } else if (printService.isDocFlavorSupported(DocFlavor.STRING.TEXT_PLAIN)) {
            selectedFlavor = DocFlavor.STRING.TEXT_PLAIN;
            doc = new SimpleDoc(content, selectedFlavor, null);
        } else if (printService.isDocFlavorSupported(DocFlavor.CHAR_ARRAY.TEXT_PLAIN)) {
            selectedFlavor = DocFlavor.CHAR_ARRAY.TEXT_PLAIN;
            doc = new SimpleDoc(content.toCharArray(), selectedFlavor, null);
        } else if (printService.isDocFlavorSupported(DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8)) {
            selectedFlavor = DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8;
            doc = new SimpleDoc(content.getBytes(StandardCharsets.UTF_8), selectedFlavor, null);
        } else if (printService.isDocFlavorSupported(DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII)) {
            selectedFlavor = DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII;
            doc = new SimpleDoc(content.getBytes(StandardCharsets.US_ASCII), selectedFlavor, null);
        } else if (printService.isDocFlavorSupported(DocFlavor.INPUT_STREAM.AUTOSENSE)) {
            selectedFlavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            doc = new SimpleDoc(
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    selectedFlavor,
                    null);
        } else {
            String availableFlavors = Arrays.stream(printService.getSupportedDocFlavors())
                    .map(flavor -> flavor.getRepresentationClassName() + " | " + flavor.getMimeType())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("<none>");
            logger.warn("No supported plain-text DocFlavor for printer '{}'. Supported flavors: {}",
                    printService.getName(),
                    availableFlavors);
            throw new PrintException("No supported plain-text DocFlavor for " + printService.getName());
        }

        job.print(doc, null);
        logger.info("Submitted plain-text print job to '{}' (chars={}, flavor='{}')",
                printService.getName(),
                content.length(),
                selectedFlavor);
    }

    /**
     * Prints via PrinterJob so the PageFormat can be controlled: the imageable
     * area is set to start at 10,10 (printer points, 1/72"). validatePage()
     * clamps that back inside the printer's hardware margins if necessary.
     */
    private void printViaPrinterJob(PrintService printService, String content) throws PrintException {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        try {
            printerJob.setPrintService(printService);

            PageFormat pageFormat = printerJob.defaultPage();
            Paper paper = pageFormat.getPaper();
            paper.setImageableArea(10, 10, 180, paper.getHeight() - 20);
            pageFormat.setPaper(paper);
            pageFormat = printerJob.validatePage(pageFormat);

            printerJob.setPrintable(new PlainTextPrintable(content), pageFormat);
            printerJob.print();
        } catch (PrinterException e) {
            throw new PrintException("Printing via PrinterJob failed for " + printService.getName(), e);
        }

        logger.info("Submitted plain-text print job to '{}' (chars={}, imageable origin 10,10)",
                printService.getName(),
                content.length());
    }

    /**
     * Renders a ticket with the header (everything up to and including the first
     * DIVIDER line) in a smaller font and the order lines below it in a larger
     * one. The header only appears on the first page; pagination of the order
     * lines accounts for the space it occupies there.
     */
    private static final class PlainTextPrintable implements Printable {
        private static final int FONT_SIZE = 14;
        private static final int FONT_SIZE_LINES = 18;
        private static final String FONT_NAME_LINES = "Arial Narrow";
        private static final String CONTINUATION_INDENT = "    ";

        private final String[] header;
        private final String[] lines;

        private PlainTextPrintable(String content) {
            String[] all = content.split("\\R", -1);
            int dividerIdx = -1;
            for (int i = 0; i < all.length; i++) {
                if (DIVIDER.equals(all[i])) {
                    dividerIdx = i;
                    break;
                }
            }
            if (dividerIdx >= 0) {
                this.header = Arrays.copyOfRange(all, 0, dividerIdx + 1);
                this.lines = Arrays.copyOfRange(all, dividerIdx + 1, all.length);
            } else {
                this.header = new String[0];
                this.lines = all;
            }
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            Graphics2D g2 = (Graphics2D) graphics;
            Font headerFont = new Font(Font.SANS_SERIF, Font.BOLD, FONT_SIZE);
            Font lineFont = new Font(FONT_NAME_LINES, Font.BOLD, FONT_SIZE_LINES);
            FontMetrics headerMetrics = g2.getFontMetrics(headerFont);
            FontMetrics lineMetrics = g2.getFontMetrics(lineFont);
            double maxWidth = pageFormat.getImageableWidth();

            List<Segment> headerSegments = new ArrayList<>();
            for (String headerLine : header) {
                if (DIVIDER.equals(headerLine)) {
                    headerSegments.add(new Segment(fitDivider(headerMetrics, maxWidth), false));
                    continue;
                }
                boolean big = headerLine.startsWith("Tisch:");
                for (String piece : wrap(headerLine, big ? lineMetrics : headerMetrics, maxWidth)) {
                    headerSegments.add(new Segment(piece, big));
                }
            }
            List<Segment> bodySegments = new ArrayList<>();
            for (String line : lines) {
                if (DIVIDER.equals(line)) {
                    bodySegments.add(new Segment(fitDivider(headerMetrics, maxWidth), false));
                    continue;
                }
                for (String piece : wrap(line, lineMetrics, maxWidth)) {
                    bodySegments.add(new Segment(piece, true));
                }
            }

            int lineHeight = lineMetrics.getHeight();
            double pageHeight = pageFormat.getImageableHeight();
            int headerHeight = 0;
            for (Segment segment : headerSegments) {
                headerHeight += segment.big ? lineHeight : headerMetrics.getHeight();
            }
            int firstPageCapacity = Math.max(1, (int) ((pageHeight - headerHeight) / lineHeight));
            int otherPageCapacity = Math.max(1, (int) (pageHeight / lineHeight));

            int start = pageIndex == 0 ? 0 : firstPageCapacity + (pageIndex - 1) * otherPageCapacity;
            if (pageIndex > 0 && start >= bodySegments.size()) {
                return NO_SUCH_PAGE;
            }

            float x = (float) pageFormat.getImageableX();
            float y = (float) pageFormat.getImageableY();

            if (pageIndex == 0) {
                for (Segment segment : headerSegments) {
                    FontMetrics metrics = segment.big ? lineMetrics : headerMetrics;
                    g2.setFont(segment.big ? lineFont : headerFont);
                    g2.drawString(segment.text, x, y + metrics.getAscent());
                    y += metrics.getHeight();
                }
            }

            int capacity = pageIndex == 0 ? firstPageCapacity : otherPageCapacity;
            int end = Math.min(bodySegments.size(), start + capacity);
            for (int i = start; i < end; i++) {
                Segment segment = bodySegments.get(i);
                FontMetrics metrics = segment.big ? lineMetrics : headerMetrics;
                g2.setFont(segment.big ? lineFont : headerFont);
                g2.drawString(segment.text, x, y + metrics.getAscent());
                y += metrics.getHeight();
            }

            return PAGE_EXISTS;
        }

        /**
         * One rendered line after wrapping; big = order-line font, otherwise header
         * font.
         */
        private static final class Segment {
            final String text;
            final boolean big;

            Segment(String text, boolean big) {
                this.text = text;
                this.big = big;
            }
        }

        /** Builds a divider with exactly as many '─' chars as fit into maxWidth. */
        private static String fitDivider(FontMetrics metrics, double maxWidth) {
            int charWidth = metrics.charWidth('─');
            int count = charWidth > 0 ? (int) (maxWidth / charWidth) : LINE_WIDTH;
            return "─".repeat(Math.max(1, count));
        }

        /**
         * Breaks text into pieces no wider than maxWidth, preferring breaks at
         * spaces. Continuation pieces after a break are indented; the indent
         * counts toward their width.
         */
        private static List<String> wrap(String text, FontMetrics metrics, double maxWidth) {
            List<String> pieces = new ArrayList<>();
            String indent = "";
            String remaining = text;
            while (metrics.stringWidth(indent + remaining) > maxWidth && remaining.length() > 1) {
                int cut = remaining.length() - 1;
                while (cut > 1 && metrics.stringWidth(indent + remaining.substring(0, cut)) > maxWidth) {
                    cut--;
                }
                int breakAt = cut;
                for (int i = cut - 1; i > 0; i--) {
                    char c = remaining.charAt(i);
                    if (c == ' ') {
                        breakAt = i; // space is dropped by stripLeading below
                        break;
                    }
                    if (c == '/' || c == '-') {
                        breakAt = i + 1; // keep the separator at the end of the line
                        break;
                    }
                }
                pieces.add(indent + remaining.substring(0, breakAt));
                remaining = remaining.substring(breakAt).stripLeading();
                indent = CONTINUATION_INDENT;
            }
            pieces.add(indent + remaining);
            return pieces;
        }
    }

    public boolean reprint(PrintJob job) {
        if (job.getContent() == null || job.getContent().isBlank()) {
            logger.warn("reprint: no stored content for printJobId={}", job.getId());
            return false;
        }
        PrintService printService = findPrintService(job.getPrinterName());
        if (printService == null) {
            logger.warn("reprint: printer '{}' not found for printJobId={}", job.getPrinterName(), job.getId());
            return false;
        }
        try {
            printPlainText(printService, job.getContent());
            logger.info("reprint: submitted printJobId={} to printer '{}'", job.getId(), job.getPrinterName());
            printJobRepository.updateSuccess(job.getId(), true, null);
            return true;
        } catch (PrintException e) {
            logger.warn("reprint: failed for printJobId={}: {}", job.getId(), e.getMessage(), e);
            printJobRepository.updateSuccess(job.getId(), false, e.getMessage());
            return false;
        }
    }

    private String[] queryPersonByName(String name) {
        if (name == null || name.isBlank())
            return new String[] { "", "" };
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn.prepareStatement(
                "SELECT ID, NAME FROM PEOPLE WHERE NAME = ?")) {
            st.setString(1, name);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new String[] { rs.getString("ID"), rs.getString("NAME") };
                }
            }
        } catch (SQLException e) {
            logger.warn("Could not query person for name='{}': {}", name, e.getMessage());
        }
        return new String[] { "", name };
    }

    private Map<Integer, String> queryPrinterNames() {
        var printersConfig = new HashMap<Integer, String>();
        printersConfig.put(1, W4cashApplication.APP_CONFIG.getProperty("machine.printer"));
        printersConfig.put(2, W4cashApplication.APP_CONFIG.getProperty("machine.printer.2"));
        printersConfig.put(3, W4cashApplication.APP_CONFIG.getProperty("machine.printer.3"));
        return printersConfig;
    }

    private int queryPrinterIndexForProduct(String productId) {
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn.prepareStatement(
                "SELECT c.PRINTER FROM PRODUCTS p JOIN CATEGORIES c ON p.CATEGORY = c.ID WHERE p.ID = ?")) {
            st.setString(1, productId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    int p = rs.getInt("PRINTER");
                    return p > 0 ? p : 1;
                }
            }
        } catch (SQLException e) {
            logger.warn("Could not query printer index for productId={}: {}", productId, e.getMessage());
        }
        return 1;
    }

    private String queryTableName(String tableId) {
        try (Connection conn = LoadDatabase.getConnection();
        		PreparedStatement st = conn.prepareStatement(
                "SELECT NAME FROM PLACES WHERE ID = ?")) {
            st.setString(1, tableId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("NAME");
                }
            }
        } catch (SQLException e) {
            logger.warn("Could not query table name for id={}: {}", tableId, e.getMessage());
        }
        return tableId;
    }

    private PrintService findPrintService(String name) {
        if (name.contains(":")) {
            name = name.split(":", 2)[1].trim();
        }
        name = name.split(",")[0].trim();
        for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (service.getName().equalsIgnoreCase(name)) {
                return service;
            }
        }
        return null;
    }

    public void printPayment(String sresourcename, TicketInfo ticket, Object ticketext) {

        String[] printerdata = W4cashApplication.APP_CONFIG.getProperty("machine.printer").split(",");
        String printerSubName = null;

        // find customer printer
        if (ticket != null && ticket.getCustomer() != null) {
            String printerCustomer = W4cashApplication.APP_CONFIG.getProperty("machine.printer.customer");
            if (printerCustomer != null) {
                printerdata = printerCustomer.split(",");
                printerSubName = "customer";
            }
        }

        if (printerdata.length > 2) {
            sresourcename = sresourcename.replace("{size}", printerdata[2]);

            if ("Printer.TicketPreview".equals(sresourcename)) {
                sresourcename = sresourcename + "." + printerdata[2];
            }
        }

        String sresource = printerResource;// dlSystem.getResourceAsXML(sresourcename);
        if (printerSubName != null) {
            sresource = sresource.replaceAll("<ticket>", "<ticket printer=\"" + printerSubName + "\">");
        }

        try {
            ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
            script.put("taxes", taxcollection);
            /*
             * if (taxeslogic != null) {
             * script.put("taxeslogic", taxeslogic);
             * try {
             * taxeslogic.calculateTaxes(ticket);
             * } catch (Exception ex) {
             * 
             * }
             * }
             */
            script.put("ticket", ticket);
            script.put("place", ticketext != null && ticketext.getClass().equals(String.class)
                    && !ticketext.toString().endsWith("$") ? ticketext.toString().replace("&", "&amp;") : "");
            // script.put("host", m_App.getHost());

            // script.put("SystemDataAddresLine1", SystemDataAddressLine1);
            // script.put("SystemDataAddresLine2", SystemDataAddressLine2);
            // script.put("SystemDataStreet", SystemDataStreet);
            // script.put("SystemDataCity", SystemDataCity);
            // script.put("SystemDataTaxid", SystemDataTaxid);
            // script.put("SystemDataThanks", SystemDataThanks);
            // script.put("SystemDataInfo", SystemDataInfo);
            // script.put("SystemDataAccountBank", SystemDataAccountBank);
            // script.put("SystemDataAccountOwner", SystemDataAccountOwner);
            // script.put("SystemDataAccountBIC", SystemDataAccountBIC);
            // script.put("SystemDataAccountIBAN", SystemDataAccountIBAN);
            String tt = script.eval(sresource).toString();
            System.out.println(tt);
            // One parser per print: TicketParser keeps parse state in instance
            // fields and holds an output device for the whole receipt, so a
            // shared instance would let concurrent prints interleave into one
            // corrupt receipt.
            new TicketParser().printTicket(tt);
        } catch (ScriptException e) {
            e.printStackTrace();
            // MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
            // AppLocal.getIntString("message.cannotprintticket"), e);
            // msg.show(m_App, JPanelTicket.this);

            // JConfirmDialog.showError(m_App, this, AppLocal.getIntString("error.network"),
            // AppLocal.getIntString("message.cannotprintticket"), e);
            // } catch (TicketPrinterException e) {
            // MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
            // AppLocal.getIntString("message.cannotprintticket"), e);
            // msg.show(m_App, JPanelTicket.this);
            // JConfirmDialog.showError(m_App, this, AppLocal.getIntString("error.network"),
            // AppLocal.getIntString("message.cannotprintticket"), e);
        } catch (TicketPrinterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (sresource == null) {
            // TODO add ticket to print job repository with error message
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    AppLocal.getIntString("message.cannotprintticket"));
            // msg.show(m_App, JPanelTicket.this);
        }
    }
}

package w4cash.print;

import java.io.ByteArrayOutputStream;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.SimpleDoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openbravo.pos.printer.escpos.PrinterWritter;

class PrinterWritterPrintService extends PrinterWritter {

    private static final Logger logger = LoggerFactory.getLogger(PrinterWritterPrintService.class);

    private final PrintService printService;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    PrinterWritterPrintService(PrintService printService) {
        this.printService = printService;
    }

    @Override
    protected void internalWrite(byte[] bytes) {
        buffer.write(bytes, 0, bytes.length);
        logger.debug("Printer writer buffered {} bytes (total buffered={}) for '{}'",
                bytes.length,
                buffer.size(),
                printService.getName());
    }

    @Override
    protected void internalFlush() {
        if (buffer.size() == 0) {
            logger.debug("Printer writer flush skipped: buffer is empty for '{}'", printService.getName());
            return;
        }

        int payloadBytes = buffer.size();
        try {
            logger.info("Sending {} bytes to print service '{}'", payloadBytes, printService.getName());
            DocPrintJob job = printService.createPrintJob();
            Doc doc = new SimpleDoc(buffer.toByteArray(), DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, null);
            logger.info("Print job submitted successfully: {} bytes to '{}'", payloadBytes, printService.getName());
        } catch (PrintException e) {
            throw new RuntimeException("Failed to send ticket to printer " + printService.getName(), e);
        } finally {
            buffer.reset();
        }
    }

    @Override
    protected void internalClose() {
        logger.debug("Printer writer close called for '{}'", printService.getName());
        internalFlush();
    }
}

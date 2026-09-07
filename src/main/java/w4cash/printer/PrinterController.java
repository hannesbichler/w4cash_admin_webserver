package w4cash.printer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import w4cash.W4cashApplication;

// Configures which installed OS print service handles each printer "slot"
// (1/2/3, plus the special "customer" slot) - the same machine.printer*
// AppConfig properties TicketPrintService#queryPrinterNames/#printPayment
// already read at print time. Driver-type/serial/JavaPOS configuration
// (com.openbravo.pos.config.JPanelConfigGeneral in the desktop app) stays out
// of scope: TicketPrintService only ever resolves an OS PrintService by name,
// so that's the only thing worth configuring here.
@RestController
class PrinterController {

	private static final Logger logger = LoggerFactory.getLogger(PrinterController.class);
	private static final String NOT_DEFINED = "Not defined";
	private static final List<String> SLOTS = List.of("1", "2", "3", "customer");

	@GetMapping("/printers/installed")
	List<String> installed() {
		List<String> names = new ArrayList<>();
		for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) {
			names.add(service.getName());
		}
		return names;
	}

	@GetMapping("/printers/config")
	List<PrinterSlotConfig> config() {
		List<PrinterSlotConfig> result = new ArrayList<>();
		for (String slot : SLOTS) {
			result.add(new PrinterSlotConfig(slot, W4cashApplication.APP_CONFIG.getProperty(propertyKey(slot))));
		}
		return result;
	}

	@PutMapping("/printers/config/{slot}")
	ResponseEntity<?> configure(@PathVariable String slot, @RequestBody PrinterSlotConfig body) {
		if (!SLOTS.contains(slot)) {
			return ResponseEntity.badRequest()
					.body("Unknown printer slot: " + slot + " (must be one of " + SLOTS + ")");
		}

		String printerName = body.printerName();
		if (printerName == null || printerName.isBlank()) {
			return ResponseEntity.badRequest()
					.body("printerName is required (use \"" + NOT_DEFINED + "\" to clear a slot)");
		}
		if (!NOT_DEFINED.equals(printerName) && !isInstalled(printerName)) {
			return ResponseEntity.badRequest()
					.body("Unknown printer: '" + printerName + "' is not an installed print service");
		}

		W4cashApplication.APP_CONFIG.setProperty(propertyKey(slot), printerName);
		try {
			W4cashApplication.APP_CONFIG.save();
		} catch (IOException e) {
			logger.error("Failed to persist printer config for slot={}", slot, e);
			return ResponseEntity.internalServerError().body("Failed to save printer config: " + e.getMessage());
		}

		logger.info("Configured printer slot={} -> '{}'", slot, printerName);
		return ResponseEntity.ok(new PrinterSlotConfig(slot, printerName));
	}

	private boolean isInstalled(String name) {
		for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) {
			if (service.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	private static String propertyKey(String slot) {
		if ("customer".equals(slot)) {
			return "machine.printer.customer";
		}
		return "1".equals(slot) ? "machine.printer" : "machine.printer." + slot;
	}
}

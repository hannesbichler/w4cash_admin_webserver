package w4cash.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import net.sf.jasperreports.engine.JRException;

// Upload/list/delete/run JasperReports .jrxml templates. Designing the .jrxml itself stays an
// external step (Jaspersoft Studio or similar) - see w4cash.report.ReportsRepository for the
// fill/export pipeline.
@RestController
@RequestMapping({ "", "/api" })
class ReportsController {

	private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);
	private final ReportsRepository repository;

	ReportsController(ReportsRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/reports")
	ResponseEntity<?> all() {
		try {
			List<ReportRef> result = new ArrayList<>();
			for (String name : repository.listNames()) {
				result.add(new ReportRef(name, repository.parameters(name)));
			}
			return ResponseEntity.ok(result);
		} catch (JRException e) {
			logger.error("Failed to list reports", e);
			return ResponseEntity.internalServerError().body("Failed to list reports: " + e.getMessage());
		}
	}

	@PostMapping("/reports")
	ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
		String name = sanitizeName(file.getOriginalFilename());
		if (name == null || name.isBlank()) {
			return ResponseEntity.badRequest().body("Upload a .jrxml file with a valid name");
		}
		try {
			repository.upload(name, file.getBytes());
			return ResponseEntity.status(HttpStatus.CREATED).body(new ReportRef(name, repository.parameters(name)));
		} catch (JRException e) {
			logger.error("Invalid report template: {}", name, e);
			return ResponseEntity.badRequest().body("Invalid .jrxml template: " + e.getMessage());
		} catch (IOException e) {
			logger.error("Failed to save report template: {}", name, e);
			return ResponseEntity.internalServerError().body("Failed to save report template: " + e.getMessage());
		}
	}

	@DeleteMapping("/reports/{name}")
	ResponseEntity<?> delete(@PathVariable String name) {
		String safeName = sanitizeName(name);
		if (safeName == null || !repository.delete(safeName)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No report named " + name);
		}
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/reports/{name}/run")
	ResponseEntity<?> run(@PathVariable String name, @RequestBody(required = false) Map<String, String> parameters) {
		String safeName = sanitizeName(name);
		if (safeName == null || !repository.exists(safeName)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No report named " + name);
		}
		try {
			byte[] pdf = repository.run(safeName, parameters);
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + ".pdf\"")
					.body(pdf);
		} catch (JRException e) {
			logger.error("Failed to run report {}", name, e);
			return ResponseEntity.internalServerError().body("Failed to run report: " + e.getMessage());
		}
	}

	private static String sanitizeName(String raw) {
		if (raw == null) {
			return null;
		}
		String name = raw;
		int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		if (name.toLowerCase().endsWith(".jrxml")) {
			name = name.substring(0, name.length() - ".jrxml".length());
		}
		name = name.replaceAll("[^a-zA-Z0-9_-]", "");
		return name.isBlank() ? null : name;
	}
}

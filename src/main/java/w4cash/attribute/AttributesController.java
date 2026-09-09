package w4cash.attribute;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Master list of ATTRIBUTE rows (e.g. "Size", "Color"), independent of any attribute set.
@RestController
@RequestMapping({ "", "/api" })
class AttributesController {

	private static final Logger logger = LoggerFactory.getLogger(AttributesController.class);
	private final AttributesRepository repository;

	AttributesController(AttributesRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/attributes")
	ResponseEntity<?> all() {
		try {
			return ResponseEntity.ok(repository.findAll());
		} catch (SQLException e) {
			logger.error("Failed to load attributes", e);
			return ResponseEntity.internalServerError().body("Failed to load attributes");
		}
	}

	@PostMapping("/attributes")
	ResponseEntity<?> create(@RequestBody AttributeRef body) {
		if (body.name() == null || body.name().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		try {
			return ResponseEntity.status(HttpStatus.CREATED).body(repository.insert(body.name()));
		} catch (SQLException e) {
			logger.error("Failed to create attribute", e);
			return ResponseEntity.internalServerError().body("Failed to create attribute: " + e.getMessage());
		}
	}

	@PutMapping("/attributes/{id}")
	ResponseEntity<?> update(@PathVariable String id, @RequestBody AttributeRef body) {
		if (body.name() == null || body.name().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		try {
			if (!repository.update(id, body.name())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute with id=" + id);
			}
			return ResponseEntity.ok(new AttributeRef(id, body.name()));
		} catch (SQLException e) {
			logger.error("Failed to update attribute id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to update attribute: " + e.getMessage());
		}
	}

	@DeleteMapping("/attributes/{id}")
	ResponseEntity<?> delete(@PathVariable String id) {
		try {
			if (!repository.deleteById(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute with id=" + id);
			}
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete attribute id={}", id, e);
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Failed to delete attribute (it may still be used by an attribute set): " + e.getMessage());
		}
	}
}

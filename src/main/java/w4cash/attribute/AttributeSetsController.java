package w4cash.attribute;

import java.sql.SQLException;
import java.util.List;

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

// Attribute sets (ATTRIBUTESET) and which attributes belong to each, in order (ATTRIBUTEUSE).
// Used to populate a product's "attribute set" picker and to let admins define what a set contains.
@RestController
@RequestMapping({ "", "/api" })
class AttributeSetsController {

	private static final Logger logger = LoggerFactory.getLogger(AttributeSetsController.class);
	private final AttributeSetsRepository repository;
	private final AttributesRepository attributesRepository;

	AttributeSetsController(AttributeSetsRepository repository, AttributesRepository attributesRepository) {
		this.repository = repository;
		this.attributesRepository = attributesRepository;
	}

	@GetMapping("/attribute-sets")
	ResponseEntity<?> all() {
		try {
			return ResponseEntity.ok(repository.findAll());
		} catch (SQLException e) {
			logger.error("Failed to load attribute sets", e);
			return ResponseEntity.internalServerError().body("Failed to load attribute sets");
		}
	}

	@GetMapping("/attribute-sets/{id}")
	ResponseEntity<?> one(@PathVariable String id) {
		try {
			return repository.findById(id)
					.<ResponseEntity<?>>map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute set with id=" + id));
		} catch (SQLException e) {
			logger.error("Failed to load attribute set id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to load attribute set");
		}
	}

	@PostMapping("/attribute-sets")
	ResponseEntity<?> create(@RequestBody AttributeSetRef body) {
		if (body.name() == null || body.name().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		try {
			return ResponseEntity.status(HttpStatus.CREATED).body(repository.insert(body.name()));
		} catch (SQLException e) {
			logger.error("Failed to create attribute set", e);
			return ResponseEntity.internalServerError().body("Failed to create attribute set: " + e.getMessage());
		}
	}

	@PutMapping("/attribute-sets/{id}")
	ResponseEntity<?> rename(@PathVariable String id, @RequestBody AttributeSetRef body) {
		if (body.name() == null || body.name().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		try {
			if (!repository.rename(id, body.name())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute set with id=" + id);
			}
			return one(id);
		} catch (SQLException e) {
			logger.error("Failed to rename attribute set id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to rename attribute set: " + e.getMessage());
		}
	}

	@DeleteMapping("/attribute-sets/{id}")
	ResponseEntity<?> delete(@PathVariable String id) {
		try {
			if (!repository.deleteById(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute set with id=" + id);
			}
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete attribute set id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to delete attribute set: " + e.getMessage());
		}
	}

	@PostMapping("/attribute-sets/{setId}/attributes/{attributeId}")
	ResponseEntity<?> addAttribute(@PathVariable String setId, @PathVariable String attributeId) {
		try {
			if (!repository.setExists(setId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute set with id=" + setId);
			}
			if (attributesRepository.findById(attributeId).isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute with id=" + attributeId);
			}
			if (repository.existsAttributeUse(setId, attributeId)) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("Attribute already in this set");
			}
			repository.addAttributeToSet(setId, attributeId);
			return one(setId);
		} catch (SQLException e) {
			logger.error("Failed to add attribute {} to set {}", attributeId, setId, e);
			return ResponseEntity.internalServerError().body("Failed to add attribute to set: " + e.getMessage());
		}
	}

	@DeleteMapping("/attribute-sets/{setId}/attributes/{attributeId}")
	ResponseEntity<?> removeAttribute(@PathVariable String setId, @PathVariable String attributeId) {
		try {
			if (!repository.removeAttributeFromSet(setId, attributeId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Attribute not in this set");
			}
			return one(setId);
		} catch (SQLException e) {
			logger.error("Failed to remove attribute {} from set {}", attributeId, setId, e);
			return ResponseEntity.internalServerError().body("Failed to remove attribute from set: " + e.getMessage());
		}
	}

	@PutMapping("/attribute-sets/{setId}/attribute-order")
	ResponseEntity<?> reorder(@PathVariable String setId, @RequestBody List<String> attributeIdsInOrder) {
		try {
			if (!repository.setExists(setId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute set with id=" + setId);
			}
			repository.reorder(setId, attributeIdsInOrder);
			return one(setId);
		} catch (SQLException e) {
			logger.error("Failed to reorder attributes for set {}", setId, e);
			return ResponseEntity.internalServerError().body("Failed to reorder attributes: " + e.getMessage());
		}
	}
}

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
import org.springframework.web.bind.annotation.RestController;

// Allowed values for an attribute (ATTRIBUTEVALUE), e.g. attribute "Size" -> values S, M, L, in order.
@RestController
class AttributeValuesController {

	private static final Logger logger = LoggerFactory.getLogger(AttributeValuesController.class);
	private final AttributeValuesRepository repository;
	private final AttributesRepository attributesRepository;

	AttributeValuesController(AttributeValuesRepository repository, AttributesRepository attributesRepository) {
		this.repository = repository;
		this.attributesRepository = attributesRepository;
	}

	@GetMapping("/attributes/{attributeId}/values")
	ResponseEntity<?> all(@PathVariable String attributeId) {
		try {
			if (attributesRepository.findById(attributeId).isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute with id=" + attributeId);
			}
			return ResponseEntity.ok(repository.findAllForAttribute(attributeId));
		} catch (SQLException e) {
			logger.error("Failed to load values for attribute {}", attributeId, e);
			return ResponseEntity.internalServerError().body("Failed to load attribute values");
		}
	}

	@PostMapping("/attributes/{attributeId}/values")
	ResponseEntity<?> create(@PathVariable String attributeId, @RequestBody AttributeValueRef body) {
		if (body.value() == null || body.value().isBlank()) {
			return ResponseEntity.badRequest().body("value is required");
		}
		try {
			if (attributesRepository.findById(attributeId).isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute with id=" + attributeId);
			}
			return ResponseEntity.status(HttpStatus.CREATED).body(repository.insert(attributeId, body.value()));
		} catch (SQLException e) {
			logger.error("Failed to create value for attribute {}", attributeId, e);
			return ResponseEntity.internalServerError().body("Failed to create attribute value: " + e.getMessage());
		}
	}

	@PutMapping("/attributes/{attributeId}/values/{valueId}")
	ResponseEntity<?> update(@PathVariable String attributeId, @PathVariable String valueId,
			@RequestBody AttributeValueRef body) {
		if (body.value() == null || body.value().isBlank()) {
			return ResponseEntity.badRequest().body("value is required");
		}
		try {
			if (!repository.update(attributeId, valueId, body.value())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No value with id=" + valueId);
			}
			return ResponseEntity.ok(new AttributeValueRef(valueId, body.value(), body.lineno()));
		} catch (SQLException e) {
			logger.error("Failed to update value {} for attribute {}", valueId, attributeId, e);
			return ResponseEntity.internalServerError().body("Failed to update attribute value: " + e.getMessage());
		}
	}

	@DeleteMapping("/attributes/{attributeId}/values/{valueId}")
	ResponseEntity<?> delete(@PathVariable String attributeId, @PathVariable String valueId) {
		try {
			if (!repository.deleteById(attributeId, valueId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No value with id=" + valueId);
			}
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete value {} for attribute {}", valueId, attributeId, e);
			return ResponseEntity.internalServerError().body("Failed to delete attribute value: " + e.getMessage());
		}
	}

	@PutMapping("/attributes/{attributeId}/values-order")
	ResponseEntity<?> reorder(@PathVariable String attributeId, @RequestBody List<String> valueIdsInOrder) {
		try {
			if (attributesRepository.findById(attributeId).isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attribute with id=" + attributeId);
			}
			repository.reorder(attributeId, valueIdsInOrder);
			return ResponseEntity.ok(repository.findAllForAttribute(attributeId));
		} catch (SQLException e) {
			logger.error("Failed to reorder values for attribute {}", attributeId, e);
			return ResponseEntity.internalServerError().body("Failed to reorder attribute values: " + e.getMessage());
		}
	}
}

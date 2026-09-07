package w4cash.taxcategory;

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
import org.springframework.web.bind.annotation.RestController;

// Tax categories (TAXCATEGORIES) and their default rate history (TAXES, see TaxesRepository
// for the "default rows only" scoping). Used both to populate a product's tax-category
// picker and to let admins define/edit rates directly.
@RestController
class TaxCategoriesController {

	private static final Logger logger = LoggerFactory.getLogger(TaxCategoriesController.class);
	private final TaxCategoriesRepository repository;
	private final TaxesRepository taxesRepository;

	TaxCategoriesController(TaxCategoriesRepository repository, TaxesRepository taxesRepository) {
		this.repository = repository;
		this.taxesRepository = taxesRepository;
	}

	@GetMapping("/tax-categories")
	ResponseEntity<?> all() {
		try {
			return ResponseEntity.ok(repository.findAll());
		} catch (SQLException e) {
			logger.error("Failed to load tax categories", e);
			return ResponseEntity.internalServerError().body("Failed to load tax categories");
		}
	}

	@GetMapping("/tax-categories/{id}")
	ResponseEntity<?> one(@PathVariable String id) {
		try {
			var category = repository.findById(id);
			if (category.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No tax category with id=" + id);
			}
			var rates = taxesRepository.findAllForCategory(id);
			return ResponseEntity.ok(new TaxCategoryDetail(category.get().id(), category.get().name(), rates));
		} catch (SQLException e) {
			logger.error("Failed to load tax category id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to load tax category");
		}
	}

	@PostMapping("/tax-categories")
	ResponseEntity<?> create(@RequestBody TaxCategoryRef body) {
		if (body.name() == null || body.name().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		try {
			return ResponseEntity.status(HttpStatus.CREATED).body(repository.insert(body.name()));
		} catch (SQLException e) {
			logger.error("Failed to create tax category", e);
			return ResponseEntity.internalServerError().body("Failed to create tax category: " + e.getMessage());
		}
	}

	@PutMapping("/tax-categories/{id}")
	ResponseEntity<?> rename(@PathVariable String id, @RequestBody TaxCategoryRef body) {
		if (body.name() == null || body.name().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		try {
			if (!repository.rename(id, body.name())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No tax category with id=" + id);
			}
			return ResponseEntity.ok(new TaxCategoryRef(id, body.name()));
		} catch (SQLException e) {
			logger.error("Failed to rename tax category id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to rename tax category: " + e.getMessage());
		}
	}

	@DeleteMapping("/tax-categories/{id}")
	ResponseEntity<?> delete(@PathVariable String id) {
		try {
			if (!repository.deleteById(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No tax category with id=" + id);
			}
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete tax category id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to delete tax category: " + e.getMessage());
		}
	}

	@PostMapping("/tax-categories/{categoryId}/rates")
	ResponseEntity<?> createRate(@PathVariable String categoryId, @RequestBody TaxRateRef body) {
		if (body.name() == null || body.name().isBlank() || body.validFrom() == null || body.validFrom().isBlank()) {
			return ResponseEntity.badRequest().body("name and validFrom are required");
		}
		try {
			if (!repository.exists(categoryId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No tax category with id=" + categoryId);
			}
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(taxesRepository.insert(categoryId, body.name(), body.rate(), body.validFrom()));
		} catch (SQLException e) {
			logger.error("Failed to create rate for tax category {}", categoryId, e);
			return ResponseEntity.internalServerError().body("Failed to create rate: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("validFrom must be a yyyy-MM-dd date");
		}
	}

	@PutMapping("/tax-categories/{categoryId}/rates/{taxId}")
	ResponseEntity<?> updateRate(@PathVariable String categoryId, @PathVariable String taxId,
			@RequestBody TaxRateRef body) {
		if (body.name() == null || body.name().isBlank() || body.validFrom() == null || body.validFrom().isBlank()) {
			return ResponseEntity.badRequest().body("name and validFrom are required");
		}
		try {
			if (!taxesRepository.update(categoryId, taxId, body.name(), body.rate(), body.validFrom())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No rate with id=" + taxId);
			}
			return ResponseEntity.ok(new TaxRateRef(taxId, body.name(), body.rate(), body.validFrom()));
		} catch (SQLException e) {
			logger.error("Failed to update rate {} for tax category {}", taxId, categoryId, e);
			return ResponseEntity.internalServerError().body("Failed to update rate: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("validFrom must be a yyyy-MM-dd date");
		}
	}

	@DeleteMapping("/tax-categories/{categoryId}/rates/{taxId}")
	ResponseEntity<?> deleteRate(@PathVariable String categoryId, @PathVariable String taxId) {
		try {
			if (!taxesRepository.deleteById(categoryId, taxId)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No rate with id=" + taxId);
			}
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete rate {} for tax category {}", taxId, categoryId, e);
			return ResponseEntity.internalServerError().body("Failed to delete rate: " + e.getMessage());
		}
	}
}

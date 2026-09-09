package w4cash.product;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "", "/api" })
class ProductsController {

	private static final Logger logger = LoggerFactory.getLogger(ProductsController.class);
	private final ProductsRepository repository;

	ProductsController(ProductsRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/products")
	ResponseEntity<?> all(@RequestParam(required = false) String categoryId) {
		logger.info("GET /products request was called, categoryId={}", categoryId);
		try {
			List<EntityModel<Product>> products = repository.findAll(categoryId).stream()
					.map(EntityModel::of)
					.collect(Collectors.toList());
			return ResponseEntity.ok(Objects.requireNonNull(CollectionModel.of(Objects.requireNonNull(products),
					linkTo(methodOn(ProductsController.class).all(categoryId)).withSelfRel())));
		} catch (SQLException e) {
			logger.error("Failed to load products, categoryId={}", categoryId, e);
			return ResponseEntity.internalServerError().body("Failed to load products");
		}
	}

	@GetMapping("/products/{id}")
	ResponseEntity<?> one(@PathVariable String id) {
		try {
			return repository.findById(id)
					.<ResponseEntity<?>>map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("No product with id=" + id));
		} catch (SQLException e) {
			logger.error("Failed to load product id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to load product");
		}
	}

	@PostMapping("/products")
	ResponseEntity<?> create(@RequestBody Product newProduct) {
		try {
			Product saved = repository.insert(newProduct);
			return ResponseEntity.status(HttpStatus.CREATED).body(saved);
		} catch (SQLException e) {
			logger.error("Failed to create product", e);
			return ResponseEntity.internalServerError().body("Failed to create product: " + e.getMessage());
		}
	}

	@PutMapping("/products/{id}")
	ResponseEntity<?> update(@PathVariable String id, @RequestBody Product updatedProduct) {
		try {
			if (!repository.update(id, updatedProduct)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No product with id=" + id);
			}
			updatedProduct.setId(id);
			return ResponseEntity.ok(updatedProduct);
		} catch (SQLException e) {
			logger.error("Failed to update product id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to update product: " + e.getMessage());
		}
	}

	@DeleteMapping("/products/{id}")
	ResponseEntity<?> delete(@PathVariable String id) {
		try {
			if (!repository.deleteById(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No product with id=" + id);
			}
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete product id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to delete product: " + e.getMessage());
		}
	}
}

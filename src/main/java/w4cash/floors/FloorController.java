package w4cash.floors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import w4cash.LoadDatabase;

@RestController
@RequestMapping({ "", "/api" })
class FloorController {

	private static final Logger logger = LoggerFactory.getLogger(FloorController.class);

	private final FloorsRepository repository;

	FloorController(FloorsRepository repository) {
		this.repository = repository;
	}

	// Reads FLOORS straight through. This used to wipe and repopulate a JPA
	// mirror in in-memory H2 on every call, which meant two concurrent requests
	// could each observe the other's half-built table.
	@GetMapping("/floors")
	@NonNull
	CollectionModel<EntityModel<Floor>> all() {
		List<EntityModel<Floor>> floors = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(
						"SELECT ID, NAME, SORTORDER FROM FLOORS ORDER BY SORTORDER NULLS LAST, NAME");
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				int sortOrder = rs.getInt("SORTORDER");
				floors.add(EntityModel.of(new Floor(
						rs.getString("ID"),
						rs.getString("NAME"),
						rs.wasNull() ? null : sortOrder)));
			}
		} catch (SQLException e) {
			logger.error("Failed to load floors", e);
		}

		return CollectionModel.of(floors, linkTo(methodOn(FloorController.class).all()).withSelfRel());
	}

	@PostMapping("/floors")
	ResponseEntity<?> create(@RequestBody Floor body) {
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		try {
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(repository.insert(body.getName().trim(), body.getSortOrder()));
		} catch (SQLException e) {
			logger.error("Failed to create floor", e);
			return ResponseEntity.internalServerError().body("Failed to create floor: " + e.getMessage());
		}
	}

	@PutMapping("/floors/{id}")
	ResponseEntity<?> update(@PathVariable String id, @RequestBody Floor body) {
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		try {
			if (!repository.update(id, body.getName().trim(), body.getSortOrder())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No floor with id=" + id);
			}
			body.setId_(id);
			return ResponseEntity.ok(body);
		} catch (SQLException e) {
			logger.error("Failed to update floor id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to update floor: " + e.getMessage());
		}
	}

	@DeleteMapping("/floors/{id}")
	ResponseEntity<?> delete(@PathVariable String id) {
		try {
			if (!repository.exists(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No floor with id=" + id);
			}
			// PLACES.FLOOR references this row, so name the blocker instead of letting
			// PLACES_FK_1 surface as a 500.
			int places = repository.countPlaces(id);
			if (places > 0) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("Floor still has " + places + " tables");
			}
			repository.deleteById(id);
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete floor id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to delete floor: " + e.getMessage());
		}
	}
}

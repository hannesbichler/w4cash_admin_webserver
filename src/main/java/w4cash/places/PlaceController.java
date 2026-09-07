package w4cash.places;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import w4cash.LoadDatabase;

@RestController
class PlaceController {
	private static final Logger logger = LoggerFactory.getLogger(PlaceController.class);

	private static final String SELECT_COLUMNS =
			"SELECT ID, NAME, X, Y, FLOOR, WIDTH, HEIGHT, FONTSIZE, FONTCOLOR FROM PLACES";

	private final PlacesRepository repository;

	PlaceController(PlacesRepository repository) {
		this.repository = repository;
	}

	// Both endpoints read PLACES straight through. They previously wiped and
	// repopulated one shared JPA mirror in in-memory H2, so a concurrent
	// /places (all floors) could make /places/{floorId} return places from
	// other floors.
	@GetMapping("/places")
	CollectionModel<EntityModel<Place>> all() {
		logger.info("GET /places request was called");
		List<EntityModel<Place>> places = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(SELECT_COLUMNS + " ORDER BY NAME");
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				places.add(EntityModel.of(map(rs)));
			}
		} catch (SQLException e) {
			logger.error("Failed to load places", e);
		}
		return CollectionModel.of(places, linkTo(methodOn(PlaceController.class).all()).withSelfRel());
	}

	@GetMapping("/places/{floorId}")
	CollectionModel<EntityModel<Place>> all(@PathVariable String floorId) {
		logger.info("GET /places/{} request was called", floorId);
		List<EntityModel<Place>> places = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement(SELECT_COLUMNS + " WHERE FLOOR = ? ORDER BY NAME")) {
			st.setString(1, floorId);
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					places.add(EntityModel.of(map(rs)));
				}
			}
		} catch (SQLException e) {
			logger.error("Failed to load places for floorId={}", floorId, e);
		}

		return CollectionModel.of(places, linkTo(methodOn(PlaceController.class).all(floorId)).withSelfRel());
	}

	@PostMapping("/places")
	ResponseEntity<?> create(@RequestBody Place body) {
		try {
			ResponseEntity<?> invalid = validate(body, null);
			if (invalid != null) {
				return invalid;
			}
			body.setName(body.getName().trim());
			return ResponseEntity.status(HttpStatus.CREATED).body(repository.insert(body));
		} catch (SQLException e) {
			logger.error("Failed to create place", e);
			return ResponseEntity.internalServerError().body("Failed to create table: " + e.getMessage());
		}
	}

	@PutMapping("/places/{id}")
	ResponseEntity<?> update(@PathVariable String id, @RequestBody Place body) {
		try {
			if (!repository.exists(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No table with id=" + id);
			}
			ResponseEntity<?> invalid = validate(body, id);
			if (invalid != null) {
				return invalid;
			}
			body.setName(body.getName().trim());
			if (!repository.update(id, body)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No table with id=" + id);
			}
			body.setId_(id);
			return ResponseEntity.ok(body);
		} catch (SQLException e) {
			logger.error("Failed to update place id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to update table: " + e.getMessage());
		}
	}

	@DeleteMapping("/places/{id}")
	ResponseEntity<?> delete(@PathVariable String id) {
		try {
			if (!repository.exists(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No table with id=" + id);
			}
			if (repository.hasOpenTicket(id)) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("Table still has an open ticket");
			}
			repository.deleteById(id);
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete place id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to delete table: " + e.getMessage());
		}
	}

	private ResponseEntity<?> validate(Place body, String id) throws SQLException {
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		if (body.getFloorId() == null || body.getFloorId().isBlank()) {
			return ResponseEntity.badRequest().body("floorId is required");
		}
		if (!repository.floorExists(body.getFloorId())) {
			return ResponseEntity.badRequest().body("No floor with id=" + body.getFloorId());
		}
		if (repository.nameTaken(body.getName().trim(), id)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Another table is already named \"" + body.getName().trim() + "\"");
		}
		return null;
	}

	private Place map(ResultSet rs) throws SQLException {
		Place place = new Place(rs.getString("ID"), rs.getString("NAME"));
		place.setFloorId(rs.getString("FLOOR"));
		place.setX(rs.getInt("X"));
		place.setY(rs.getInt("Y"));
		place.setWidth(nullableInt(rs, "WIDTH"));
		place.setHeight(nullableInt(rs, "HEIGHT"));
		place.setFontSize(nullableInt(rs, "FONTSIZE"));
		place.setFontColor(rs.getString("FONTCOLOR"));
		return place;
	}

	private Integer nullableInt(ResultSet rs, String column) throws SQLException {
		int value = rs.getInt(column);
		return rs.wasNull() ? null : value;
	}
}

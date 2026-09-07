package w4cash.role;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import w4cash.LoadDatabase;

@RestController
class RoleController {

	private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

	private final RolesRepository repository;

	RoleController(RolesRepository repository) {
		this.repository = repository;
	}

	// Reads ROLES straight through, in the order the admin UI lists them.
	// PERMISSIONS is not selected: it is a BLOB the admin UI has no use for, and
	// leaving it out keeps the response small and the column untouched.
	@GetMapping("/roles")
	CollectionModel<EntityModel<Role>> all() {
		List<EntityModel<Role>> roles = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn.prepareStatement("SELECT ID, NAME FROM ROLES ORDER BY NAME");
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				roles.add(EntityModel.of(new Role(
						rs.getString("ID"),
						rs.getString("NAME"))));
			}
		} catch (SQLException e) {
			logger.error("Failed to load roles", e);
		}

		return CollectionModel.of(roles, linkTo(methodOn(RoleController.class).all()).withSelfRel());
	}

	@PostMapping("/roles")
	ResponseEntity<?> create(@RequestBody Role body) {
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		String name = body.getName().trim();
		try {
			if (repository.nameTaken(name, "")) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("A role named \"" + name + "\" already exists");
			}
			return ResponseEntity.status(HttpStatus.CREATED).body(repository.insert(name));
		} catch (SQLException e) {
			logger.error("Failed to create role", e);
			return ResponseEntity.internalServerError().body("Failed to create role: " + e.getMessage());
		}
	}

	@PutMapping("/roles/{id}")
	ResponseEntity<?> update(@PathVariable String id, @RequestBody Role body) {
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		String name = body.getName().trim();
		try {
			if (repository.nameTaken(name, id)) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("A role named \"" + name + "\" already exists");
			}
			if (!repository.update(id, name)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No role with id=" + id);
			}
			body.setId_(id);
			return ResponseEntity.ok(body);
		} catch (SQLException e) {
			logger.error("Failed to update role id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to update role: " + e.getMessage());
		}
	}

	@DeleteMapping("/roles/{id}")
	ResponseEntity<?> delete(@PathVariable String id) {
		try {
			if (!repository.exists(id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No role with id=" + id);
			}
			// PEOPLE.ROLE references this row, so name the blocker instead of letting
			// the foreign key surface as a 500 - and instead of leaving people pointing
			// at a role that no longer exists, which would lock them out of the POS.
			int people = repository.countPeople(id);
			if (people > 0) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("Role is still held by " + people + " people");
			}
			repository.deleteById(id);
			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete role id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to delete role: " + e.getMessage());
		}
	}
}

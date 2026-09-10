package w4cash.person;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import w4cash.LoadDatabase;

@RestController
@RequestMapping({ "", "/api" })
class PersonController {
	private static final Logger logger = LoggerFactory.getLogger(PersonController.class);

	// Reads PEOPLE straight through. This used to wipe and repopulate a JPA
	// mirror in in-memory H2 on every call, which meant two concurrent requests
	// could each observe the other's half-built table.
	@GetMapping("/persons")
	CollectionModel<EntityModel<Person>> all() {
		logger.info("GET /persons request was called");
		List<EntityModel<Person>> persons = new ArrayList<>();
		try (Connection conn = LoadDatabase.getConnection();
				PreparedStatement st = conn
						.prepareStatement("SELECT ID, NAME, APPPASSWORD, CARD, ROLE, IMAGE FROM PEOPLE");
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				persons.add(EntityModel.of(new Person(
						rs.getString("ID"),
						rs.getString("NAME"),
						rs.getString("APPPASSWORD"),
						rs.getString("CARD"),
						rs.getString("ROLE"),
						"")));
			}
		} catch (SQLException e) {
			logger.error("Failed to load persons", e);
		}

		return Objects.requireNonNull(CollectionModel.of(Objects.requireNonNull(persons),
				linkTo(methodOn(PersonController.class).all()).withSelfRel()));
	}

	@PostMapping("/persons")
	ResponseEntity<?> create(@RequestBody Person body) {
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		if (body.getRole() == null || body.getRole().isBlank()) {
			return ResponseEntity.badRequest().body("role is required");
		}

		String name = body.getName().trim();
		String role = body.getRole().trim();
		String card = body.getCard();
		if (card != null) {
			card = card.trim();
			if (card.isEmpty()) {
				card = null;
			}
		}

		try (Connection conn = LoadDatabase.getConnection()) {
			if (nameTaken(conn, name, "")) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body("A person named \"" + name + "\" already exists");
			}

			String id = UUID.randomUUID().toString();
			try (PreparedStatement st = conn.prepareStatement(
					"INSERT INTO PEOPLE (ID, NAME, APPPASSWORD, CARD, ROLE, VISIBLE) VALUES (?, ?, ?, ?, ?, ?)")) {
				st.setString(1, id);
				st.setString(2, name);
				st.setString(3, "");
				st.setString(4, card);
				st.setString(5, role);
				st.setInt(6, 1);
				st.executeUpdate();
			}

			body.setId_(id);
			body.setName(name);
			body.setRole(role);
			body.setCard(card);
			return ResponseEntity.status(HttpStatus.CREATED).body(body);
		} catch (SQLException e) {
			logger.error("Failed to create person", e);
			return ResponseEntity.internalServerError().body("Failed to create person: " + e.getMessage());
		}
	}

	@PutMapping("/persons/{id}")
	ResponseEntity<?> update(@PathVariable String id, @RequestBody Person body) {
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return ResponseEntity.badRequest().body("name is required");
		}
		if (body.getRole() == null || body.getRole().isBlank()) {
			return ResponseEntity.badRequest().body("role is required");
		}

		String name = body.getName().trim();
		String role = body.getRole().trim();
		String card = body.getCard();
		if (card != null) {
			card = card.trim();
			if (card.isEmpty()) {
				card = null;
			}
		}

		try (Connection conn = LoadDatabase.getConnection()) {
			if (!exists(conn, id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No person with id=" + id);
			}
			if (nameTaken(conn, name, id)) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body("A person named \"" + name + "\" already exists");
			}

			try (PreparedStatement st = conn.prepareStatement(
					"UPDATE PEOPLE SET NAME = ?, CARD = ?, ROLE = ? WHERE ID = ?")) {
				st.setString(1, name);
				st.setString(2, card);
				st.setString(3, role);
				st.setString(4, id);
				st.executeUpdate();
			}

			body.setId_(id);
			body.setName(name);
			body.setRole(role);
			body.setCard(card);
			return ResponseEntity.ok(body);
		} catch (SQLException e) {
			logger.error("Failed to update person id={}", id, e);
			return ResponseEntity.internalServerError().body("Failed to update person: " + e.getMessage());
		}
	}

	@DeleteMapping("/persons/{id}")
	ResponseEntity<?> delete(@PathVariable String id) {
		try (Connection conn = LoadDatabase.getConnection()) {
			if (!exists(conn, id)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No person with id=" + id);
			}

			try (PreparedStatement st = conn.prepareStatement("DELETE FROM PEOPLE WHERE ID = ?")) {
				st.setString(1, id);
				int deleted = st.executeUpdate();
				if (deleted <= 0) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No person with id=" + id);
				}
			}

			return ResponseEntity.noContent().build();
		} catch (SQLException e) {
			logger.error("Failed to delete person id={}", id, e);
			String message = e.getMessage();
			if (message != null && message.toLowerCase().contains("constraint")) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body("Person is still referenced and cannot be deleted");
			}
			return ResponseEntity.internalServerError().body("Failed to delete person: " + e.getMessage());
		}
	}

	private boolean exists(Connection conn, String id) throws SQLException {
		try (PreparedStatement st = conn.prepareStatement("SELECT 1 FROM PEOPLE WHERE ID = ?")) {
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}

	private boolean nameTaken(Connection conn, String name, String exceptId) throws SQLException {
		try (PreparedStatement st = conn
				.prepareStatement("SELECT 1 FROM PEOPLE WHERE LOWER(NAME) = LOWER(?) AND ID <> ?")) {
			st.setString(1, name);
			st.setString(2, exceptId);
			try (ResultSet rs = st.executeQuery()) {
				return rs.next();
			}
		}
	}
}

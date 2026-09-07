package w4cash.person;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import w4cash.LoadDatabase;

@RestController
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

		return CollectionModel.of(persons, linkTo(methodOn(PersonController.class).all()).withSelfRel());
	}
}

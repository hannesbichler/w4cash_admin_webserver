package w4cash.person;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import w4cash.LoadDatabase;

@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    MockMvc mockMvc;

    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws Exception {
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        DataSource mockDataSource = mock(DataSource.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        LoadDatabase.setDataSource(mockDataSource);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
    }

    @AfterEach
    void tearDown() {
        LoadDatabase.setDataSource(null);
    }

    @Test
    void getAll_returnsEmptyCollection() throws Exception {
        when(mockResultSet.next()).thenReturn(false);

        mockMvc.perform(get("/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    void getAll_returnsPersonsStraightFromOracle() throws Exception {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("ID")).thenReturn("p1");
        when(mockResultSet.getString("NAME")).thenReturn("Alice");
        when(mockResultSet.getString("APPPASSWORD")).thenReturn("secret");
        when(mockResultSet.getString("CARD")).thenReturn("1234");
        when(mockResultSet.getString("ROLE")).thenReturn("admin");

        mockMvc.perform(get("/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.*[0].id_").value("p1"))
                .andExpect(jsonPath("$._embedded.*[0].name").value("Alice"))
                .andExpect(jsonPath("$._embedded.*[0].role").value("admin"));
    }

    @Test
    void getAll_returnsEveryRow() throws Exception {
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("ID")).thenReturn("p1", "p2");
        when(mockResultSet.getString("NAME")).thenReturn("Alice", "Bob");
        when(mockResultSet.getString("APPPASSWORD")).thenReturn("a", "b");
        when(mockResultSet.getString("CARD")).thenReturn("1", "2");
        when(mockResultSet.getString("ROLE")).thenReturn("admin", "cashier");

        mockMvc.perform(get("/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.*[0].name").value("Alice"))
                .andExpect(jsonPath("$._embedded.*[1].name").value("Bob"));
    }

    @Test
    void getAll_closesConnection() throws Exception {
        when(mockResultSet.next()).thenReturn(false);

        mockMvc.perform(get("/persons")).andExpect(status().isOk());

        verify(mockConnection).close();
    }

    @Test
    void getAll_survivesSqlError() throws Exception {
        when(mockStatement.executeQuery()).thenThrow(new SQLException("db down"));

        mockMvc.perform(get("/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    void put_updatesPerson() throws Exception {
        when(mockResultSet.next()).thenReturn(true, false);

        mockMvc.perform(put("/persons/p1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"name\":\"  Alice  \",\"role\":\"admin\",\"card\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_").value("p1"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.card").value("1234"));
    }

    @Test
    void put_rejectsBlankName() throws Exception {
        mockMvc.perform(put("/persons/p1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"name\":\"\",\"role\":\"admin\",\"card\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("name is required"));
    }

    @Test
    void put_returnsNotFoundWhenMissing() throws Exception {
        when(mockResultSet.next()).thenReturn(false);

        mockMvc.perform(put("/persons/p999")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"name\":\"Alice\",\"role\":\"admin\",\"card\":null}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No person with id=p999"));
    }

    @Test
    void put_rejectsDuplicateName() throws Exception {
        when(mockResultSet.next()).thenReturn(true, true);

        mockMvc.perform(put("/persons/p1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"name\":\"Alice\",\"role\":\"admin\",\"card\":null}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("A person named \"Alice\" already exists"));
    }

    @Test
    void delete_removesPerson() throws Exception {
        when(mockResultSet.next()).thenReturn(true);
        when(mockStatement.executeUpdate()).thenReturn(1);

        mockMvc.perform(delete("/persons/p1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNotFoundWhenMissing() throws Exception {
        when(mockResultSet.next()).thenReturn(false);

        mockMvc.perform(delete("/persons/p999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No person with id=p999"));
    }
}

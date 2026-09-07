package w4cash.role;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RolesRepository repository;

    @Test
    void create_returnsCreatedRole() throws Exception {
        when(repository.nameTaken("Kellner", "")).thenReturn(false);
        when(repository.insert("Kellner")).thenReturn(new Role("generated-id", "Kellner"));

        mockMvc.perform(post("/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Kellner\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_").value("generated-id"))
                .andExpect(jsonPath("$.name").value("Kellner"));
    }

    @Test
    void create_trimsName() throws Exception {
        when(repository.insert("Kellner")).thenReturn(new Role("generated-id", "Kellner"));

        mockMvc.perform(post("/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  Kellner  \"}"))
                .andExpect(status().isCreated());

        verify(repository).insert("Kellner");
    }

    @Test
    void create_rejectsBlankName() throws Exception {
        mockMvc.perform(post("/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(repository, never()).insert(anyString());
    }

    @Test
    void create_returns409OnDuplicateName() throws Exception {
        when(repository.nameTaken("Kellner", "")).thenReturn(true);

        mockMvc.perform(post("/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Kellner\"}"))
                .andExpect(status().isConflict());

        verify(repository, never()).insert(anyString());
    }

    @Test
    void update_renamesExistingRole() throws Exception {
        when(repository.nameTaken("Service", "2")).thenReturn(false);
        when(repository.update("2", "Service")).thenReturn(true);

        mockMvc.perform(put("/roles/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Service\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_").value("2"))
                .andExpect(jsonPath("$.name").value("Service"));
    }

    @Test
    void update_returns404WhenMissing() throws Exception {
        when(repository.update("missing", "Service")).thenReturn(false);

        mockMvc.perform(put("/roles/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Service\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_removesUnusedRole() throws Exception {
        when(repository.exists("2")).thenReturn(true);
        when(repository.countPeople("2")).thenReturn(0);

        mockMvc.perform(delete("/roles/2"))
                .andExpect(status().isNoContent());

        verify(repository).deleteById("2");
    }

    @Test
    void delete_returns409WhilePeopleHoldTheRole() throws Exception {
        when(repository.exists("2")).thenReturn(true);
        when(repository.countPeople("2")).thenReturn(6);

        mockMvc.perform(delete("/roles/2"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Role is still held by 6 people"));

        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void delete_returns404WhenMissing() throws Exception {
        when(repository.exists("missing")).thenReturn(false);

        mockMvc.perform(delete("/roles/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns500OnSqlFailure() throws Exception {
        when(repository.exists("2")).thenReturn(true);
        when(repository.countPeople("2")).thenThrow(new SQLException("ORA-00942"));

        mockMvc.perform(delete("/roles/2"))
                .andExpect(status().isInternalServerError());
    }
}

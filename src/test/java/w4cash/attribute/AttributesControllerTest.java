package w4cash.attribute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttributesController.class)
class AttributesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AttributesRepository repository;

    @Test
    void getAll_returnsAttributes() throws Exception {
        when(repository.findAll()).thenReturn(List.of(new AttributeRef("attr1", "Size")));

        mockMvc.perform(get("/attributes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("attr1"))
                .andExpect(jsonPath("$[0].name").value("Size"));
    }

    @Test
    void getAll_repositoryError_returns500() throws Exception {
        when(repository.findAll()).thenThrow(new SQLException("db down"));

        mockMvc.perform(get("/attributes"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void create_returnsCreatedAttribute() throws Exception {
        when(repository.insert("Color")).thenReturn(new AttributeRef("generated-id", "Color"));

        mockMvc.perform(post("/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Color\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("generated-id"))
                .andExpect(jsonPath("$.name").value("Color"));
    }

    @Test
    void create_blankName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(repository, never()).insert(any());
    }

    @Test
    void update_updatesExistingAttribute() throws Exception {
        when(repository.update("attr1", "Size (EU)")).thenReturn(true);

        mockMvc.perform(put("/attributes/attr1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Size (EU)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Size (EU)"));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        when(repository.update(eq("missing"), any())).thenReturn(false);

        mockMvc.perform(put("/attributes/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ghost\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_deletesExistingAttribute() throws Exception {
        when(repository.deleteById("attr1")).thenReturn(true);

        mockMvc.perform(delete("/attributes/attr1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(repository.deleteById("missing")).thenReturn(false);

        mockMvc.perform(delete("/attributes/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_stillInUse_returnsConflict() throws Exception {
        when(repository.deleteById("attr1")).thenThrow(new SQLException("FK violation"));

        mockMvc.perform(delete("/attributes/attr1"))
                .andExpect(status().isConflict());
    }
}

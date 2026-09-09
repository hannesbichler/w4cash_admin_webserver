package w4cash.attribute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttributeValuesController.class)
class AttributeValuesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AttributeValuesRepository repository;

    @MockBean
    AttributesRepository attributesRepository;

    @Test
    void getAll_returnsValuesInOrder() throws Exception {
        when(attributesRepository.findById("attr1")).thenReturn(Optional.of(new AttributeRef("attr1", "Size")));
        when(repository.findAllForAttribute("attr1")).thenReturn(List.of(
                new AttributeValueRef("v1", "S", 1), new AttributeValueRef("v2", "M", 2)));

        mockMvc.perform(get("/attributes/attr1/values"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("S"))
                .andExpect(jsonPath("$[1].value").value("M"));
    }

    @Test
    void getAll_returns404WhenAttributeMissing() throws Exception {
        when(attributesRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/attributes/missing/values"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_createsValue() throws Exception {
        when(attributesRepository.findById("attr1")).thenReturn(Optional.of(new AttributeRef("attr1", "Size")));
        when(repository.insert("attr1", "L")).thenReturn(new AttributeValueRef("generated-id", "L", 3));

        mockMvc.perform(post("/attributes/attr1/values")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"value\":\"L\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("generated-id"))
                .andExpect(jsonPath("$.value").value("L"));
    }

    @Test
    void create_blankValue_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/attributes/attr1/values")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"value\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(repository, never()).insert(any(), any());
    }

    @Test
    void create_returns404WhenAttributeMissing() throws Exception {
        when(attributesRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(post("/attributes/missing/values")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"value\":\"L\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_updatesExistingValue() throws Exception {
        when(repository.update("attr1", "v1", "XS")).thenReturn(true);

        mockMvc.perform(put("/attributes/attr1/values/v1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"value\":\"XS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("XS"));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        when(repository.update(eq("attr1"), eq("missing"), any())).thenReturn(false);

        mockMvc.perform(put("/attributes/attr1/values/missing")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"value\":\"Ghost\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_deletesExistingValue() throws Exception {
        when(repository.deleteById("attr1", "v1")).thenReturn(true);

        mockMvc.perform(delete("/attributes/attr1/values/v1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(repository.deleteById("attr1", "missing")).thenReturn(false);

        mockMvc.perform(delete("/attributes/attr1/values/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reorder_updatesOrder() throws Exception {
        when(attributesRepository.findById("attr1")).thenReturn(Optional.of(new AttributeRef("attr1", "Size")));
        when(repository.findAllForAttribute("attr1")).thenReturn(List.of(
                new AttributeValueRef("v2", "M", 1), new AttributeValueRef("v1", "S", 2)));

        mockMvc.perform(put("/attributes/attr1/values-order")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("[\"v2\",\"v1\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("M"));

        verify(repository).reorder(eq("attr1"), eq(List.of("v2", "v1")));
    }

    @Test
    void reorder_returns404WhenAttributeMissing() throws Exception {
        when(attributesRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(put("/attributes/missing/values-order")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("[]"))
                .andExpect(status().isNotFound());

        verify(repository, never()).reorder(any(), any());
    }
}

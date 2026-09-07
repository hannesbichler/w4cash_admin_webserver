package w4cash.attribute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttributeSetsController.class)
class AttributeSetsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AttributeSetsRepository repository;

    @MockBean
    AttributesRepository attributesRepository;

    @Test
    void getAll_returnsSets() throws Exception {
        when(repository.findAll()).thenReturn(List.of(new AttributeSetRef("set1", "Shirt Variants")));

        mockMvc.perform(get("/attribute-sets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("set1"))
                .andExpect(jsonPath("$[0].name").value("Shirt Variants"));
    }

    @Test
    void getOne_returnsDetailWithAttributes() throws Exception {
        var detail = new AttributeSetDetail("set1", "Shirt Variants",
                List.of(new AttributeUseRef("attr1", "Size", 1)));
        when(repository.findById("set1")).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/attribute-sets/set1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shirt Variants"))
                .andExpect(jsonPath("$.attributes[0].name").value("Size"));
    }

    @Test
    void getOne_returns404WhenNotFound() throws Exception {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/attribute-sets/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreatedSet() throws Exception {
        when(repository.insert("Shirt Variants"))
                .thenReturn(new AttributeSetDetail("generated-id", "Shirt Variants", List.of()));

        mockMvc.perform(post("/attribute-sets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Shirt Variants\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("generated-id"));
    }

    @Test
    void rename_updatesExistingSet() throws Exception {
        when(repository.rename("set1", "Renamed")).thenReturn(true);
        when(repository.findById("set1")).thenReturn(Optional.of(new AttributeSetDetail("set1", "Renamed", List.of())));

        mockMvc.perform(put("/attribute-sets/set1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void rename_returns404WhenNotFound() throws Exception {
        when(repository.rename(eq("missing"), any())).thenReturn(false);

        mockMvc.perform(put("/attribute-sets/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ghost\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_deletesExistingSet() throws Exception {
        when(repository.deleteById("set1")).thenReturn(true);

        mockMvc.perform(delete("/attribute-sets/set1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(repository.deleteById("missing")).thenReturn(false);

        mockMvc.perform(delete("/attribute-sets/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAttribute_addsAndReturnsUpdatedDetail() throws Exception {
        when(repository.setExists("set1")).thenReturn(true);
        when(attributesRepository.findById("attr1")).thenReturn(Optional.of(new AttributeRef("attr1", "Size")));
        when(repository.existsAttributeUse("set1", "attr1")).thenReturn(false);
        when(repository.findById("set1")).thenReturn(Optional.of(
                new AttributeSetDetail("set1", "Shirt Variants", List.of(new AttributeUseRef("attr1", "Size", 1)))));

        mockMvc.perform(post("/attribute-sets/set1/attributes/attr1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes[0].name").value("Size"));

        verify(repository).addAttributeToSet("set1", "attr1");
    }

    @Test
    void addAttribute_returns404WhenSetMissing() throws Exception {
        when(repository.setExists("missing")).thenReturn(false);

        mockMvc.perform(post("/attribute-sets/missing/attributes/attr1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAttribute_returns404WhenAttributeMissing() throws Exception {
        when(repository.setExists("set1")).thenReturn(true);
        when(attributesRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(post("/attribute-sets/set1/attributes/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAttribute_returns409WhenAlreadyLinked() throws Exception {
        when(repository.setExists("set1")).thenReturn(true);
        when(attributesRepository.findById("attr1")).thenReturn(Optional.of(new AttributeRef("attr1", "Size")));
        when(repository.existsAttributeUse("set1", "attr1")).thenReturn(true);

        mockMvc.perform(post("/attribute-sets/set1/attributes/attr1"))
                .andExpect(status().isConflict());

        verify(repository, never()).addAttributeToSet(any(), any());
    }

    @Test
    void removeAttribute_removesAndReturnsUpdatedDetail() throws Exception {
        when(repository.removeAttributeFromSet("set1", "attr1")).thenReturn(true);
        when(repository.findById("set1")).thenReturn(Optional.of(new AttributeSetDetail("set1", "Shirt Variants", List.of())));

        mockMvc.perform(delete("/attribute-sets/set1/attributes/attr1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes").isEmpty());
    }

    @Test
    void removeAttribute_returns404WhenNotLinked() throws Exception {
        when(repository.removeAttributeFromSet("set1", "attr1")).thenReturn(false);

        mockMvc.perform(delete("/attribute-sets/set1/attributes/attr1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reorder_updatesOrder() throws Exception {
        when(repository.setExists("set1")).thenReturn(true);
        when(repository.findById("set1")).thenReturn(Optional.of(new AttributeSetDetail("set1", "Shirt Variants",
                List.of(new AttributeUseRef("attr2", "Color", 1), new AttributeUseRef("attr1", "Size", 2)))));

        mockMvc.perform(put("/attribute-sets/set1/attribute-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"attr2\",\"attr1\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes[0].name").value("Color"));

        verify(repository).reorder(eq("set1"), eq(List.of("attr2", "attr1")));
    }

    @Test
    void reorder_returns404WhenSetMissing() throws Exception {
        when(repository.setExists("missing")).thenReturn(false);

        mockMvc.perform(put("/attribute-sets/missing/attribute-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isNotFound());

        verify(repository, never()).reorder(any(), anyList());
    }
}

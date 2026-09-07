package w4cash.taxcategory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
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

@WebMvcTest(TaxCategoriesController.class)
class TaxCategoriesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TaxCategoriesRepository repository;

    @MockBean
    TaxesRepository taxesRepository;

    @Test
    void getAll_returnsCategories() throws Exception {
        when(repository.findAll()).thenReturn(List.of(new TaxCategoryRef("cat1", "Standard")));

        mockMvc.perform(get("/tax-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cat1"))
                .andExpect(jsonPath("$[0].name").value("Standard"));
    }

    @Test
    void getAll_repositoryError_returns500() throws Exception {
        when(repository.findAll()).thenThrow(new SQLException("db down"));

        mockMvc.perform(get("/tax-categories"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getOne_returnsDetailWithRates() throws Exception {
        when(repository.findById("cat1")).thenReturn(Optional.of(new TaxCategoryRef("cat1", "Standard")));
        when(taxesRepository.findAllForCategory("cat1"))
                .thenReturn(List.of(new TaxRateRef("tax1", "Standard 20%", 20.0, "2024-01-01")));

        mockMvc.perform(get("/tax-categories/cat1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Standard"))
                .andExpect(jsonPath("$.rates[0].rate").value(20.0))
                .andExpect(jsonPath("$.rates[0].validFrom").value("2024-01-01"));
    }

    @Test
    void getOne_returns404WhenNotFound() throws Exception {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/tax-categories/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreatedCategory() throws Exception {
        when(repository.insert("Reduced")).thenReturn(new TaxCategoryRef("generated-id", "Reduced"));

        mockMvc.perform(post("/tax-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Reduced\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("generated-id"));
    }

    @Test
    void rename_updatesExisting() throws Exception {
        when(repository.rename("cat1", "Renamed")).thenReturn(true);

        mockMvc.perform(put("/tax-categories/cat1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void rename_returns404WhenNotFound() throws Exception {
        when(repository.rename(eq("missing"), any())).thenReturn(false);

        mockMvc.perform(put("/tax-categories/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ghost\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_deletesExisting() throws Exception {
        when(repository.deleteById("cat1")).thenReturn(true);

        mockMvc.perform(delete("/tax-categories/cat1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(repository.deleteById("missing")).thenReturn(false);

        mockMvc.perform(delete("/tax-categories/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRate_addsRate() throws Exception {
        when(repository.exists("cat1")).thenReturn(true);
        when(taxesRepository.insert("cat1", "Standard 20%", 20.0, "2024-01-01"))
                .thenReturn(new TaxRateRef("generated-id", "Standard 20%", 20.0, "2024-01-01"));

        mockMvc.perform(post("/tax-categories/cat1/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Standard 20%\",\"rate\":20.0,\"validFrom\":\"2024-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("generated-id"));
    }

    @Test
    void createRate_returns404WhenCategoryMissing() throws Exception {
        when(repository.exists("missing")).thenReturn(false);

        mockMvc.perform(post("/tax-categories/missing/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"rate\":1.0,\"validFrom\":\"2024-01-01\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRate_missingValidFrom_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/tax-categories/cat1/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"rate\":1.0}"))
                .andExpect(status().isBadRequest());

        verify(taxesRepository, never()).insert(any(), any(), anyDouble(), any());
    }

    @Test
    void updateRate_updatesExisting() throws Exception {
        when(taxesRepository.update("cat1", "tax1", "Renamed", 21.0, "2024-06-01")).thenReturn(true);

        mockMvc.perform(put("/tax-categories/cat1/rates/tax1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\",\"rate\":21.0,\"validFrom\":\"2024-06-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(21.0));
    }

    @Test
    void updateRate_returns404WhenNotFound() throws Exception {
        when(taxesRepository.update(eq("cat1"), eq("missing"), any(), anyDouble(), any())).thenReturn(false);

        mockMvc.perform(put("/tax-categories/cat1/rates/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ghost\",\"rate\":1.0,\"validFrom\":\"2024-01-01\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRate_deletesExisting() throws Exception {
        when(taxesRepository.deleteById("cat1", "tax1")).thenReturn(true);

        mockMvc.perform(delete("/tax-categories/cat1/rates/tax1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRate_returns404WhenNotFound() throws Exception {
        when(taxesRepository.deleteById("cat1", "missing")).thenReturn(false);

        mockMvc.perform(delete("/tax-categories/cat1/rates/missing"))
                .andExpect(status().isNotFound());
    }
}

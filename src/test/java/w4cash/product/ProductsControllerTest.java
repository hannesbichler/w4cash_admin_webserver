package w4cash.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(ProductsController.class)
class ProductsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ProductsRepository repository;

    @Test
    void getAll_returnsEmptyCollection() throws Exception {
        when(repository.findAll(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    void getAll_acceptsApiPrefix() throws Exception {
        when(repository.findAll(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    void getAll_returnsProducts() throws Exception {
        Product product = new Product("prod1", "REF1", "BURGER", "Burger", 5.00, 9.99, "tax1", "cat1", "unit", "attrset1");
        when(repository.findAll(isNull())).thenReturn(List.of(product));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.*[0].id_").value("prod1"))
                .andExpect(jsonPath("$._embedded.*[0].name").value("Burger"))
                .andExpect(jsonPath("$._embedded.*[0].pricesell").value(9.99))
                .andExpect(jsonPath("$._embedded.*[0].attributeSetId").value("attrset1"));
    }

    @Test
    void getAll_passesCategoryIdFilterToRepository() throws Exception {
        when(repository.findAll(eq("drinks"))).thenReturn(List.of());

        mockMvc.perform(get("/products").param("categoryId", "drinks"))
                .andExpect(status().isOk());

        verify(repository).findAll("drinks");
    }

    @Test
    void getAll_repositoryError_returns500() throws Exception {
        when(repository.findAll(any())).thenThrow(new SQLException("db down"));

        mockMvc.perform(get("/products"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getById_returnsProduct() throws Exception {
        Product product = new Product("prod1", "REF1", "BURGER", "Burger", 5.00, 9.99, "tax1", "cat1", "unit", "attrset1");
        when(repository.findById("prod1")).thenReturn(Optional.of(product));

        mockMvc.perform(get("/products/prod1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Burger"))
                .andExpect(jsonPath("$.pricesell").value(9.99));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_savesAndReturnsProductWithGeneratedId() throws Exception {
        when(repository.insert(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId("generated-id");
            return p;
        });

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"Fries\",\"code\":\"FRIES\",\"pricesell\":3.50,\"categoryId\":\"food\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_").value("generated-id"))
                .andExpect(jsonPath("$.name").value("Fries"));
    }

    @Test
    void update_updatesExistingProduct() throws Exception {
        when(repository.update(eq("prod1"), any(Product.class))).thenReturn(true);

        mockMvc.perform(put("/products/prod1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"Premium Burger\",\"code\":\"BURGER\",\"pricesell\":12.99,\"categoryId\":\"food\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_").value("prod1"))
                .andExpect(jsonPath("$.name").value("Premium Burger"));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        when(repository.update(eq("missing"), any(Product.class))).thenReturn(false);

        mockMvc.perform(put("/products/missing")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"Ghost\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_deletesExistingProduct() throws Exception {
        when(repository.deleteById("prod1")).thenReturn(true);

        mockMvc.perform(delete("/products/prod1"))
                .andExpect(status().isNoContent());

        verify(repository).deleteById("prod1");
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(repository.deleteById("missing")).thenReturn(false);

        mockMvc.perform(delete("/products/missing"))
                .andExpect(status().isNotFound());
    }
}

package w4cash.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import net.sf.jasperreports.engine.JRException;

@WebMvcTest(ReportsController.class)
class ReportsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ReportsRepository repository;

    @Test
    void getAll_returnsReportRefs() throws Exception {
        when(repository.listNames()).thenReturn(List.of("products-list"));
        when(repository.parameters("products-list"))
                .thenReturn(List.of(new ReportParamInfo("minPrice", "Double")));

        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("products-list"))
                .andExpect(jsonPath("$[0].parameters[0].name").value("minPrice"));
    }

    @Test
    void getAll_repositoryError_returns500() throws Exception {
        when(repository.listNames()).thenReturn(List.of("broken"));
        when(repository.parameters("broken")).thenThrow(new JRException("bad template"));

        mockMvc.perform(get("/reports"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void upload_returnsCreatedWithParams() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "products-list.jrxml",
                MediaType.APPLICATION_XML_VALUE, "<jasperReport/>".getBytes());
        when(repository.parameters("products-list")).thenReturn(List.of());

        mockMvc.perform(multipart("/reports").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("products-list"));

        verify(repository).upload(eq("products-list"), any(byte[].class));
    }

    @Test
    void upload_invalidJrxml_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "broken.jrxml",
                MediaType.APPLICATION_XML_VALUE, "not xml".getBytes());
        doThrow(new JRException("parse error")).when(repository).upload(eq("broken"), any(byte[].class));

        mockMvc.perform(multipart("/reports").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_saveFails_returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "products-list.jrxml",
                MediaType.APPLICATION_XML_VALUE, "<jasperReport/>".getBytes());
        doThrow(new IOException("disk full")).when(repository).upload(eq("products-list"), any(byte[].class));

        mockMvc.perform(multipart("/reports").file(file))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void delete_deletesExisting() throws Exception {
        when(repository.delete("products-list")).thenReturn(true);

        mockMvc.perform(delete("/reports/products-list"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        when(repository.delete("missing")).thenReturn(false);

        mockMvc.perform(delete("/reports/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void run_returnsPdfBytes() throws Exception {
        byte[] pdfBytes = new byte[] { 1, 2, 3 };
        when(repository.exists("products-list")).thenReturn(true);
        when(repository.run(eq("products-list"), any())).thenReturn(pdfBytes);

        mockMvc.perform(post("/reports/products-list/run")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"minPrice\":\"5.0\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"products-list.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void run_returns404WhenNotFound() throws Exception {
        when(repository.exists("missing")).thenReturn(false);

        mockMvc.perform(post("/reports/missing/run")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void run_fillFails_returns500() throws Exception {
        when(repository.exists("products-list")).thenReturn(true);
        when(repository.run(eq("products-list"), any())).thenThrow(new JRException("fill failed"));

        mockMvc.perform(post("/reports/products-list/run")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(status().isInternalServerError());
    }
}

package w4cash.printer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Note: deliberately does NOT exercise the successful PUT /printers/config/{slot}
// path. A real success writes through W4cashApplication.APP_CONFIG.save(), which
// persists to the real on-disk w4cash.properties config file used by whichever
// machine runs the tests - not something a test suite should ever do as a side
// effect. Only the validation branches (which return before touching APP_CONFIG's
// file) and the read-only GETs are covered here.
@WebMvcTest(PrinterController.class)
class PrinterControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void installed_returnsOkWithArray() throws Exception {
        mockMvc.perform(get("/printers/installed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void config_returnsAllFourSlotsInOrder() throws Exception {
        mockMvc.perform(get("/printers/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slot").value("1"))
                .andExpect(jsonPath("$[1].slot").value("2"))
                .andExpect(jsonPath("$[2].slot").value("3"))
                .andExpect(jsonPath("$[3].slot").value("customer"));
    }

    @Test
    void configure_returns400_whenSlotUnknown() throws Exception {
        mockMvc.perform(put("/printers/config/4")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"printerName\":\"Not defined\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configure_returns400_whenPrinterNameBlank() throws Exception {
        mockMvc.perform(put("/printers/config/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"printerName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configure_returns400_whenPrinterNotInstalled() throws Exception {
        mockMvc.perform(put("/printers/config/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"printerName\":\"totally-bogus-printer-xyz-123\"}"))
                .andExpect(status().isBadRequest());
    }
}

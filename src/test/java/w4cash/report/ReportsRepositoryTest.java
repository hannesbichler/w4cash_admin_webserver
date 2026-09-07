package w4cash.report;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.sf.jasperreports.engine.JRException;

// Exercises the real JasperReports compile/introspect pipeline (no DB access needed for
// this - only run()/fillReport requires a live connection, which is verified manually
// against the actual Oracle DB instead, same as the rest of this webserver's JDBC repositories).
class ReportsRepositoryTest {

    @TempDir
    File tempDir;

    private ReportsRepository repository;
    private byte[] validJrxml;

    @BeforeEach
    void setUp() throws IOException {
        repository = new ReportsRepository(tempDir);
        try (InputStream in = getClass().getResourceAsStream("/products-list.jrxml")) {
            validJrxml = in.readAllBytes();
        }
    }

    @Test
    void upload_compilesAndStoresTemplate() throws Exception {
        repository.upload("products-list", validJrxml);

        assertTrue(repository.exists("products-list"));
        assertEquals(List.of("products-list"), repository.listNames());
        assertTrue(new File(tempDir, "products-list.jrxml").exists());
        assertTrue(new File(tempDir, "products-list.jasper").exists());
    }

    @Test
    void upload_rejectsInvalidXml() {
        byte[] garbage = "not a jrxml file".getBytes();
        assertThrows(JRException.class, () -> repository.upload("broken", garbage));
        assertFalse(repository.exists("broken"));
    }

    @Test
    void parameters_returnsDeclaredNonSystemParameters() throws Exception {
        repository.upload("products-list", validJrxml);

        List<ReportParamInfo> params = repository.parameters("products-list");

        ReportParamInfo minPrice = params.stream().filter(p -> p.name().equals("minPrice")).findFirst()
                .orElseThrow(() -> new AssertionError("minPrice parameter not found in: " + params));
        assertEquals("Double", minPrice.type());
    }

    @Test
    void delete_removesBothFiles() throws Exception {
        repository.upload("products-list", validJrxml);

        boolean deleted = repository.delete("products-list");

        assertTrue(deleted);
        assertFalse(repository.exists("products-list"));
        assertFalse(new File(tempDir, "products-list.jrxml").exists());
        assertFalse(new File(tempDir, "products-list.jasper").exists());
    }

    @Test
    void delete_returnsFalseWhenNotFound() {
        assertFalse(repository.delete("missing"));
    }

    @Test
    void listNames_isSortedAndOnlyIncludesUploaded() throws Exception {
        repository.upload("zzz-report", validJrxml);
        repository.upload("aaa-report", validJrxml);

        assertEquals(List.of("aaa-report", "zzz-report"), repository.listNames());
    }

    @Test
    void load_usesCachedCompiledReportWhenUpToDate() throws Exception {
        repository.upload("products-list", validJrxml);
        File jasperFile = new File(tempDir, "products-list.jasper");
        long compiledAt = jasperFile.lastModified();

        // loading again should not need to touch the source file's timestamp / recompile
        repository.load("products-list");

        assertEquals(compiledAt, jasperFile.lastModified());
    }
}

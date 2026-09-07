package w4cash.product;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import w4cash.LoadDatabase;
import javax.sql.DataSource;

class ProductsRepositoryTest {

    private Connection mockConnection;
    private PreparedStatement mockLockStmt;
    private PreparedStatement mockNextNumberStmt;
    private ResultSet mockNextNumberRs;
    private PreparedStatement mockInsertStmt;
    private PreparedStatement mockCatalogInsertStmt;
    private final ProductsRepository repository = new ProductsRepository();

    @BeforeEach
    void setUp() throws Exception {
        mockConnection = mock(Connection.class);
        mockLockStmt = mock(PreparedStatement.class);
        mockNextNumberStmt = mock(PreparedStatement.class);
        mockNextNumberRs = mock(ResultSet.class);
        mockInsertStmt = mock(PreparedStatement.class);
        mockCatalogInsertStmt = mock(PreparedStatement.class);

        when(mockConnection.prepareStatement(argThat(s -> s != null && s.startsWith("LOCK TABLE"))))
                .thenReturn(mockLockStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.startsWith("SELECT NVL(MAX"))))
                .thenReturn(mockNextNumberStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.startsWith("INSERT INTO PRODUCTS ("))))
                .thenReturn(mockInsertStmt);
        when(mockConnection.prepareStatement(argThat(s -> s != null && s.startsWith("INSERT INTO PRODUCTS_CAT"))))
                .thenReturn(mockCatalogInsertStmt);
        when(mockNextNumberStmt.executeQuery()).thenReturn(mockNextNumberRs);

        DataSource mockDataSource = mock(DataSource.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        LoadDatabase.setDataSource(mockDataSource);
    }

    @AfterEach
    void tearDown() {
        LoadDatabase.setDataSource(null);
    }

    @Test
    void insert_autoFillsReferenceAndCode_whenBothBlank() throws Exception {
        when(mockNextNumberRs.next()).thenReturn(true);
        when(mockNextNumberRs.getLong(1)).thenReturn(42L);

        Product p = new Product();
        p.setName("Fries");
        p.setCategoryId("food");

        Product saved = repository.insert(p);

        assertEqualsNextNumber(saved);
        verify(mockLockStmt).execute();
        verify(mockInsertStmt).setString(2, "42");
        verify(mockInsertStmt).setString(3, "42");
        verify(mockConnection).commit();
        // autoCommit is reset by the pool; what matters here is that the
        // connection is handed back rather than leaked.
        verify(mockConnection).close();
    }

    private void assertEqualsNextNumber(Product saved) {
        org.junit.jupiter.api.Assertions.assertEquals("42", saved.getReference());
        org.junit.jupiter.api.Assertions.assertEquals("42", saved.getCode());
    }

    @Test
    void insert_doesNotAutoFillOrLock_whenBothSupplied() throws Exception {
        Product p = new Product();
        p.setName("Fries");
        p.setReference("REF1");
        p.setCode("CODE1");

        repository.insert(p);

        verify(mockConnection, never()).prepareStatement(argThat(s -> s != null && s.startsWith("LOCK TABLE")));
        verify(mockConnection, never()).setAutoCommit(anyBoolean());
        verify(mockInsertStmt).setString(2, "REF1");
        verify(mockInsertStmt).setString(3, "CODE1");
    }

    @Test
    void insert_fillsOnlyMissingField_whenOnlyOneSupplied() throws Exception {
        when(mockNextNumberRs.next()).thenReturn(true);
        when(mockNextNumberRs.getLong(1)).thenReturn(7L);

        Product p = new Product();
        p.setName("Fries");
        p.setCode("CUSTOMCODE");

        repository.insert(p);

        verify(mockInsertStmt).setString(2, "7");
        verify(mockInsertStmt).setString(3, "CUSTOMCODE");
    }

    @Test
    void insert_addsCatalogRow_whenActive() throws Exception {
        Product p = new Product();
        p.setName("Fries");
        p.setReference("REF1");
        p.setCode("CODE1");
        p.setActive(true);

        Product saved = repository.insert(p);

        verify(mockCatalogInsertStmt).setString(1, saved.getId());
        verify(mockCatalogInsertStmt).executeUpdate();
    }

    @Test
    void insert_leavesCatalogAlone_whenInactive() throws Exception {
        Product p = new Product();
        p.setName("Fries");
        p.setReference("REF1");
        p.setCode("CODE1");

        repository.insert(p);

        verify(mockConnection, never()).prepareStatement(argThat(s -> s != null && s.contains("PRODUCTS_CAT")));
    }

    @Test
    void insert_rollsBackAndClosesConnection_onSqlException() throws Exception {
        when(mockNextNumberRs.next()).thenReturn(true);
        when(mockNextNumberRs.getLong(1)).thenReturn(1L);
        when(mockInsertStmt.executeUpdate()).thenThrow(new java.sql.SQLException("boom"));

        Product p = new Product();
        p.setName("Fries");

        org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, () -> repository.insert(p));

        verify(mockConnection).rollback();
        verify(mockConnection).close();
    }
}

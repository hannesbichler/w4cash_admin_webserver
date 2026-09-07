package w4cash.attribute;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import w4cash.LoadDatabase;
import javax.sql.DataSource;

class AttributeSetsRepositoryTest {

    private Connection mockConnection;
    private PreparedStatement mockUpdateStmt;
    private final AttributeSetsRepository repository = new AttributeSetsRepository();

    @BeforeEach
    void setUp() throws Exception {
        mockConnection = mock(Connection.class);
        mockUpdateStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockUpdateStmt);
        DataSource mockDataSource = mock(DataSource.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        LoadDatabase.setDataSource(mockDataSource);
    }

    @AfterEach
    void tearDown() {
        LoadDatabase.setDataSource(null);
    }

    @Test
    void reorder_stagesNegativeLinenosBeforeFinalOrder() throws Exception {
        // ATTUSE_LINE is a UNIQUE(ATTRIBUTESET_ID, LINENO) index: writing final
        // positions in one pass can collide mid-batch with another row's still-old
        // LINENO (e.g. swapping two attributes). Assert the negative-staging batch
        // runs, then the final-order batch, as two separate executeBatch() calls.
        repository.reorder("set1", List.of("attr2", "attr1"));

        var inOrder = inOrder(mockUpdateStmt);
        inOrder.verify(mockUpdateStmt).setInt(1, -1);
        inOrder.verify(mockUpdateStmt).setString(2, "set1");
        inOrder.verify(mockUpdateStmt).setString(3, "attr2");
        inOrder.verify(mockUpdateStmt).addBatch();
        inOrder.verify(mockUpdateStmt).setInt(1, -2);
        inOrder.verify(mockUpdateStmt).setString(3, "attr1");
        inOrder.verify(mockUpdateStmt).addBatch();
        inOrder.verify(mockUpdateStmt).executeBatch();

        inOrder.verify(mockUpdateStmt).setInt(1, 1);
        inOrder.verify(mockUpdateStmt).setString(3, "attr2");
        inOrder.verify(mockUpdateStmt).addBatch();
        inOrder.verify(mockUpdateStmt).setInt(1, 2);
        inOrder.verify(mockUpdateStmt).setString(3, "attr1");
        inOrder.verify(mockUpdateStmt).addBatch();
        inOrder.verify(mockUpdateStmt).executeBatch();

        verify(mockUpdateStmt, times(2)).executeBatch();
    }
}

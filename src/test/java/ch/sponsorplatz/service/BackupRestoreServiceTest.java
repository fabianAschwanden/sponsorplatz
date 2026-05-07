package ch.sponsorplatz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link BackupRestoreService}.
 *
 * Test-IDs: RESTORE-01..03 in {@code specs/TESTSTRATEGIE.md}.
 */
@ExtendWith(MockitoExtension.class)
class BackupRestoreServiceTest {

    @Mock private DataSource dataSource;
    @Mock private AuditService auditService;

    private BackupRestoreService service;

    @BeforeEach
    void setUp() {
        service = new BackupRestoreService(dataSource, auditService);
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:file:./data/test");
        ReflectionTestUtils.setField(service, "datasourceUser", "sa");
    }

    @Test
    @DisplayName("RESTORE-01: leerer Restore-Input wird abgelehnt")
    void leererInputWirdAbgelehnt() {
        assertThatThrownBy(() -> service.restore(new byte[0], "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leer");
        assertThatThrownBy(() -> service.restore(null, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RESTORE-02: H2-Restore ruft RUNSCRIPT FROM mit Temp-Datei + Audit-Eintrag")
    void h2RestoreFuehrtRunscriptAus() throws Exception {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        byte[] sql = "CREATE TABLE t (id INT);".getBytes();
        service.restore(sql, "admin");

        verify(stmt).execute(contains("RUNSCRIPT FROM"));
        verify(auditService).protokolliere(
                org.mockito.ArgumentMatchers.eq(ch.sponsorplatz.model.AuditAktion.BACKUP_ERSTELLT),
                org.mockito.ArgumentMatchers.eq("admin"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("RESTORE"),
                org.mockito.ArgumentMatchers.contains("Bytes=24"));
    }

    @Test
    @DisplayName("RESTORE-03: SQL-Failure beim H2-RUNSCRIPT wird als RuntimeException propagiert")
    void h2FehlerWirdPropagiert() throws Exception {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.execute(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new java.sql.SQLException("Syntax error"));

        assertThatThrownBy(() -> service.restore("BROKEN".getBytes(), "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("H2-Restore fehlgeschlagen");
    }
}

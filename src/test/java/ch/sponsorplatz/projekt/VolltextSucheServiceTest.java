package ch.sponsorplatz.projekt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link VolltextSucheService}.
 *
 * <p>Postgres-Pfad (tsvector) wird live auf der Staging-VM verifiziert —
 * H2 unterstützt tsvector nicht, deshalb hier nur Dialect-Routing + LIKE-
 * Fallback testen.
 *
 * Test-IDs: VTS-PG-01..04.
 */
@ExtendWith(MockitoExtension.class)
class VolltextSucheServiceTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private ProjektRepository projektRepository;

    private VolltextSucheService service;

    @BeforeEach
    void setUp() {
        service = new VolltextSucheService(jdbc, projektRepository);
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:mem:test");
    }

    @Test
    @DisplayName("VTS-PG-01: leerer Suchbegriff liefert alle öffentlichen Projekte")
    void leererSuchbegriff() {
        when(projektRepository.findBySichtbarkeitOrderByVeroeffentlichtAmDesc(Sichtbarkeit.OEFFENTLICH))
                .thenReturn(List.of(new Projekt(), new Projekt()));

        assertThat(service.suchen(null)).hasSize(2);
        assertThat(service.suchen("")).hasSize(2);
        assertThat(service.suchen("   ")).hasSize(2);

        verify(jdbc, never()).queryForList(any(String.class), eq(java.util.UUID.class), any());
    }

    @Test
    @DisplayName("VTS-PG-02: H2 (kein Postgres) → repository.sucheOeffentliche (LIKE-Fallback)")
    void h2FallbackAufLike() {
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:file:./data/test");
        when(projektRepository.sucheOeffentliche("yoga", Sichtbarkeit.OEFFENTLICH))
                .thenReturn(List.of(new Projekt()));

        var result = service.suchen("yoga");

        assertThat(result).hasSize(1);
        verify(projektRepository).sucheOeffentliche("yoga", Sichtbarkeit.OEFFENTLICH);
        verify(jdbc, never()).queryForList(any(String.class), eq(java.util.UUID.class), any());
    }

    @Test
    @DisplayName("VTS-PG-03: jdbc-URL Postgres → JdbcTemplate.queryForList wird genutzt")
    void postgresPfadNutztJdbcTemplate() {
        ReflectionTestUtils.setField(service, "datasourceUrl",
                "jdbc:postgresql://db:5432/sponsorplatz");
        when(jdbc.queryForList(any(String.class), eq(java.util.UUID.class),
                any(), any(), any(), any())).thenReturn(List.of());

        service.suchen("fussball");

        verify(jdbc).queryForList(any(String.class), eq(java.util.UUID.class),
                eq("OEFFENTLICH"), eq("fussball"), eq("fussball"), eq("fussball"));
    }

    @Test
    @DisplayName("VTS-PG-04: Postgres-Query-Fehler → Fallback auf LIKE-Suche")
    void postgresFehlerFallback() {
        ReflectionTestUtils.setField(service, "datasourceUrl",
                "jdbc:postgresql://db:5432/sponsorplatz");
        when(jdbc.queryForList(any(String.class), eq(java.util.UUID.class),
                any(), any(), any(), any()))
                .thenThrow(new RuntimeException("syntax error at or near \"tsvektor\""));
        when(projektRepository.sucheOeffentliche("kaputt", Sichtbarkeit.OEFFENTLICH))
                .thenReturn(List.of(new Projekt()));

        var result = service.suchen("kaputt");

        assertThat(result).hasSize(1);
        verify(projektRepository).sucheOeffentliche("kaputt", Sichtbarkeit.OEFFENTLICH);
    }
}

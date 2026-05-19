package ch.sponsorplatz.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapping-Test für {@link AuditLogView} — primär das neue {@code umgebung}-
 * Feld (Phase 15.3-Folge: Cross-Cloud-DB-Sync-Schutz).
 *
 * Test-ID: VIEW-AUDIT-01.
 */
class AuditLogViewTest {

    @Test
    void mappingMitUmgebung() {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setZeitpunkt(Instant.parse("2026-05-19T10:00:00Z"));
        log.setAktion("ERSTELLT");
        log.setBereich("ORGANISATION");
        log.setBenutzerEmail("admin@sp.ch");
        log.setZielId(UUID.randomUUID());
        log.setZielTyp("Organisation");
        log.setDetails("x");
        log.setUmgebung("oci-staging-free");

        AuditLogView view = AuditLogView.von(log);

        assertThat(view.id()).isEqualTo(log.getId());
        assertThat(view.aktion()).isEqualTo("ERSTELLT");
        assertThat(view.bereich()).isEqualTo("ORGANISATION");
        assertThat(view.benutzerEmail()).isEqualTo("admin@sp.ch");
        assertThat(view.zielTyp()).isEqualTo("Organisation");
        assertThat(view.details()).isEqualTo("x");
        assertThat(view.umgebung()).isEqualTo("oci-staging-free");
    }
}

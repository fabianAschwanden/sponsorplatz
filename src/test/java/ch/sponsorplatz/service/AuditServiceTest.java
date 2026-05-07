package ch.sponsorplatz.service;
import ch.sponsorplatz.organisation.Organisation;

import ch.sponsorplatz.model.AuditAktion;
import ch.sponsorplatz.model.AuditLog;
import ch.sponsorplatz.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository repository;

    private AuditService service;

    @BeforeEach
    void setUp() {
        service = new AuditService(repository);
    }

    @Test
    @DisplayName("AUDIT-01: protokolliere speichert Eintrag mit korrekten Feldern")
    void protokolliertKorrekt() {
        UUID zielId = UUID.randomUUID();
        when(repository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.protokolliere(AuditAktion.ERSTELLT, "ORGANISATION", zielId, "Organisation", "Neue Org erstellt");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAktion()).isEqualTo("ERSTELLT");
        assertThat(log.getBereich()).isEqualTo("ORGANISATION");
        assertThat(log.getZielId()).isEqualTo(zielId);
        assertThat(log.getZielTyp()).isEqualTo("Organisation");
        assertThat(log.getDetails()).isEqualTo("Neue Org erstellt");
    }

    @Test
    @DisplayName("AUDIT-02: protokolliereMitBenutzer setzt Benutzer-Felder")
    void protokolliertMitBenutzer() {
        UUID userId = UUID.randomUUID();
        UUID zielId = UUID.randomUUID();
        when(repository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.protokolliereMitBenutzer(AuditAktion.VERIFIZIERT, "ORGANISATION",
                userId, "admin@sp.ch", zielId, "Organisation", "Org verifiziert");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBenutzerId()).isEqualTo(userId);
        assertThat(captor.getValue().getBenutzerEmail()).isEqualTo("admin@sp.ch");
    }

    @Test
    @DisplayName("AUDIT-03: letzteEintraege gibt Repository-Ergebnis zurück")
    void letzteEintraege() {
        AuditLog log1 = new AuditLog();
        when(repository.findTop100ByOrderByZeitpunktDesc()).thenReturn(List.of(log1));

        List<AuditLog> result = service.letzteEintraege();
        assertThat(result).hasSize(1);
    }
}


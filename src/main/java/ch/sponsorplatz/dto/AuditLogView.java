package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO für Audit-Log Einträge — für Admin-UI.
 */
public record AuditLogView(
        UUID id,
        Instant zeitpunkt,
        String aktion,
        String bereich,
        String benutzerEmail,
        UUID zielId,
        String zielTyp,
        String details
) {

    public static AuditLogView von(AuditLog log) {
        return new AuditLogView(
                log.getId(),
                log.getZeitpunkt(),
                log.getAktion(),
                log.getBereich(),
                log.getBenutzerEmail(),
                log.getZielId(),
                log.getZielTyp(),
                log.getDetails()
        );
    }

    public static List<AuditLogView> von(List<AuditLog> logs) {
        return logs.stream().map(AuditLogView::von).toList();
    }
}


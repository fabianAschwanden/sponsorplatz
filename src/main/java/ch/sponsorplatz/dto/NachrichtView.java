package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.Nachricht;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only View-DTO für Nachrichten im Anfrage-Thread.
 * Flacht Absender-Anzeigename ein — niemals passwortHash oder andere User-Details.
 */
public record NachrichtView(
        UUID id,
        String absenderName,
        UUID absenderId,
        String text,
        Instant createdAt
) {

    public static NachrichtView von(Nachricht n) {
        return new NachrichtView(
                n.getId(),
                n.getAbsender() != null ? n.getAbsender().getAnzeigename() : "Unbekannt",
                n.getAbsender() != null ? n.getAbsender().getId() : null,
                n.getText(),
                n.getCreatedAt()
        );
    }

    public static List<NachrichtView> von(List<Nachricht> nachrichten) {
        return nachrichten.stream().map(NachrichtView::von).toList();
    }
}


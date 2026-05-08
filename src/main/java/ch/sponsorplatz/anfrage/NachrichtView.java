package ch.sponsorplatz.anfrage;


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
        String absenderProfilbildUrl,
        String text,
        Instant createdAt
) {

    public static NachrichtView von(Nachricht n) {
        var absender = n.getAbsender();
        String bildUrl = (absender != null && absender.getProfilbildId() != null)
                ? "/medien/" + absender.getProfilbildId()
                : null;
        return new NachrichtView(
                n.getId(),
                absender != null ? absender.getAnzeigename() : "Unbekannt",
                absender != null ? absender.getId() : null,
                bildUrl,
                n.getText(),
                n.getCreatedAt()
        );
    }

    public static List<NachrichtView> von(List<Nachricht> nachrichten) {
        return nachrichten.stream().map(NachrichtView::von).toList();
    }
}


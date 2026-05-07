package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.Benachrichtigung;
import ch.sponsorplatz.model.BenachrichtigungTyp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO für Benachrichtigungen.
 */
public record BenachrichtigungView(
        UUID id,
        BenachrichtigungTyp typ,
        String titel,
        String text,
        String link,
        boolean gelesen,
        Instant createdAt
) {

    public static BenachrichtigungView von(Benachrichtigung b) {
        return new BenachrichtigungView(
                b.getId(),
                b.getTyp(),
                b.getTitel(),
                b.getText(),
                b.getLink(),
                b.isGelesen(),
                b.getCreatedAt()
        );
    }

    public static List<BenachrichtigungView> von(List<Benachrichtigung> liste) {
        return liste.stream().map(BenachrichtigungView::von).toList();
    }
}


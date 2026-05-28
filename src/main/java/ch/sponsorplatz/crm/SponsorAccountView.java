package ch.sponsorplatz.crm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO für {@link SponsorAccount} (View-DTO-Pflicht — Entities verlassen den
 * Service-Layer nicht). Flacht den Verein auf {@code vereinName}/{@code vereinSlug}
 * ein, damit das Template keine JPA-Relation anfasst. Enthält bewusst keinen
 * Mandanten-Schlüssel im Output — er ist Zugriffs-, kein Anzeige-Belang.
 */
public record SponsorAccountView(
        UUID id,
        UUID vereinOrgId,
        String vereinName,
        String vereinSlug,
        UUID accountOwnerUserId,
        AccountStatus status,
        AccountTier tier,
        String notiz,
        Instant erstelltAm,
        Instant aktualisiertAm
) {

    public static SponsorAccountView von(SponsorAccount account) {
        return new SponsorAccountView(
                account.getId(),
                account.getVerein().getId(),
                account.getVerein().getName(),
                account.getVerein().getSlug(),
                account.getAccountOwnerUserId(),
                account.getStatus(),
                account.getTier(),
                account.getNotiz(),
                account.getErstelltAm(),
                account.getAktualisiertAm());
    }

    public static List<SponsorAccountView> von(List<SponsorAccount> accounts) {
        return accounts.stream().map(SponsorAccountView::von).toList();
    }
}

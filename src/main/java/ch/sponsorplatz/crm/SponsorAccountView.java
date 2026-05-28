package ch.sponsorplatz.crm;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        PipelineStage pipelineStage,
        BigDecimal forecastBetragChf,
        BigDecimal gewichteterForecastChf,
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
                account.getPipelineStage(),
                account.getForecastBetragChf(),
                gewichte(account.getForecastBetragChf(), account.getPipelineStage()),
                account.getNotiz(),
                account.getErstelltAm(),
                account.getAktualisiertAm());
    }

    /**
     * Gewichteter Forecast: {@code Betrag × Stufen-Wahrscheinlichkeit / 100}.
     * Null-Betrag → null (kein erwartetes Volumen erfasst).
     */
    private static BigDecimal gewichte(BigDecimal betrag, PipelineStage stage) {
        if (betrag == null || stage == null) return null;
        return betrag.multiply(BigDecimal.valueOf(stage.standardWahrscheinlichkeit()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public static List<SponsorAccountView> von(List<SponsorAccount> accounts) {
        return accounts.stream().map(SponsorAccountView::von).toList();
    }
}

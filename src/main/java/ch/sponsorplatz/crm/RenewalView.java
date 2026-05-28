package ch.sponsorplatz.crm;

import ch.sponsorplatz.anfrage.Vertrag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO für die Renewal-Pipeline der CRM-Layer (ADR-0011): ein
 * unterzeichneter {@link Vertrag} eines Sponsors, dessen Laufzeit bald endet.
 * Flacht den Verein auf {@code vereinName}/{@code vereinSlug} ein und trägt
 * {@code tageVerbleibend} (negativ = bereits überfällig) für die Dringlichkeit.
 * Kein Mandanten-Schlüssel im Output — er ist Zugriffs-, kein Anzeige-Belang.
 */
public record RenewalView(
        UUID vertragId,
        UUID vereinOrgId,
        String vereinName,
        String vereinSlug,
        String paketName,
        BigDecimal preisChf,
        LocalDate laufzeitBis,
        long tageVerbleibend,
        boolean istUeberfaellig
) {

    public static RenewalView von(Vertrag vertrag, LocalDate heute) {
        long tage = ChronoUnit.DAYS.between(heute, vertrag.getLaufzeitBis());
        return new RenewalView(
                vertrag.getId(),
                vertrag.getOrg().getId(),
                vertrag.getOrgName(),
                vertrag.getOrg().getSlug(),
                vertrag.getPaketName(),
                vertrag.getPreisChf(),
                vertrag.getLaufzeitBis(),
                tage,
                tage < 0);
    }

    public static List<RenewalView> von(List<Vertrag> vertraege, LocalDate heute) {
        return vertraege.stream().map(v -> von(v, heute)).toList();
    }
}

package ch.sponsorplatz.anfrage;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * View-DTO für Rechnungs-Detail- und PDF-Templates.
 */
public record RechnungView(
        UUID id,
        UUID vertragId,
        String orgName,
        String orgSlug,
        String rechnungsnummer,
        RechnungsStatus status,
        BigDecimal betragChf,
        String iban,
        String qrReferenz,
        String sponsorName,
        String sponsorEmail,
        String sponsorAdresse,
        String zahlungszweck,
        Instant erstelltAm,
        String erstelltVon,
        LocalDate faelligAm,
        Instant bezahltAm,
        String bezahltVon
) {
    public static RechnungView von(Rechnung r) {
        return new RechnungView(
                r.getId(),
                r.getVertrag() != null ? r.getVertrag().getId() : null,
                r.getOrg() != null ? r.getOrg().getName() : null,
                r.getOrg() != null ? r.getOrg().getSlug() : null,
                r.getRechnungsnummer(),
                r.getStatus(),
                r.getBetragChf(),
                r.getIban(),
                r.getQrReferenz(),
                r.getSponsorName(),
                r.getSponsorEmail(),
                r.getSponsorAdresse(),
                r.getZahlungszweck(),
                r.getErstelltAm(),
                r.getErstelltVon(),
                r.getFaelligAm(),
                r.getBezahltAm(),
                r.getBezahltVon()
        );
    }
}

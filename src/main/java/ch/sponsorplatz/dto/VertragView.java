package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.Vertrag;
import ch.sponsorplatz.model.VertragsStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * View-DTO für Vertrags-Detail- und PDF-Templates. Flacht Org-Referenzen
 * auf Slugs/Names ein, damit Templates nicht auf JPA-Entities zugreifen.
 */
public record VertragView(
        UUID id,
        UUID anfrageId,
        VertragsStatus status,
        String orgName,
        String orgSlug,
        String sponsorName,
        String sponsorEmail,
        String sponsorOrgName,
        String paketName,
        String paketBeschreibung,
        BigDecimal preisChf,
        LocalDate laufzeitVon,
        LocalDate laufzeitBis,
        String leistungVerein,
        String leistungSponsor,
        Instant erstelltAm,
        String erstelltVon,
        Instant unterzeichnetAm,
        String unterzeichnetVon
) {
    public static VertragView von(Vertrag v) {
        return new VertragView(
                v.getId(),
                v.getAnfrage() != null ? v.getAnfrage().getId() : null,
                v.getStatus(),
                v.getOrgName(),
                v.getOrg() != null ? v.getOrg().getSlug() : null,
                v.getSponsorName(),
                v.getSponsorEmail(),
                v.getSponsorOrg() != null ? v.getSponsorOrg().getName() : null,
                v.getPaketName(),
                v.getPaketBeschreibung(),
                v.getPreisChf(),
                v.getLaufzeitVon(),
                v.getLaufzeitBis(),
                v.getLeistungVerein(),
                v.getLeistungSponsor(),
                v.getErstelltAm(),
                v.getErstelltVon(),
                v.getUnterzeichnetAm(),
                v.getUnterzeichnetVon()
        );
    }
}

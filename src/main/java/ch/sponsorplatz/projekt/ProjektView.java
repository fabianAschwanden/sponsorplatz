package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Branche;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Immutable View-DTO für Projekt — inkl. nested OrganisationKurzView.
 */
public record ProjektView(
        UUID id,
        String name,
        String slug,
        Sichtbarkeit sichtbarkeit,
        String kategorie,
        String ort,
        LocalDate startDatum,
        LocalDate endDatum,
        String beschreibung,
        Instant veroeffentlichtAm,
        OrganisationKurzView org,
        String coverUrl) {

    public static ProjektView von(Projekt projekt) {
        return von(projekt, null);
    }

    public static ProjektView von(Projekt projekt, String coverUrl) {
        return new ProjektView(
                projekt.getId(),
                projekt.getName(),
                projekt.getSlug(),
                projekt.getSichtbarkeit(),
                projekt.getKategorie(),
                projekt.getOrt(),
                projekt.getStartDatum(),
                projekt.getEndDatum(),
                projekt.getBeschreibung(),
                projekt.getVeroeffentlichtAm(),
                OrganisationKurzView.von(projekt.getOrg()),
                coverUrl);
    }

    /** Kopie der View mit ergänztem {@code coverUrl} — für Listen-Mapping ohne Entity-Touch. */
    public ProjektView mitCoverUrl(String coverUrl) {
        return new ProjektView(id, name, slug, sichtbarkeit, kategorie, ort,
                startDatum, endDatum, beschreibung, veroeffentlichtAm, org, coverUrl);
    }

    /**
     * Kurzversion einer Organisation für nested Darstellung.
     */
    public record OrganisationKurzView(UUID id, String name, String slug, Branche branche) {
        public static OrganisationKurzView von(ch.sponsorplatz.organisation.Organisation org) {
            return new OrganisationKurzView(org.getId(), org.getName(), org.getSlug(), org.getBranche());
        }
    }
}

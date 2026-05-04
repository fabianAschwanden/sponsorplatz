package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.OrgStatus;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable View-DTO für Organisation — verhindert, dass Entities den Service-Layer verlassen.
 */
public record OrganisationView(
        UUID id,
        String name,
        String slug,
        OrgTyp typ,
        OrgStatus status,
        String rechtsform,
        String branche,
        String beschreibung,
        String websiteUrl,
        Instant registriertAm,
        Instant verifiziertAm
) {

    public static OrganisationView von(Organisation org) {
        return new OrganisationView(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getTyp(),
                org.getStatus(),
                org.getRechtsform(),
                org.getBranche(),
                org.getBeschreibung(),
                org.getWebsiteUrl(),
                org.getRegistriertAm(),
                org.getVerifiziertAm()
        );
    }

    public static List<OrganisationView> von(List<Organisation> orgs) {
        return orgs.stream().map(OrganisationView::von).toList();
    }
}


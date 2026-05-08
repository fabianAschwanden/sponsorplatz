package ch.sponsorplatz.organisation;


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
        Branche branche,
        String beschreibung,
        String websiteUrl,
        Instant registriertAm,
        Instant verifiziertAm,
        UUID uebergeordneteOrgId,
        String uebergeordneteOrgName,
        String uebergeordneteOrgSlug
) {

    public static OrganisationView von(Organisation org) {
        Organisation eltern = org.getUebergeordneteOrg();
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
                org.getVerifiziertAm(),
                eltern != null ? eltern.getId() : null,
                eltern != null ? eltern.getName() : null,
                eltern != null ? eltern.getSlug() : null
        );
    }

    public static List<OrganisationView> von(List<Organisation> orgs) {
        return orgs.stream().map(OrganisationView::von).toList();
    }

    /** Prüft ob diese Org eine Unterorganisation ist. */
    public boolean istUnterorganisation() {
        return uebergeordneteOrgId != null;
    }
}


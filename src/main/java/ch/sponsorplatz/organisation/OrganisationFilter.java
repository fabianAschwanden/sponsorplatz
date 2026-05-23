package ch.sponsorplatz.organisation;

import java.util.Locale;

/**
 * Filter-Kriterien für die Organisationen-Liste.
 *
 * <p>Alle Felder sind optional ({@code null} oder Leerstring = nicht gesetzt).
 * Mehrere gesetzte Felder werden mit UND verknüpft.
 *
 * <p>{@code branche} ist ein String, weil dasselbe Feld sowohl
 * {@link Branche} (Vereine) als auch {@link SponsorBranche} (Unternehmen)
 * matchen kann — das Template zeigt beide Enum-Familien als
 * {@code <optgroup>}s im selben Select.
 */
public record OrganisationFilter(OrgTyp typ, OrgStatus status, String branche, String suche) {

    public boolean istLeer() {
        return typ == null
                && status == null
                && (branche == null || branche.isBlank())
                && (suche == null || suche.isBlank());
    }

    public boolean matcht(OrganisationView v) {
        if (typ != null && v.typ() != typ) return false;
        if (status != null && v.status() != status) return false;
        if (branche != null && !branche.isBlank() && !brancheMatch(v)) return false;
        if (suche != null && !suche.isBlank() && !nameMatch(v)) return false;
        return true;
    }

    private boolean brancheMatch(OrganisationView v) {
        boolean vereinTreffer = v.branche() != null && branche.equals(v.branche().name());
        boolean sponsorTreffer = v.sponsorBranche() != null && branche.equals(v.sponsorBranche().name());
        return vereinTreffer || sponsorTreffer;
    }

    private boolean nameMatch(OrganisationView v) {
        return v.name() != null
                && v.name().toLowerCase(Locale.ROOT).contains(suche.toLowerCase(Locale.ROOT));
    }
}

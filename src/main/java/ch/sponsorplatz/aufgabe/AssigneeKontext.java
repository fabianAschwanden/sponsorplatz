package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.anfrage.Rechnung;
import ch.sponsorplatz.anfrage.SponsoringAnfrage;
import ch.sponsorplatz.anfrage.Vertrag;
import ch.sponsorplatz.organisation.Organisation;

import java.util.Optional;

/**
 * Hält für eine konkrete Trigger-Entity alle Org-Referenzen bereit, die eine
 * {@link AssigneeRegel} potenziell auflösen kann. Wird im jeweiligen
 * Service-Trigger befüllt — die {@link AufgabenEngine} braucht so kein
 * Repository-Lookup, um z.B. {@code vertrag.getSponsorOrg()} zu erreichen.
 */
public record AssigneeKontext(
        Organisation vereinSeite,
        Organisation sponsorSeite,
        Organisation empfaengerOrg,
        Organisation anfragenderOrg
) {

    public static AssigneeKontext ausOrg(Organisation org) {
        return new AssigneeKontext(org, null, null, null);
    }

    public static AssigneeKontext ausAnfrage(SponsoringAnfrage anfrage) {
        return new AssigneeKontext(null, null, anfrage.getEmpfaengerOrg(), anfrage.getAnfragenderOrg());
    }

    public static AssigneeKontext ausVertrag(Vertrag vertrag) {
        return new AssigneeKontext(vertrag.getOrg(), vertrag.getSponsorOrg(), null, null);
    }

    public static AssigneeKontext ausRechnung(Rechnung rechnung) {
        return new AssigneeKontext(rechnung.getOrg(), null, null, null);
    }

    /** Wendet die Regel auf den Kontext an. Leeres Optional bedeutet „kein Assignee auflösbar". */
    public Optional<Organisation> aufloesen(AssigneeRegel regel) {
        return switch (regel) {
            case PLATFORM_ADMIN              -> Optional.empty(); // Engine signalisiert das via nurPlatformAdmin
            case ORG_MITGLIEDER              -> Optional.ofNullable(vereinSeite);
            case ANFRAGE_EMPFAENGER_ORG      -> Optional.ofNullable(empfaengerOrg);
            case ANFRAGE_ANFRAGENDER_ORG     -> Optional.ofNullable(anfragenderOrg);
            case VERTRAG_VEREIN_ORG          -> Optional.ofNullable(vereinSeite);
            case VERTRAG_SPONSOR_ORG         -> Optional.ofNullable(sponsorSeite);
            case RECHNUNG_VEREIN_ORG         -> Optional.ofNullable(vereinSeite);
        };
    }
}

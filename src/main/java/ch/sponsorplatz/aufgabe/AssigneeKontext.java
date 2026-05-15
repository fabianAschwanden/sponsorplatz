package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.organisation.Organisation;

import java.util.Optional;

/**
 * Hält für eine konkrete Trigger-Entity alle Org-Referenzen bereit, die eine
 * {@link AssigneeRegel} potenziell auflösen kann. Wird im jeweiligen
 * Service-Trigger befüllt — die {@link AufgabenEngine} braucht so kein
 * Repository-Lookup, um z.B. {@code vertrag.getSponsorOrg()} zu erreichen.
 *
 * <p>Bewusst nur {@link Organisation} als Feldtyp: höhere Domains (Anfrage,
 * Vertrag, Rechnung) bauen den Kontext aus ihren eigenen Entities selbst —
 * so muss {@code aufgabe} keine ihrer Klassen kennen (ARCH-06).
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

    public static AssigneeKontext ausAnfrageOrgs(Organisation empfaengerOrg, Organisation anfragenderOrg) {
        return new AssigneeKontext(null, null, empfaengerOrg, anfragenderOrg);
    }

    public static AssigneeKontext ausVertragOrgs(Organisation vereinOrg, Organisation sponsorOrg) {
        return new AssigneeKontext(vereinOrg, sponsorOrg, null, null);
    }

    public static AssigneeKontext ausRechnungOrg(Organisation vereinOrg) {
        return new AssigneeKontext(vereinOrg, null, null, null);
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

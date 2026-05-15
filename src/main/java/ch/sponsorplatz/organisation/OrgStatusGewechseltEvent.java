package ch.sponsorplatz.organisation;

/**
 * Event: Status einer Organisation hat sich geändert (PENDING → VERIFIED,
 * VERIFIED → SUSPENDED usw.). Wird vom {@link OrganisationService} bei jeder
 * Status-Mutation publiziert; das {@code aufgabe}-Paket hört darauf, um
 * Workflow-Tasks zu erzeugen/abzuschliessen.
 *
 * <p>Bricht die {@code organisation → aufgabe}-Direktreferenz, damit ARCH-06
 * (Feature-Cycles) nicht verletzt wird.
 */
public record OrgStatusGewechseltEvent(Organisation org) {}

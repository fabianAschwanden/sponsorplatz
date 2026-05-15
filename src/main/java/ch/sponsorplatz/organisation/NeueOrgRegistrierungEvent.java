package ch.sponsorplatz.organisation;

/**
 * Event: Eine neue Organisation hat sich registriert (Status PENDING) und
 * wartet auf Verifizierung. Wird von {@link OrganisationService} und
 * {@link SponsorRegistrierungService} publiziert; Listener-Adapter im
 * {@code admin}-Paket benachrichtigt PLATFORM_ADMINs.
 *
 * <p>Spring-Events brechen die {@code organisation → admin}-Direktreferenz,
 * damit ARCH-06 (Feature-Cycles) nicht verletzt wird.
 */
public record NeueOrgRegistrierungEvent(Organisation org) {}

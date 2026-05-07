package ch.sponsorplatz.service;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.model.Rolle;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
import ch.sponsorplatz.repository.OrganisationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * AccessControl-Bean — prüft Berechtigungen auf Organisations-Ebene.
 */
@Component("accessControl")
public class AccessControl {

    private static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final AppUserRepository appUserRepository;
    private final OrganisationRepository organisationRepository;

    public AccessControl(MitgliedschaftRepository mitgliedschaftRepository,
                         AppUserRepository appUserRepository,
                         OrganisationRepository organisationRepository) {
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.appUserRepository = appUserRepository;
        this.organisationRepository = organisationRepository;
    }

    /**
     * Prüft ob der authentifizierte User die Org bearbeiten darf.
     * True für: ORG_EDITOR, ORG_OWNER, PLATFORM_ADMIN.
     */
    public boolean kannOrgEditieren(UUID orgId, Authentication auth) {
        if (!istAuthentifiziert(auth)) return false;
        if (istPlattformAdmin(auth)) return true;

        return findeUserId(auth)
                .map(userId -> mitgliedschaftRepository.existsByUserIdAndOrgIdAndRolleIn(
                        userId, orgId, Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR)))
                .orElse(false);
    }

    /**
     * Prüft ob der authentifizierte User die Org verwalten darf (Mitglieder-Verwaltung).
     * True für: ORG_OWNER, PLATFORM_ADMIN.
     */
    public boolean kannOrgVerwalten(UUID orgId, Authentication auth) {
        if (!istAuthentifiziert(auth)) return false;
        if (istPlattformAdmin(auth)) return true;

        return findeUserId(auth)
                .map(userId -> mitgliedschaftRepository.existsByUserIdAndOrgIdAndRolle(
                        userId, orgId, Rolle.ORG_OWNER))
                .orElse(false);
    }

    /**
     * Slug-Variante von {@link #kannOrgEditieren}. Für unbekannte Slugs → false
     * (kein Throw — der Auth-Layer leakt keine Existenz-Information).
     */
    public boolean kannOrgEditierenNachSlug(String slug, Authentication auth) {
        return organisationRepository.findBySlug(slug)
                .map(org -> kannOrgEditieren(org.getId(), auth))
                .orElse(false);
    }

    /**
     * Slug-Variante von {@link #kannOrgVerwalten}.
     */
    public boolean kannOrgVerwaltenNachSlug(String slug, Authentication auth) {
        return organisationRepository.findBySlug(slug)
                .map(org -> kannOrgVerwalten(org.getId(), auth))
                .orElse(false);
    }

    private boolean istAuthentifiziert(Authentication auth) {
        return auth != null && auth.isAuthenticated();
    }

    private boolean istPlattformAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_PLATFORM_ADMIN::equals);
    }

    private Optional<UUID> findeUserId(Authentication auth) {
        return appUserRepository.findByEmail(auth.getName())
                .map(AppUser::getId);
    }
}


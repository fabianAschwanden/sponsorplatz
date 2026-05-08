package ch.sponsorplatz.organisation;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AccessControl-Bean — prüft Berechtigungen auf Organisations-Ebene.
 *
 * <p>Unterstützt hierarchische Vererbung: Ein ORG_OWNER/ORG_EDITOR
 * einer Eltern-Org hat implizit dieselben Rechte auf alle Kind-Orgs.</p>
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
     * True für: ORG_EDITOR/ORG_OWNER auf dieser oder einer Eltern-Org, PLATFORM_ADMIN.
     */
    public boolean kannOrgEditieren(UUID orgId, Authentication auth) {
        if (!istAuthentifiziert(auth)) return false;
        if (istPlattformAdmin(auth)) return true;

        return findeUserId(auth)
                .map(userId -> hatBerechtigungMitVererbung(
                        userId, orgId, Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR)))
                .orElse(false);
    }

    /**
     * Prüft ob der authentifizierte User die Org verwalten darf (Mitglieder-Verwaltung).
     * True für: ORG_OWNER auf dieser oder einer Eltern-Org, PLATFORM_ADMIN.
     */
    public boolean kannOrgVerwalten(UUID orgId, Authentication auth) {
        if (!istAuthentifiziert(auth)) return false;
        if (istPlattformAdmin(auth)) return true;

        return findeUserId(auth)
                .map(userId -> hatBerechtigungMitVererbung(
                        userId, orgId, Set.of(Rolle.ORG_OWNER)))
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

    /**
     * Prüft Berechtigung auf der Org selbst oder einer Eltern-Org in EINEM
     * Query (rekursive CTE). Ersetzt die alte iterative Variante mit N×2
     * Queries pro Auth-Check — bei tiefen Hierarchien spürbar schneller,
     * bei flachen kein Mehraufwand.
     */
    private boolean hatBerechtigungMitVererbung(UUID userId, UUID orgId, Collection<Rolle> rollen) {
        Collection<String> rollenAlsString = rollen.stream()
                .map(Rolle::name)
                .collect(Collectors.toSet());
        return mitgliedschaftRepository
                .zaehleMitgliedschaftenInHierarchie(userId, orgId, rollenAlsString) > 0;
    }
}


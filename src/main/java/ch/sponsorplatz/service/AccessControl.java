package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Rolle;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
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

    public AccessControl(MitgliedschaftRepository mitgliedschaftRepository,
                         AppUserRepository appUserRepository) {
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.appUserRepository = appUserRepository;
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


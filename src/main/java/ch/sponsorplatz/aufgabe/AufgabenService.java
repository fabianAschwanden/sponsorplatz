package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * User-facing Service: liefert „meine offenen Aufgaben" und erlaubt das manuelle
 * Markieren als erledigt. Die Auto-Erledigung über Status-Wechsel der Trigger-Entity
 * läuft separat über {@link AufgabenEngine}.
 *
 * <p>Sichtbarkeit: User sieht alle Aufgaben, deren {@code assigneeOrg} zu einer
 * seiner Mitgliedschaften gehört (jede Rolle), plus — wenn PLATFORM_ADMIN — alle
 * {@code nurPlatformAdmin}-Aufgaben.
 */
@Service
@Transactional
public class AufgabenService {

    private static final Set<Rolle> ALLE_ROLLEN = Set.of(
            Rolle.ORG_OWNER, Rolle.ORG_EDITOR, Rolle.ORG_VIEWER);

    private final AufgabeRepository aufgabeRepository;
    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;

    public AufgabenService(AufgabeRepository aufgabeRepository,
                            AppUserRepository appUserRepository,
                            MitgliedschaftRepository mitgliedschaftRepository) {
        this.aufgabeRepository = aufgabeRepository;
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
    }

    @Transactional(readOnly = true)
    public List<Aufgabe> meineOffenen(String email) {
        Sichtbarkeit s = sichtbarkeit(email);
        return aufgabeRepository.findOffeneFuer(s.orgIds(), s.istAdmin());
    }

    @Transactional(readOnly = true)
    public long zaehleMeineOffenen(String email) {
        Sichtbarkeit s = sichtbarkeit(email);
        return aufgabeRepository.zaehleOffeneFuer(s.orgIds(), s.istAdmin());
    }

    /**
     * Manuelles Erledigen — z.B. wenn der User die Aufgabe nicht über den
     * Standard-Workflow abschließt (Notiz: das automatische Erledigen läuft
     * über {@link AufgabenEngine}). Rückgabe ist ein View-DTO, damit der
     * Controller keine Entity ans Template gibt (CLAUDE.md View-Pflicht).
     */
    public AufgabeView markiereErledigt(UUID aufgabeId, String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User nicht gefunden: " + email));
        Aufgabe a = aufgabeRepository.findById(aufgabeId)
                .orElseThrow(() -> new NotFoundException("Aufgabe nicht gefunden: " + aufgabeId));

        if (!darfSehen(a, user)) {
            throw new AccessDeniedException("Aufgabe gehört nicht zur Sichtbarkeit des Users");
        }
        if (a.getStatus() != AufgabenStatus.OFFEN) {
            return AufgabeView.von(a); // Idempotent — bereits erledigt
        }
        a.setStatus(AufgabenStatus.ERLEDIGT);
        a.setErledigtAm(Instant.now());
        a.setErledigtVon(user);
        return AufgabeView.von(aufgabeRepository.save(a));
    }

    private Sichtbarkeit sichtbarkeit(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User nicht gefunden: " + email));
        List<UUID> orgIds = mitgliedschaftRepository
                .findByUserIdAndRolleInMitOrg(user.getId(), ALLE_ROLLEN)
                .stream()
                .map(Mitgliedschaft::getOrg)
                .map(o -> o.getId())
                .distinct()
                .toList();
        boolean istAdmin = user.getPlatformRolle() == PlatformRolle.PLATFORM_ADMIN;
        return new Sichtbarkeit(orgIds, istAdmin);
    }

    private boolean darfSehen(Aufgabe a, AppUser user) {
        if (a.isNurPlatformAdmin()) {
            return user.getPlatformRolle() == PlatformRolle.PLATFORM_ADMIN;
        }
        if (a.getAssigneeOrg() == null) {
            return false;
        }
        UUID orgId = a.getAssigneeOrg().getId();
        return mitgliedschaftRepository
                .findByUserIdAndRolleInMitOrg(user.getId(), ALLE_ROLLEN)
                .stream()
                .anyMatch(m -> m.getOrg().getId().equals(orgId));
    }

    private record Sichtbarkeit(List<UUID> orgIds, boolean istAdmin) {}
}

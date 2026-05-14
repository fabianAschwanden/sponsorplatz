package ch.sponsorplatz.organisation;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service für Mitgliedschaft-Verwaltung.
 */
@Service
@Transactional
public class MitgliedschaftService {

    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final AppUserRepository appUserRepository;
    private final OrganisationRepository organisationRepository;

    public MitgliedschaftService(MitgliedschaftRepository mitgliedschaftRepository,
                                  AppUserRepository appUserRepository,
                                  OrganisationRepository organisationRepository) {
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.appUserRepository = appUserRepository;
        this.organisationRepository = organisationRepository;
    }

    /**
     * Fügt eine Mitgliedschaft hinzu.
     *
     * @throws IllegalStateException falls User bereits Mitglied der Org ist
     * @throws IllegalArgumentException falls Org oder User nicht existiert
     */
    public Mitgliedschaft fuegeHinzu(UUID orgId, UUID userId, Rolle rolle, UUID eingeladenVonId) {
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden"));
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden"));

        if (mitgliedschaftRepository.existsByUserIdAndOrgId(userId, orgId)) {
            throw new IllegalStateException("Benutzer ist bereits Mitglied dieser Organisation");
        }

        Mitgliedschaft mitgliedschaft = new Mitgliedschaft();
        mitgliedschaft.setUser(user);
        mitgliedschaft.setOrg(org);
        mitgliedschaft.setRolle(rolle);

        if (eingeladenVonId != null) {
            appUserRepository.findById(eingeladenVonId)
                    .ifPresent(mitgliedschaft::setEingeladenVon);
        }

        return mitgliedschaftRepository.save(mitgliedschaft);
    }

    /**
     * Entfernt eine Mitgliedschaft.
     */
    public void entferne(UUID mitgliedschaftId) {
        mitgliedschaftRepository.deleteById(mitgliedschaftId);
    }

    @Transactional(readOnly = true)
    public List<Mitgliedschaft> findeNachOrg(UUID orgId) {
        return mitgliedschaftRepository.findByOrgId(orgId);
    }

    /**
     * Org-IDs aller Mitgliedschaften eines Users — Komfort-Methode für
     * Controller, damit sie {@link MitgliedschaftRepository} nicht direkt
     * injizieren müssen (ARCH-01).
     */
    @Transactional(readOnly = true)
    public List<UUID> findeOrgIdsVonUser(UUID userId) {
        return mitgliedschaftRepository.findOrgIdsByUserId(userId);
    }

    /**
     * Mitgliedschaften eines Users mit konkreten Rollen, mit eager-geladener
     * Org (für Controller-Listen ohne LazyInit-Risiko).
     */
    @Transactional(readOnly = true)
    public List<Mitgliedschaft> findeMitgliedschaftenVonUser(UUID userId,
            java.util.Set<Rolle> rollen) {
        return mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(userId, rollen);
    }

    public boolean existierenMitgliedschaftenFuerOrg(UUID orgId) {
        return mitgliedschaftRepository.existsByOrgId(orgId);
    }
}


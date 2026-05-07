package ch.sponsorplatz.service;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.model.Mitgliedschaft;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.Rolle;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
import ch.sponsorplatz.repository.OrganisationRepository;
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

    @Transactional(readOnly = true)
    public boolean existierenMitgliedschaftenFuerOrg(UUID orgId) {
        return mitgliedschaftRepository.existsByOrgId(orgId);
    }
}


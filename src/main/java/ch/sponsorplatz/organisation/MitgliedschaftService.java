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

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<MitgliedView> findeViewsNachOrg(UUID orgId) {
        return MitgliedView.von(mitgliedschaftRepository.findByOrgId(orgId));
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

    /**
     * Org-IDs aller Mitgliedschaften eines Users mit beliebiger Rolle aus der
     * Menge — Controller braucht keine Entity-Liste (ARCH-02).
     */
    @Transactional(readOnly = true)
    public List<UUID> findeOrgIdsVonUserMitRollen(UUID userId, java.util.Set<Rolle> rollen) {
        return findeMitgliedschaftenVonUser(userId, rollen).stream()
                .map(m -> m.getOrg().getId())
                .toList();
    }

    /**
     * Aggregat-Snapshot für die /anfragen-Seite — bündelt die wiederkehrenden
     * Berechnungen (alle Org-IDs, Vereins-Org-IDs mit Edit-Recht, Vereins-Org-
     * Namen) in einem Datenobjekt, damit der Controller keine Entity-Streams
     * mehr selbst aufmacht (ARCH-02).
     */
    @Transactional(readOnly = true)
    public AnfragenSeitenDaten findeAnfragenSeitenDaten(UUID userId) {
        java.util.Set<Rolle> alle = java.util.Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR, Rolle.ORG_VIEWER);
        java.util.Set<Rolle> editRollen = java.util.Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR);
        List<Mitgliedschaft> mitgliedschaften = findeMitgliedschaftenVonUser(userId, alle);
        List<UUID> alleOrgIds = mitgliedschaften.stream()
                .map(m -> m.getOrg().getId()).toList();
        List<Mitgliedschaft> mitEdit = mitgliedschaften.stream()
                .filter(m -> editRollen.contains(m.getRolle()))
                .filter(m -> m.getOrg().getTyp() == OrgTyp.VEREIN)
                .toList();
        List<UUID> vereinsOrgIds = mitEdit.stream()
                .map(m -> m.getOrg().getId()).toList();
        List<String> vereinsOrgNamen = mitEdit.stream()
                .map(m -> m.getOrg().getName()).toList();
        return new AnfragenSeitenDaten(alleOrgIds, vereinsOrgIds, vereinsOrgNamen);
    }

    /** Datenpaket für die /anfragen-Seite. */
    public record AnfragenSeitenDaten(
            List<UUID> alleOrgIds,
            List<UUID> vereinsOrgIds,
            List<String> vereinsOrgNamen) {}

    /**
     * Vereins-Orgs eines Users mit Edit-Recht als View — für den
     * Sponsor-Picker im Kontakt-Anfrage-Formular (ARCH-02).
     */
    @Transactional(readOnly = true)
    public List<OrganisationView> findeMeineVereinsOrgViews(UUID userId) {
        java.util.Set<Rolle> editRollen = java.util.Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR);
        return findeMitgliedschaftenVonUser(userId, editRollen).stream()
                .map(Mitgliedschaft::getOrg)
                .filter(o -> o.getTyp() == OrgTyp.VEREIN)
                .map(OrganisationView::von)
                .toList();
    }

    /**
     * Edit-fähige Orgs eines Users abzüglich der angegebenen Empfänger-Org —
     * für den Anfrage-Form-Picker (ARCH-02).
     */
    @Transactional(readOnly = true)
    public List<OrganisationView> findeMeineOrgsAusser(UUID userId, UUID empfaengerOrgId) {
        java.util.Set<Rolle> editRollen = java.util.Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR);
        return findeMitgliedschaftenVonUser(userId, editRollen).stream()
                .map(Mitgliedschaft::getOrg)
                .filter(o -> !o.getId().equals(empfaengerOrgId))
                .map(OrganisationView::von)
                .toList();
    }

    public boolean existierenMitgliedschaftenFuerOrg(UUID orgId) {
        return mitgliedschaftRepository.existsByOrgId(orgId);
    }
}


package ch.sponsorplatz.organisation;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.shared.util.SlugGenerator;

import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Geschäftslogik für Organisations-CRUD.
 *
 * Aufgaben:
 * - Slug aus Name generieren, wenn keiner angegeben
 * - Slug-Eindeutigkeit prüfen (Service-seitig, ergänzend zur DB-Constraint für aussagekräftige Fehler)
 * - Validierung jenseits der DTO-Constraints
 */
@Service
@Transactional
public class OrganisationService {

    private final OrganisationRepository repository;
    private final SlugGenerator slugGenerator;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final AppUserRepository appUserRepository;

    public OrganisationService(OrganisationRepository repository, SlugGenerator slugGenerator,
                               MitgliedschaftRepository mitgliedschaftRepository,
                               AppUserRepository appUserRepository) {
        this.repository = repository;
        this.slugGenerator = slugGenerator;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<Organisation> alle() {
        return repository.findAllByOrderByNameAsc();
    }

    /**
     * Aktive Sponsor-Orgs (Typ UNTERNEHMEN, Status VERIFIED oder ACTIVE) —
     * für den Sponsor-Picker im Verein→Sponsor-Anfrage-Flow. PENDING und
     * SUSPENDED bleiben ausgeblendet, damit nicht-verifizierte Sponsoren
     * nicht angefragt werden können.
     */
    @Transactional(readOnly = true)
    public List<Organisation> findeAktiveSponsoren() {
        return repository.findByTypAndStatusInOrderByNameAsc(
                OrgTyp.UNTERNEHMEN,
                List.of(OrgStatus.VERIFIED, OrgStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public Optional<Organisation> findeNachId(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Organisation> findeNachSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return repository.findBySlug(slug);
    }

    /**
     * Direkte Untergeordnete einer Org — für Detail-Anzeige + Hierarchie-Navigation.
     */
    @Transactional(readOnly = true)
    public List<Organisation> findeUntergeordnete(UUID elternId) {
        return repository.findByUebergeordneteOrgIdOrderByNameAsc(elternId);
    }

    /**
     * Legt eine neue Organisation an. Slug wird aus dem Namen generiert,
     * falls keiner mitgegeben wurde.
     *
     * @throws IllegalArgumentException bei Slug-Konflikt
     */
    public Organisation erstelle(OrganisationFormDto dto) {
        Organisation org = new Organisation();
        wendeFormDatenAn(org, dto);
        return repository.save(org);
    }

    /**
     * Legt eine neue Organisation an und verknüpft den ersteller-User automatisch
     * als {@link Rolle#ORG_OWNER}. Wird vom regulären Controller (eingeloggte User)
     * genutzt — so kann der Ersteller die Org sofort bearbeiten/verwalten.
     *
     * @throws IllegalArgumentException bei Slug-Konflikt
     * @throws NotFoundException falls User nicht existiert
     */
    public Organisation erstelleMitEigentuemer(OrganisationFormDto dto, UUID eigentuemerUserId) {
        Organisation org = erstelle(dto);


        Mitgliedschaft mitgliedschaft = new Mitgliedschaft();
        mitgliedschaft.setOrg(org);
        mitgliedschaft.setUser(
                appUserRepository.findById(eigentuemerUserId)
                        .orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden: " + eigentuemerUserId)));
        mitgliedschaft.setRolle(Rolle.ORG_OWNER);
        mitgliedschaftRepository.save(mitgliedschaft);
        return org;
    }

    /**
     * Aktualisiert eine bestehende Organisation. Identifiziert via Slug aus URL —
     * niemals via Body-Parameter (Mass-Assignment-Defense, K3).
     *
     * @throws NotFoundException        wenn Slug nicht existiert
     * @throws IllegalArgumentException bei Slug-Konflikt mit anderer Org
     */
    public Organisation aktualisiere(String slug, OrganisationFormDto dto) {
        Organisation org = repository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        wendeFormDatenAn(org, dto);
        return repository.save(org);
    }

    private void wendeFormDatenAn(Organisation org, OrganisationFormDto dto) {
        String gewuenschterSlug = (dto.getSlug() == null || dto.getSlug().isBlank())
            ? slugGenerator.fromName(dto.getName())
            : slugGenerator.fromName(dto.getSlug());

        if (slugBereitsBelegt(gewuenschterSlug, org.getId())) {
            throw new IllegalArgumentException("Slug bereits vergeben: " + gewuenschterSlug);
        }

        // Typ-spezifische Branche-XOR-Validierung. Konsistent zur DB-CHECK
        // chk_branche_pro_typ — Service-Layer wirft mit klarer Meldung, DB ist
        // Defense-in-Depth.
        validiereBrancheProTyp(dto);

        org.setTyp(dto.getTyp());
        org.setName(dto.getName().trim());
        org.setSlug(gewuenschterSlug);
        org.setRechtsform(leereAlsNull(dto.getRechtsform()));
        // Pro Typ wird genau die passende Branche-Achse gesetzt; die andere
        // wird auf null geräumt, damit Edits zwischen Org-Typen sauber bleiben.
        if (dto.getTyp() == OrgTyp.VEREIN) {
            org.setBranche(dto.getBranche());
            org.setSponsorBranche(null);
        } else if (dto.getTyp() == OrgTyp.UNTERNEHMEN) {
            org.setBranche(null);
            org.setSponsorBranche(dto.getSponsorBranche());
        } else {
            // STIFTUNG / ANDERE: beides darf gesetzt werden, was vom Form kommt
            org.setBranche(dto.getBranche());
            org.setSponsorBranche(dto.getSponsorBranche());
        }
        org.setBeschreibung(leereAlsNull(dto.getBeschreibung()));
        org.setWebsiteUrl(leereAlsNull(dto.getWebsiteUrl()));
        String iban = leereAlsNull(dto.getIban());
        org.setIban(iban != null ? iban.replace(" ", "").toUpperCase() : null);
        org.setStrasse(leereAlsNull(dto.getStrasse()));
        org.setPostleitzahl(leereAlsNull(dto.getPostleitzahl()));
        org.setOrt(leereAlsNull(dto.getOrt()));
        wendeHierarchieAn(org, dto.getUebergeordneteOrgId());
    }

    /**
     * Setzt die Eltern-Org. Validiert: Eltern existiert, ist nicht die Org
     * selbst, und der Cycle-Schutz greift (Eltern darf nicht in der eigenen
     * Untergeordneten-Kette stehen). Max-Tiefe = 3 (Konzern → Tochter →
     * Abteilung) wird ebenfalls erzwungen.
     */
    private void wendeHierarchieAn(Organisation org, UUID elternId) {
        if (elternId == null) {
            org.setUebergeordneteOrg(null);
            return;
        }
        if (org.getId() != null && elternId.equals(org.getId())) {
            throw new IllegalArgumentException(
                    "Eine Organisation kann nicht ihre eigene übergeordnete Organisation sein.");
        }
        Organisation eltern = repository.findById(elternId)
                .orElseThrow(() -> new NotFoundException(
                        "Übergeordnete Organisation nicht gefunden: " + elternId));

        // Cycle-Check: die neue Eltern-Org darf nicht selbst ein Nachfahre dieser Org sein.
        Organisation aktuell = eltern;
        int sicherheit = 0;
        while (aktuell != null && sicherheit < 10) {
            if (org.getId() != null && org.getId().equals(aktuell.getId())) {
                throw new IllegalArgumentException(
                        "Zyklische Hierarchie nicht erlaubt — die gewählte Eltern-Organisation "
                                + "ist bereits eine Untergeordnete dieser Organisation.");
            }
            aktuell = aktuell.getUebergeordneteOrg();
            sicherheit++;
        }

        // Max-Tiefe: Eltern auf Stufe N → diese Org wird Stufe N+1, max. 3.
        int elternTiefe = 1;
        Organisation t = eltern;
        while (t.getUebergeordneteOrg() != null && elternTiefe < 10) {
            elternTiefe++;
            t = t.getUebergeordneteOrg();
        }
        if (elternTiefe >= 3) {
            throw new IllegalArgumentException(
                    "Maximale Hierarchie-Tiefe (3 Stufen) erreicht — diese Org kann nicht "
                            + "noch tiefer eingehängt werden.");
        }

        org.setUebergeordneteOrg(eltern);
    }

    public void loesche(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Organisation nicht gefunden: " + id);
        }
        if (mitgliedschaftRepository.existsByOrgId(id)) {
            throw new IllegalStateException(
                    "Organisation kann nicht gelöscht werden — es existieren noch Mitgliedschaften. " +
                    "Bitte zuerst alle Mitglieder entfernen.");
        }
        if (repository.existsByUebergeordneteOrgId(id)) {
            throw new IllegalStateException(
                    "Organisation kann nicht gelöscht werden — es existieren noch Unterorganisationen. " +
                    "Bitte zuerst alle Unterorganisationen entfernen.");
        }
        repository.deleteById(id);
    }

    /**
     * Typ-spezifische Branche-Pflicht: VEREIN braucht {@code branche}
     * (Health/Sport), UNTERNEHMEN braucht {@code sponsorBranche} (Industrie),
     * STIFTUNG braucht eines von beiden.
     */
    private void validiereBrancheProTyp(OrganisationFormDto dto) {
        OrgTyp typ = dto.getTyp();
        if (typ == OrgTyp.VEREIN && dto.getBranche() == null) {
            throw new IllegalArgumentException(
                    "Branche ist Pflicht für Vereine — Sponsorplatz ist auf Sport und Gesundheit fokussiert.");
        }
        if (typ == OrgTyp.UNTERNEHMEN && dto.getSponsorBranche() == null) {
            throw new IllegalArgumentException(
                    "Industrie ist Pflicht für Sponsor-Unternehmen.");
        }
        if (typ == OrgTyp.STIFTUNG
                && dto.getBranche() == null
                && dto.getSponsorBranche() == null) {
            throw new IllegalArgumentException(
                    "Stiftungen müssen entweder eine Health/Sport-Branche oder eine Industrie wählen.");
        }
    }

    private boolean slugBereitsBelegt(String slug, UUID eigeneId) {
        return repository.findBySlug(slug)
            .filter(existierende -> !existierende.getId().equals(eigeneId))
            .isPresent();
    }

    private String leereAlsNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // --- Admin-Verifizierung ---

    /**
     * Gibt alle Organisationen mit Status PENDING zurück (älteste zuerst).
     */
    @Transactional(readOnly = true)
    public List<Organisation> findePending() {
        return repository.findByStatusOrderByCreatedAtAsc(OrgStatus.PENDING);
    }

    /**
     * Verifiziert eine Organisation (Admin-Aktion).
     *
     * @throws NotFoundException wenn ID nicht existiert
     * @throws IllegalStateException wenn Status nicht PENDING
     */
    public Organisation verifiziere(UUID id) {
        Organisation org = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + id));
        if (org.getStatus() != OrgStatus.PENDING) {
            throw new IllegalStateException(
                    "Nur PENDING-Organisationen können verifiziert werden (aktuell: " + org.getStatus() + ")");
        }
        org.setStatus(OrgStatus.VERIFIED);
        org.setVerifiziertAm(Instant.now());
        return repository.save(org);
    }

    /**
     * Suspendiert eine Organisation (Admin-Aktion).
     *
     * @throws NotFoundException wenn ID nicht existiert
     */
    public Organisation suspendiere(UUID id) {
        Organisation org = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + id));
        org.setStatus(OrgStatus.SUSPENDED);
        return repository.save(org);
    }
}

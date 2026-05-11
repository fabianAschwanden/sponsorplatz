package ch.sponsorplatz.projekt;
import ch.sponsorplatz.shared.util.SlugGenerator;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.Organisation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProjektService {

    private final ProjektRepository repository;
    private final SlugGenerator slugGenerator;
    private final VolltextSucheService volltextSuche;

    public ProjektService(ProjektRepository repository,
                          SlugGenerator slugGenerator,
                          VolltextSucheService volltextSuche) {
        this.repository = repository;
        this.slugGenerator = slugGenerator;
        this.volltextSuche = volltextSuche;
    }

    @Transactional(readOnly = true)
    public Optional<Projekt> findeNachSlug(String slug) {
        return repository.findBySlug(slug);
    }

    /**
     * Findet ein Projekt per ID — wird vom MedienController für den
     * Berechtigungs-Check beim Löschen von Projekt-Assets benutzt.
     */
    @Transactional(readOnly = true)
    public Optional<Projekt> findeNachId(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Projekt> findeNachOrg(UUID orgId) {
        return repository.findByOrgIdOrderByCreatedAtDesc(orgId);
    }

    /**
     * Findet alle Projekte aller Organisationen, in denen der User Mitglied ist.
     * Leere Collection → leere Liste, kein Repo-Aufruf (vermeidet eine sinnlose
     * `WHERE id IN ()`-Query, die je nach DB-Dialekt aufwändig ist).
     */
    @Transactional(readOnly = true)
    public List<Projekt> findeNachOrgIds(Collection<UUID> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return List.of();
        }
        return repository.findByOrgIdInOrderByCreatedAtDesc(orgIds);
    }

    @Transactional(readOnly = true)
    public List<Projekt> findeOeffentliche() {
        return repository.findBySichtbarkeitOrderByVeroeffentlichtAmDesc(Sichtbarkeit.OEFFENTLICH);
    }

    /** Die neuesten N öffentlichen Projekte — für die Marktplatz-Preview-Sektion. */
    @Transactional(readOnly = true)
    public List<Projekt> findeNeuesteOeffentliche(int limit) {
        return repository.findBySichtbarkeitOrderByVeroeffentlichtAmDesc(Sichtbarkeit.OEFFENTLICH)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Durchsucht öffentliche Projekte nach einem Suchbegriff.
     * Sucht in: Name, Beschreibung, Kategorie, Ort, Org-Name.
     */
    /**
     * Durchsucht öffentliche Projekte. Postgres nutzt tsvector + GIN-Index
     * mit german-Stemmer; H2 fällt auf JPQL-LIKE zurück. Routing in
     * {@link VolltextSucheService#suchen}.
     */
    @Transactional(readOnly = true)
    public List<Projekt> suche(String suchbegriff) {
        return volltextSuche.suchen(suchbegriff);
    }

    /**
     * Aggregat: zählt veröffentlichte Projekte mehrerer Orgs in einer Query.
     * Leere Collection → 0, kein Repo-Aufruf.
     */
    @Transactional(readOnly = true)
    public long zaehleOeffentlicheNachOrgIds(Collection<UUID> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return 0L;
        }
        return repository.countByOrgIdInAndSichtbarkeit(orgIds, Sichtbarkeit.OEFFENTLICH);
    }

    public Projekt erstelle(Organisation org, String name, String beschreibung) {
        return erstelleAusForm(org, name, beschreibung, null, null, null, null);
    }

    /**
     * Erstellt ein Projekt mit allen Form-Feldern innerhalb der Service-Tx.
     *
     * <p>Vorher wurden Kategorie/Ort/StartDatum/EndDatum vom Controller nach
     * dem {@code service.erstelle(...)}-Call per Setter auf der Entity
     * gesetzt — mit {@code spring.jpa.open-in-view=false} ist die Entity an
     * dem Punkt aber detached, und die Setter sind silent No-Ops. Die
     * Felder gingen ohne Fehler verloren.
     */
    public Projekt erstelleAusForm(Organisation org, String name, String beschreibung,
                                   String kategorie, String ort,
                                   java.time.LocalDate startDatum,
                                   java.time.LocalDate endDatum) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Projektname darf nicht leer sein");
        }

        String slug = slugGenerator.fromName(name);
        if (repository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        Projekt projekt = new Projekt();
        projekt.setOrg(org);
        projekt.setName(name.trim());
        projekt.setSlug(slug);
        projekt.setBeschreibung(beschreibung);
        projekt.setKategorie(kategorie);
        projekt.setOrt(ort);
        projekt.setStartDatum(startDatum);
        projekt.setEndDatum(endDatum);
        projekt.setSichtbarkeit(Sichtbarkeit.ENTWURF);
        return repository.save(projekt);
    }

    public Projekt veroeffentliche(UUID projektId) {
        Projekt projekt = repository.findById(projektId)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektId));

        if (projekt.getSichtbarkeit() == Sichtbarkeit.OEFFENTLICH) {
            throw new IllegalStateException("Projekt ist bereits öffentlich");
        }

        projekt.setSichtbarkeit(Sichtbarkeit.OEFFENTLICH);
        projekt.setVeroeffentlichtAm(Instant.now());
        return repository.save(projekt);
    }

    public void archiviere(UUID projektId) {
        Projekt projekt = repository.findById(projektId)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektId));
        projekt.setSichtbarkeit(Sichtbarkeit.ARCHIVIERT);
        repository.save(projekt);
    }
}


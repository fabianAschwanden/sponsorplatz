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
    private final ch.sponsorplatz.organisation.OrganisationRepository orgRepository;

    public ProjektService(ProjektRepository repository,
                          SlugGenerator slugGenerator,
                          VolltextSucheService volltextSuche,
                          ch.sponsorplatz.organisation.OrganisationRepository orgRepository) {
        this.repository = repository;
        this.slugGenerator = slugGenerator;
        this.volltextSuche = volltextSuche;
        this.orgRepository = orgRepository;
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

    /** Nur öffentliche Projekte einer Org als View — für /vereine/{slug} (ARCH-02). */
    @Transactional(readOnly = true)
    public List<ProjektView> findeOeffentlicheViewsNachOrg(UUID orgId) {
        return findeNachOrg(orgId).stream()
                .filter(p -> p.getSichtbarkeit() == Sichtbarkeit.OEFFENTLICH)
                .map(ProjektView::von)
                .toList();
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

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<ProjektView> findeViewsNachOrgIds(Collection<UUID> orgIds) {
        return findeNachOrgIds(orgIds).stream().map(ProjektView::von).toList();
    }

    /** Slugs aller öffentlichen Projekte — für Sitemap, ohne Entity-Touch (ARCH-02). */
    @Transactional(readOnly = true)
    public List<String> findeOeffentlicheSlugs() {
        return findeOeffentliche().stream().map(Projekt::getSlug).toList();
    }

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<ProjektView> findeOeffentlicheAlsViews() {
        return findeOeffentliche().stream().map(ProjektView::von).toList();
    }

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<ProjektView> sucheAlsViews(String suchbegriff) {
        return suche(suchbegriff).stream().map(ProjektView::von).toList();
    }

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<ProjektView> findeNeuesteOeffentlicheAlsViews(int limit) {
        return findeNeuesteOeffentliche(limit).stream().map(ProjektView::von).toList();
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
    /** Org-ID eines Projekts — für Berechtigungs-Checks ohne Entity-Touch (ARCH-02). */
    @Transactional(readOnly = true)
    public Optional<UUID> findeOrgIdNachProjektId(UUID projektId) {
        return repository.findById(projektId)
                .map(p -> p.getOrg() != null ? p.getOrg().getId() : null);
    }

    /** View-Variante mit Lookup via orgId — Controller braucht keine Entity (ARCH-02). */
    public ProjektView erstelleAusFormAlsView(UUID orgId, String name, String beschreibung,
                                              String kategorie, String ort,
                                              java.time.LocalDate startDatum,
                                              java.time.LocalDate endDatum) {
        Organisation org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Org nicht gefunden: " + orgId));
        return ProjektView.von(erstelleAusForm(org, name, beschreibung, kategorie, ort, startDatum, endDatum));
    }

    /** Veröffentlicht über Slug — gibt den Namen für Flash-Message zurück (ARCH-02). */
    public String veroeffentlicheNachSlug(String slug) {
        Projekt p = findeNachSlug(slug)
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Projekt nicht gefunden: " + slug));
        veroeffentliche(p.getId());
        return p.getName();
    }

    /** View-Variante des Slug-Lookups — wirft NotFoundException (ARCH-02). */
    @Transactional(readOnly = true)
    public ProjektView findeViewNachSlugOderWirf(String slug) {
        return findeNachSlug(slug)
                .map(ProjektView::von)
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Projekt nicht gefunden: " + slug));
    }

    /** Alle Projekte einer Org als View (Org-interne Liste). */
    @Transactional(readOnly = true)
    public List<ProjektView> findeViewsNachOrg(UUID orgId) {
        return findeNachOrg(orgId).stream().map(ProjektView::von).toList();
    }

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


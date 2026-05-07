package ch.sponsorplatz.service;
import ch.sponsorplatz.shared.util.SlugGenerator;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.Sichtbarkeit;
import ch.sponsorplatz.repository.ProjektRepository;
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

    @Transactional(readOnly = true)
    public List<Projekt> findeNachOrg(UUID orgId) {
        return repository.findByOrgIdOrderByCreatedAtDesc(orgId);
    }

    @Transactional(readOnly = true)
    public List<Projekt> findeOeffentliche() {
        return repository.findBySichtbarkeitOrderByVeroeffentlichtAmDesc(Sichtbarkeit.OEFFENTLICH);
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


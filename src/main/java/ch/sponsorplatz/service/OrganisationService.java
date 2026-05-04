package ch.sponsorplatz.service;

import ch.sponsorplatz.dto.OrganisationFormDto;
import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
import ch.sponsorplatz.repository.OrganisationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public OrganisationService(OrganisationRepository repository, SlugGenerator slugGenerator,
                               MitgliedschaftRepository mitgliedschaftRepository) {
        this.repository = repository;
        this.slugGenerator = slugGenerator;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
    }

    @Transactional(readOnly = true)
    public List<Organisation> alle() {
        return repository.findAllByOrderByNameAsc();
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

        org.setTyp(dto.getTyp());
        org.setName(dto.getName().trim());
        org.setSlug(gewuenschterSlug);
        org.setRechtsform(leereAlsNull(dto.getRechtsform()));
        org.setBranche(leereAlsNull(dto.getBranche()));
        org.setBeschreibung(leereAlsNull(dto.getBeschreibung()));
        org.setWebsiteUrl(leereAlsNull(dto.getWebsiteUrl()));
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
        repository.deleteById(id);
    }

    private boolean slugBereitsBelegt(String slug, UUID eigeneId) {
        return repository.findBySlug(slug)
            .filter(existierende -> !existierende.getId().equals(eigeneId))
            .isPresent();
    }

    private String leereAlsNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

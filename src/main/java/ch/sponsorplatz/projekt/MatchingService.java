package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrganisationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service für Matching-Empfehlungen.
 * Findet Projekte die zur Branche der eigenen Organisationen passen.
 */
@Service
@Transactional(readOnly = true)
public class MatchingService {

    private static final int MAX_EMPFEHLUNGEN = 6;

    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final OrganisationRepository organisationRepository;
    private final ProjektRepository projektRepository;

    public MatchingService(MitgliedschaftRepository mitgliedschaftRepository,
                           OrganisationRepository organisationRepository,
                           ProjektRepository projektRepository) {
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.organisationRepository = organisationRepository;
        this.projektRepository = projektRepository;
    }

    /**
     * Findet passende öffentliche Projekte für einen User basierend auf den Branchen
     * seiner Organisationen. Eigene Projekte werden ausgeschlossen.
     *
     * @return max. 6 empfohlene Projekte, neueste zuerst. Leere Liste wenn keine Matches.
     */
    public List<Projekt> findeEmpfehlungen(UUID userId) {
        List<UUID> eigeneOrgIds = mitgliedschaftRepository.findOrgIdsByUserId(userId);
        if (eigeneOrgIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Branchen der eigenen Orgs sammeln
        Set<Branche> eigeneBranchen = eigeneOrgIds.stream()
                .map(organisationRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(Organisation::getBranche)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (eigeneBranchen.isEmpty()) {
            return Collections.emptyList();
        }

        List<Projekt> passende = projektRepository.findePassende(
                eigeneBranchen, eigeneOrgIds, Sichtbarkeit.OEFFENTLICH);

        // Limitieren auf MAX
        return passende.stream().limit(MAX_EMPFEHLUNGEN).toList();
    }
}


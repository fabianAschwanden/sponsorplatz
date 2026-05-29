package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.shared.medien.OrganisationLogoLookup;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Liefert öffentlich sichtbare Engagements — abgeleitet aus {@link SponsoringAnfrage}
 * mit Status {@link AnfrageStatus#ANGENOMMEN}.
 */
@Service
@Transactional(readOnly = true)
public class EngagementService {

    private final SponsoringAnfrageRepository anfrageRepository;
    private final OrganisationRepository orgRepository;
    private final OrganisationLogoLookup logoLookup;

    public EngagementService(SponsoringAnfrageRepository anfrageRepository,
                             OrganisationRepository orgRepository,
                             OrganisationLogoLookup logoLookup) {
        this.anfrageRepository = anfrageRepository;
        this.orgRepository = orgRepository;
        this.logoLookup = logoLookup;
    }

    /**
     * Alle Engagements einer Sponsor-Organisation (öffentlich, für Schaufenster).
     */
    public List<SponsoringAnfrage> findeNachSponsorSlug(String slug) {
        Organisation org = orgRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        return anfrageRepository.findByAnfragenderOrgIdAndStatusOrderByCreatedAtDesc(
                org.getId(), AnfrageStatus.ANGENOMMEN);
    }

    /**
     * Neueste öffentliche Engagements quer über alle Marken — für den
     * Engagement-Teaser auf der anonymen Startseite. Nur ANGENOMMEN-Anfragen.
     */
    public List<EngagementView> findeNeuesteEngagements(int anzahl) {
        return mitLogos(anfrageRepository.findNeuesteNachStatus(
                AnfrageStatus.ANGENOMMEN, PageRequest.of(0, anzahl)));
    }

    /**
     * Mappt Anfragen auf Views und reichert jede mit der Verein-Logo-URL an.
     * Logos werden pro Verein nur einmal nachgeschlagen (Cache), weil ein Verein
     * mehrfach im Portfolio vorkommen kann.
     */
    private List<EngagementView> mitLogos(List<SponsoringAnfrage> anfragen) {
        Map<UUID, String> logoCache = new HashMap<>();
        return anfragen.stream()
                .map(a -> EngagementView.von(a, logoCache.computeIfAbsent(
                        EngagementView.vereinVon(a).getId(),
                        id -> logoLookup.findeLogoUrl(id).orElse(null))))
                .toList();
    }

    /**
     * Aufbereitete Schaufenster-Ansicht einer Marke: Marken-Kopf (Name + Logo),
     * nach Region gruppierte Engagements und die Filter-Optionen. Region- und
     * Branche-Filter wirken kombiniert; die Filter-Optionen kommen stets aus dem
     * ungefilterten Set, bleiben also vollständig.
     */
    public SchaufensterAnsicht findeSchaufenster(String slug, String region, Branche branche) {
        Organisation org = orgRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        List<EngagementView> alle = mitLogos(
                anfrageRepository.findByAnfragenderOrgIdAndStatusOrderByCreatedAtDesc(
                        org.getId(), AnfrageStatus.ANGENOMMEN));
        String logoUrl = logoLookup.findeLogoUrl(org.getId()).orElse(null);
        return SchaufensterAnsicht.erstelle(org.getName(), slug, logoUrl, alle, region, branche);
    }
}

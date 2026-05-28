package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.shared.medien.OrganisationLogoLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * Aufbereitete Schaufenster-Ansicht einer Marke: Marken-Kopf (Name + Logo),
     * nach Region gruppierte Engagements und die Filter-Optionen. Region- und
     * Branche-Filter wirken kombiniert; die Filter-Optionen kommen stets aus dem
     * ungefilterten Set, bleiben also vollständig.
     */
    public SchaufensterAnsicht findeSchaufenster(String slug, String region, Branche branche) {
        Organisation org = orgRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        List<EngagementView> alle = EngagementView.von(
                anfrageRepository.findByAnfragenderOrgIdAndStatusOrderByCreatedAtDesc(
                        org.getId(), AnfrageStatus.ANGENOMMEN));
        String logoUrl = logoLookup.findeLogoUrl(org.getId()).orElse(null);
        return SchaufensterAnsicht.erstelle(org.getName(), slug, logoUrl, alle, region, branche);
    }
}

package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.exception.NotFoundException;
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

    public EngagementService(SponsoringAnfrageRepository anfrageRepository,
                             OrganisationRepository orgRepository) {
        this.anfrageRepository = anfrageRepository;
        this.orgRepository = orgRepository;
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
     * Alle Engagements einer Sponsor-Organisation gefiltert nach Region.
     */
    public List<SponsoringAnfrage> findeNachSponsorSlugUndRegion(String slug, String region) {
        return findeNachSponsorSlug(slug).stream()
                .filter(a -> region.equalsIgnoreCase(a.getPaket().getProjekt().getOrt()))
                .toList();
    }

    /**
     * Alle Engagements einer Sponsor-Organisation gefiltert nach Branche.
     */
    public List<SponsoringAnfrage> findeNachSponsorSlugUndBranche(String slug, Branche branche) {
        return findeNachSponsorSlug(slug).stream()
                .filter(a -> branche == a.getEmpfaengerOrg().getBranche())
                .toList();
    }
}


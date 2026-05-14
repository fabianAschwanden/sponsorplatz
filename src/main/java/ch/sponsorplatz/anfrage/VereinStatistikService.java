package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.projekt.ProjektService;
import ch.sponsorplatz.projekt.SponsoringPaketRepository;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregiert die Vereins-zentrische Statistik für einen User (Phase 5.C,
 * Verein-Seite). Pendant zu {@link SponsorStatistikService}.
 *
 * <p>Sichtbarkeit: alle Rollen (OWNER/EDITOR/VIEWER) der VEREIN-Orgs sehen
 * die Stats — analog zur Sponsor-Variante, weil Vorstand oft VIEWER ist und
 * trotzdem Reporting braucht.
 */
@Service
@Transactional(readOnly = true)
public class VereinStatistikService {

    private static final Set<Rolle> ALLE_ROLLEN = Set.of(
            Rolle.ORG_OWNER, Rolle.ORG_EDITOR, Rolle.ORG_VIEWER);

    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final ProjektService projektService;
    private final SponsoringPaketRepository paketRepository;
    private final VertragRepository vertragRepository;
    private final SponsoringAnfrageRepository anfrageRepository;
    private final RechnungRepository rechnungRepository;

    public VereinStatistikService(AppUserRepository appUserRepository,
                                   MitgliedschaftRepository mitgliedschaftRepository,
                                   ProjektService projektService,
                                   SponsoringPaketRepository paketRepository,
                                   VertragRepository vertragRepository,
                                   SponsoringAnfrageRepository anfrageRepository,
                                   RechnungRepository rechnungRepository) {
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.projektService = projektService;
        this.paketRepository = paketRepository;
        this.vertragRepository = vertragRepository;
        this.anfrageRepository = anfrageRepository;
        this.rechnungRepository = rechnungRepository;
    }

    public VereinStatistik fuerUser(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User nicht gefunden: " + email));

        List<Mitgliedschaft> mitgliedschaften = mitgliedschaftRepository
                .findByUserIdAndRolleInMitOrg(user.getId(), ALLE_ROLLEN);
        List<Organisation> vereinOrgs = mitgliedschaften.stream()
                .map(Mitgliedschaft::getOrg)
                .filter(o -> o.getTyp() == OrgTyp.VEREIN)
                .distinct()
                .toList();
        if (vereinOrgs.isEmpty()) {
            return VereinStatistik.leer();
        }

        List<UUID> orgIds = vereinOrgs.stream().map(Organisation::getId).toList();
        List<String> orgNamen = vereinOrgs.stream().map(Organisation::getName).toList();

        BigDecimal einnahmen = vertragRepository.summePreisChfByOrg(orgIds, VertragsStatus.UNTERZEICHNET);
        if (einnahmen == null) {
            einnahmen = BigDecimal.ZERO;
        }

        return new VereinStatistik(
                projektService.zaehleOeffentlicheNachOrgIds(orgIds),
                paketRepository.zaehleAktiveByOrgIds(orgIds),

                anfrageRepository.countByEmpfaengerOrgIdInAndStatus(orgIds, AnfrageStatus.NEU),
                anfrageRepository.countByEmpfaengerOrgIdInAndStatus(orgIds, AnfrageStatus.ANGENOMMEN),
                anfrageRepository.countByEmpfaengerOrgIdInAndStatus(orgIds, AnfrageStatus.ABGELEHNT),

                anfrageRepository.countByAnfragenderOrgIdInAndStatus(orgIds, AnfrageStatus.NEU),
                anfrageRepository.countByAnfragenderOrgIdInAndStatus(orgIds, AnfrageStatus.ANGENOMMEN),
                anfrageRepository.countByAnfragenderOrgIdInAndStatus(orgIds, AnfrageStatus.ABGELEHNT),

                vertragRepository.countByOrgIdInAndStatus(orgIds, VertragsStatus.ENTWURF),
                vertragRepository.countByOrgIdInAndStatus(orgIds, VertragsStatus.UNTERZEICHNET),
                vertragRepository.countByOrgIdInAndStatus(orgIds, VertragsStatus.GEKUENDIGT),
                einnahmen,

                rechnungRepository.countByOrgIdInAndStatus(orgIds, RechnungsStatus.OFFEN),
                rechnungRepository.countByOrgIdInAndStatus(orgIds, RechnungsStatus.BEZAHLT),
                rechnungRepository.countByOrgIdInAndStatus(orgIds, RechnungsStatus.STORNIERT),

                orgNamen
        );
    }
}

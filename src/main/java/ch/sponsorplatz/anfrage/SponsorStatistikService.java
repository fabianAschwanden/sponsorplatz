package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregiert die Sponsor-zentrische Statistik für einen User (Phase 5.C).
 *
 * <p>Sicht: über <em>alle</em> UNTERNEHMEN-Orgs des Users, unabhängig von der
 * Rolle (ORG_OWNER/EDITOR/VIEWER). Damit dürfen auch reine Lese-Mitglieder
 * die Geschäftszahlen sehen — das ist gewollt, weil Geschäftsleitung typisch
 * VIEWER ist und trotzdem Reporting braucht.
 *
 * <p>Performance: 4 aggregierte Repo-Queries + 1 Branche-Group-By, alle
 * filterbar auf {@code sponsorOrgIds in (...)} statt N-Loop. Bei vielen
 * Orgs/Verträgen skaliert das linear mit den Statūs, nicht mit Vertrags-
 * Anzahl.
 */
@Service
@Transactional(readOnly = true)
public class SponsorStatistikService {

    private static final Set<Rolle> ALLE_ROLLEN = Set.of(
            Rolle.ORG_OWNER, Rolle.ORG_EDITOR, Rolle.ORG_VIEWER);

    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final VertragRepository vertragRepository;
    private final SponsoringAnfrageRepository anfrageRepository;
    private final RechnungRepository rechnungRepository;

    public SponsorStatistikService(AppUserRepository appUserRepository,
                                    MitgliedschaftRepository mitgliedschaftRepository,
                                    VertragRepository vertragRepository,
                                    SponsoringAnfrageRepository anfrageRepository,
                                    RechnungRepository rechnungRepository) {
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.vertragRepository = vertragRepository;
        this.anfrageRepository = anfrageRepository;
        this.rechnungRepository = rechnungRepository;
    }

    /**
     * Liefert die aggregierte Sponsor-Statistik für den User mit der angegebenen
     * E-Mail. Hat der User keine UNTERNEHMEN-Org-Mitgliedschaft, kommt
     * {@link SponsorStatistik#leer()} zurück — der Controller leitet dann
     * auf eine passende Empty-Page.
     */
    public SponsorStatistik fuerUser(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User nicht gefunden: " + email));

        List<Mitgliedschaft> mitgliedschaften = mitgliedschaftRepository
                .findByUserIdAndRolleInMitOrg(user.getId(), ALLE_ROLLEN);
        List<Organisation> sponsorOrgs = mitgliedschaften.stream()
                .map(Mitgliedschaft::getOrg)
                .filter(o -> o.getTyp() == OrgTyp.UNTERNEHMEN)
                .distinct()
                .toList();
        if (sponsorOrgs.isEmpty()) {
            return SponsorStatistik.leer();
        }

        List<UUID> sponsorOrgIds = sponsorOrgs.stream().map(Organisation::getId).toList();
        List<String> sponsorOrgNamen = sponsorOrgs.stream().map(Organisation::getName).toList();

        BigDecimal volumen = vertragRepository.summePreisChf(sponsorOrgIds, VertragsStatus.UNTERZEICHNET);
        if (volumen == null) {
            volumen = BigDecimal.ZERO;
        }

        // Branchen-Verteilung: DB-Group-By → LinkedHashMap (sortiert: häufigste zuerst)
        Map<Branche, Long> proBranche = new LinkedHashMap<>();
        for (Object[] row : vertragRepository.zaehleProBranche(sponsorOrgIds, VertragsStatus.UNTERZEICHNET)) {
            proBranche.put((Branche) row[0], (Long) row[1]);
        }

        return new SponsorStatistik(
                vertragRepository.countBySponsorOrgIdInAndStatus(sponsorOrgIds, VertragsStatus.ENTWURF),
                vertragRepository.countBySponsorOrgIdInAndStatus(sponsorOrgIds, VertragsStatus.UNTERZEICHNET),
                vertragRepository.countBySponsorOrgIdInAndStatus(sponsorOrgIds, VertragsStatus.GEKUENDIGT),
                volumen,

                anfrageRepository.countByAnfragenderOrgIdInAndStatus(sponsorOrgIds, AnfrageStatus.NEU),
                anfrageRepository.countByAnfragenderOrgIdInAndStatus(sponsorOrgIds, AnfrageStatus.ANGENOMMEN),
                anfrageRepository.countByAnfragenderOrgIdInAndStatus(sponsorOrgIds, AnfrageStatus.ABGELEHNT),

                rechnungRepository.zaehleProSponsorOrgUndStatus(sponsorOrgIds, RechnungsStatus.OFFEN),
                rechnungRepository.zaehleProSponsorOrgUndStatus(sponsorOrgIds, RechnungsStatus.BEZAHLT),
                rechnungRepository.zaehleProSponsorOrgUndStatus(sponsorOrgIds, RechnungsStatus.STORNIERT),

                proBranche,
                sponsorOrgNamen
        );
    }
}

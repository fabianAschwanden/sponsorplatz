package ch.sponsorplatz.service;
import ch.sponsorplatz.anfrage.SponsoringAnfrageRepository;
import ch.sponsorplatz.anfrage.NachrichtRepository;
import ch.sponsorplatz.projekt.ProjektRepository;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.benutzer.AppUserRepository;

import ch.sponsorplatz.dto.AdminStatistiken;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service für Admin-Statistiken und -Operationen.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final AppUserRepository userRepository;
    private final OrganisationRepository orgRepository;
    private final ProjektRepository projektRepository;
    private final SponsoringAnfrageRepository anfrageRepository;
    private final NachrichtRepository nachrichtRepository;

    public AdminService(AppUserRepository userRepository,
                        OrganisationRepository orgRepository,
                        ProjektRepository projektRepository,
                        SponsoringAnfrageRepository anfrageRepository,
                        NachrichtRepository nachrichtRepository) {
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
        this.projektRepository = projektRepository;
        this.anfrageRepository = anfrageRepository;
        this.nachrichtRepository = nachrichtRepository;
    }

    public AdminStatistiken ladeStatistiken() {
        long benutzer = userRepository.count();
        long aktiveBenutzer = userRepository.countByAktivTrue();
        long orgs = orgRepository.count();
        long pendingOrgs = orgRepository.findByStatusOrderByCreatedAtAsc(OrgStatus.PENDING).size();
        long verifizierteOrgs = orgRepository.findByStatusOrderByCreatedAtAsc(OrgStatus.VERIFIED).size();
        long projekte = projektRepository.count();
        long anfragen = anfrageRepository.count();
        long nachrichten = nachrichtRepository.count();

        return new AdminStatistiken(
                benutzer, aktiveBenutzer, orgs, pendingOrgs,
                verifizierteOrgs, projekte, anfragen, nachrichten
        );
    }
}


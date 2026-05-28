package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service für die private Sponsor-CRM-Layer (ADR-0011). JEDE Methode prüft als
 * erstes {@link AccessControl#kannSponsorDatenSehen} — es gibt keinen Pfad zu
 * SponsorAccount-Daten ohne diese Schranke. Damit ist die Mandanten-Isolation
 * zentral an einer Stelle durchgesetzt (Controller dürfen das Repository wegen
 * ARCH-01 nicht direkt aufrufen).
 */
@Service
@Transactional
public class SponsorAccountService {

    private final SponsorAccountRepository repository;
    private final OrganisationRepository organisationRepository;
    private final AccessControl accessControl;

    public SponsorAccountService(SponsorAccountRepository repository,
                                 OrganisationRepository organisationRepository,
                                 AccessControl accessControl) {
        this.repository = repository;
        this.organisationRepository = organisationRepository;
        this.accessControl = accessControl;
    }

    /** Portfolio (alle Accounts) einer Sponsor-Org — nur für berechtigte User. */
    @Transactional(readOnly = true)
    public List<SponsorAccountView> findePortfolio(UUID sponsorOrgId, Authentication auth) {
        pruefeZugriff(sponsorOrgId, auth);
        return SponsorAccountView.von(
                repository.findByBesitzerSponsorOrgIdOrderByErstelltAmDesc(sponsorOrgId));
    }

    /**
     * Legt einen Account für einen Verein an. {@link AccountStatus#LEAD} als
     * Start. Dublette (Sponsor↔Verein-Paar existiert schon) → IllegalArgument.
     */
    public SponsorAccountView erstelle(UUID sponsorOrgId, UUID vereinOrgId, Authentication auth) {
        pruefeZugriff(sponsorOrgId, auth);
        if (repository.existsByBesitzerSponsorOrgIdAndVereinId(sponsorOrgId, vereinOrgId)) {
            throw new IllegalArgumentException("Für diesen Verein existiert bereits ein Account");
        }
        Organisation verein = organisationRepository.findById(vereinOrgId)
                .orElseThrow(() -> new NotFoundException("Verein nicht gefunden: " + vereinOrgId));

        SponsorAccount account = new SponsorAccount();
        account.setBesitzerSponsorOrgId(sponsorOrgId);
        account.setVerein(verein);
        account.setStatus(AccountStatus.LEAD);
        return SponsorAccountView.von(repository.save(account));
    }

    private void pruefeZugriff(UUID sponsorOrgId, Authentication auth) {
        if (!accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)) {
            throw new AccessDeniedException(
                    "Kein Zugriff auf die CRM-Daten dieser Sponsor-Organisation");
        }
    }
}

package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.anfrage.AnfrageStatus;
import ch.sponsorplatz.anfrage.RechnungRepository;
import ch.sponsorplatz.anfrage.RechnungsStatus;
import ch.sponsorplatz.anfrage.SponsoringAnfrageRepository;
import ch.sponsorplatz.anfrage.VertragRepository;
import ch.sponsorplatz.anfrage.VertragsStatus;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Beim App-Start einmal alle offenen Trigger-Stati durchlaufen und die Engine
 * triggern — fängt Datensätze ab, die vor V36 angelegt wurden (oder zwischen
 * Engine-Deployment und einem Service-Restart neue Status erreicht haben, ohne
 * dass der Service-Trigger gefeuert hätte). Engine ist idempotent
 * ({@code existsByDefinitionIdAndEntityIdAndStatus}-Guard), daher kein Risiko
 * für Duplikate bei wiederholten Starts.
 */
@Component
public class AufgabenBackfillRunner {

    private static final Logger log = LoggerFactory.getLogger(AufgabenBackfillRunner.class);

    private final OrganisationRepository organisationRepository;
    private final SponsoringAnfrageRepository anfrageRepository;
    private final VertragRepository vertragRepository;
    private final RechnungRepository rechnungRepository;
    private final AufgabenEngine aufgabenEngine;

    public AufgabenBackfillRunner(OrganisationRepository organisationRepository,
                                   SponsoringAnfrageRepository anfrageRepository,
                                   VertragRepository vertragRepository,
                                   RechnungRepository rechnungRepository,
                                   AufgabenEngine aufgabenEngine) {
        this.organisationRepository = organisationRepository;
        this.anfrageRepository = anfrageRepository;
        this.vertragRepository = vertragRepository;
        this.rechnungRepository = rechnungRepository;
        this.aufgabenEngine = aufgabenEngine;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillen() {
        int orgs = 0, anfragen = 0, vertraege = 0, rechnungen = 0;

        for (var org : organisationRepository.findByStatusOrderByCreatedAtAsc(OrgStatus.PENDING)) {
            aufgabenEngine.onOrgStatusWechsel(org);
            orgs++;
        }
        for (var a : anfrageRepository.findByStatus(AnfrageStatus.NEU)) {
            aufgabenEngine.onAnfrageStatusWechsel(a);
            anfragen++;
        }
        for (var v : vertragRepository.findByStatus(VertragsStatus.ENTWURF)) {
            aufgabenEngine.onVertragStatusWechsel(v);
            vertraege++;
        }
        for (var r : rechnungRepository.findByStatus(RechnungsStatus.OFFEN)) {
            aufgabenEngine.onRechnungStatusWechsel(r);
            rechnungen++;
        }

        log.info("AufgabenBackfill durchlaufen: {} PENDING-Orgs, {} NEU-Anfragen, {} ENTWURF-Verträge, {} OFFEN-Rechnungen — Engine-Idempotenz schützt vor Duplikaten",
                orgs, anfragen, vertraege, rechnungen);
    }
}

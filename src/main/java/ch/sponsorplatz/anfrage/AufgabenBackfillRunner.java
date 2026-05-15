package ch.sponsorplatz.anfrage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.sponsorplatz.aufgabe.AufgabenEngine;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrganisationRepository;

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
            aufgabenEngine.onOrgStatusWechsel(new ch.sponsorplatz.organisation.OrgStatusGewechseltEvent(org));
            orgs++;
        }
        for (var a : anfrageRepository.findByStatus(AnfrageStatus.NEU)) {
            aufgabenEngine.onStatusWechsel(ch.sponsorplatz.aufgabe.TriggerEntityTyp.ANFRAGE, a.getId(),
                    a.getStatus().name(), ch.sponsorplatz.aufgabe.AssigneeKontext.ausAnfrageOrgs(a.getEmpfaengerOrg(),
                            a.getAnfragenderOrg()));
            anfragen++;
        }
        for (var v : vertragRepository.findByStatus(VertragsStatus.ENTWURF)) {
            aufgabenEngine.onStatusWechsel(ch.sponsorplatz.aufgabe.TriggerEntityTyp.VERTRAG, v.getId(),
                    v.getStatus().name(),
                    ch.sponsorplatz.aufgabe.AssigneeKontext.ausVertragOrgs(v.getOrg(), v.getSponsorOrg()));
            vertraege++;
        }
        for (var r : rechnungRepository.findByStatus(RechnungsStatus.OFFEN)) {
            aufgabenEngine.onStatusWechsel(ch.sponsorplatz.aufgabe.TriggerEntityTyp.RECHNUNG, r.getId(),
                    r.getStatus().name(), ch.sponsorplatz.aufgabe.AssigneeKontext.ausRechnungOrg(r.getOrg()));
            rechnungen++;
        }

        log.info(
                "AufgabenBackfill durchlaufen: {} PENDING-Orgs, {} NEU-Anfragen, {} ENTWURF-Verträge, {} OFFEN-Rechnungen — Engine-Idempotenz schützt vor Duplikaten",
                orgs, anfragen, vertraege, rechnungen);
    }
}

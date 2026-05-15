package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.organisation.OrgStatusGewechseltEvent;
import ch.sponsorplatz.organisation.Organisation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Reagiert auf Status-Wechsel der überwachten Domain-Aggregate und hält die
 * {@link Aufgabe}-Tabelle synchron — erzeugt neue Aufgaben, wenn aktive
 * {@link AufgabenDefinition}en den neuen Status als Trigger haben, und schließt
 * existierende Aufgaben (ERLEDIGT / ENTFALLEN) wenn die Entity ihren
 * Trigger-Status verlässt.
 *
 * <p>Aufrufer übergeben die volle Entity (nicht nur die ID), damit der
 * {@link AssigneeRegel}-Resolver alle relevanten Org-Referenzen (Verein-Org,
 * Sponsor-Org, Anfragender, Empfänger) zur Hand hat — ohne sie nochmal aus dem
 * Repository nachzuladen.
 *
 * <p>Wer triggert?
 * <ul>
 *   <li>{@code OrganisationService.erstelle*} + {@code verifiziere} + {@code suspendiere}</li>
 *   <li>{@code SponsoringAnfrageService.erstelle} + {@code markiereAngenommen/Abgelehnt}</li>
 *   <li>{@code VertragService.erstelle} + {@code markiereUnterzeichnet} + {@code kuendige}</li>
 *   <li>{@code RechnungService.erstelle} + {@code markiereBezahlt} + {@code stornieren}</li>
 * </ul>
 */
@Service
@Transactional
public class AufgabenEngine {

    private static final Logger log = LoggerFactory.getLogger(AufgabenEngine.class);

    private final AufgabenDefinitionRepository definitionRepository;
    private final AufgabeRepository aufgabeRepository;

    public AufgabenEngine(AufgabenDefinitionRepository definitionRepository,
                          AufgabeRepository aufgabeRepository) {
        this.definitionRepository = definitionRepository;
        this.aufgabeRepository = aufgabeRepository;
    }

    // ── Public Trigger-API ────────────────────────────────────────────────

    /**
     * Listener für Org-Status-Wechsel — Spring Event ersetzt den früheren
     * Direkt-Call aus {@code OrganisationService}, damit ARCH-06 (Feature-Cycles)
     * eingehalten wird.
     */
    @EventListener
    public void onOrgStatusWechsel(OrgStatusGewechseltEvent event) {
        Organisation org = event.org();
        if (org == null || org.getStatus() == null) return;
        handle(TriggerEntityTyp.ORG, org.getId(), org.getStatus().name(), AssigneeKontext.ausOrg(org));
    }

    /**
     * Generischer Trigger-Einstieg für höhere Domains (Anfrage, Vertrag,
     * Rechnung). Die Aufrufer im jeweiligen Service-Paket bauen den
     * {@link AssigneeKontext} aus ihren Entities selbst — so muss
     * {@code aufgabe} keine ihrer Klassen importieren (ARCH-06).
     */
    public void onStatusWechsel(TriggerEntityTyp typ, java.util.UUID entityId,
                                String neuerStatus, AssigneeKontext kontext) {
        if (typ == null || entityId == null || neuerStatus == null) return;
        handle(typ, entityId, neuerStatus, kontext != null ? kontext : new AssigneeKontext(null, null, null, null));
    }

    // ── Engine-Kern ───────────────────────────────────────────────────────

    private void handle(TriggerEntityTyp typ, java.util.UUID entityId,
                        String neuerStatus, AssigneeKontext kontext) {
        schliesseAuslaufendeAufgaben(typ, entityId, neuerStatus);
        erzeugeNeueAufgaben(typ, entityId, neuerStatus, kontext);
    }

    /**
     * Offene Aufgaben für die Entity evaluieren:
     * <ul>
     *   <li>Definition.zielStatus == neuerStatus → ERLEDIGT</li>
     *   <li>Definition.triggerStatus != neuerStatus → ENTFALLEN
     *       (Trigger-Status verlassen, ohne dass das Ziel erreicht wurde)</li>
     *   <li>Sonst (= Status bleibt im Trigger) → Aufgabe bleibt OFFEN</li>
     * </ul>
     */
    private void schliesseAuslaufendeAufgaben(TriggerEntityTyp typ, java.util.UUID entityId,
                                               String neuerStatus) {
        List<Aufgabe> offene = aufgabeRepository.findByEntityTypAndEntityIdAndStatus(
                typ, entityId, AufgabenStatus.OFFEN);
        Instant jetzt = Instant.now();
        for (Aufgabe a : offene) {
            AufgabenDefinition def = a.getDefinition();
            if (neuerStatus.equals(def.getZielStatus())) {
                a.setStatus(AufgabenStatus.ERLEDIGT);
                a.setErledigtAm(jetzt);
            } else if (!neuerStatus.equals(def.getTriggerStatus())) {
                a.setStatus(AufgabenStatus.ENTFALLEN);
                a.setErledigtAm(jetzt);
            }
        }
    }

    private void erzeugeNeueAufgaben(TriggerEntityTyp typ, java.util.UUID entityId,
                                      String neuerStatus, AssigneeKontext kontext) {
        List<AufgabenDefinition> defs = definitionRepository
                .findByAktivTrueAndTriggerEntityTypAndTriggerStatus(typ, neuerStatus);
        for (AufgabenDefinition def : defs) {
            if (aufgabeRepository.existsByDefinitionIdAndEntityIdAndStatus(
                    def.getId(), entityId, AufgabenStatus.OFFEN)) {
                continue; // Idempotenz: doppelter Trigger-Aufruf darf keine Duplikate erzeugen
            }
            Optional<Organisation> assignee = kontext.aufloesen(def.getAssigneeRegel());
            boolean nurAdmin = def.getAssigneeRegel() == AssigneeRegel.PLATFORM_ADMIN;
            if (!nurAdmin && assignee.isEmpty()) {
                log.debug("Aufgabe '{}' für {}#{} übersprungen — Assignee-Regel {} liefert keine Org",
                        def.getTitel(), typ, entityId, def.getAssigneeRegel());
                continue;
            }

            Aufgabe a = new Aufgabe();
            a.setDefinition(def);
            a.setEntityTyp(typ);
            a.setEntityId(entityId);
            a.setTitel(def.getTitel());
            a.setLink(def.getLinkTemplate());
            a.setNurPlatformAdmin(nurAdmin);
            assignee.ifPresent(a::setAssigneeOrg);
            aufgabeRepository.save(a);
        }
    }
}

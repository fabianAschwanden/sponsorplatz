package ch.sponsorplatz.organisation;

import ch.sponsorplatz.shared.util.SlugGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service für hierarchische Organisationsstrukturen.
 *
 * <p>Regeln:
 * <ul>
 *   <li>Max 3 Stufen tief (Konzern → Tochter → Abteilung)</li>
 *   <li>Nur UNTERNEHMEN-Orgs dürfen Sub-Orgs haben</li>
 *   <li>Löschen einer Eltern-Org mit Kindern wird blockiert</li>
 * </ul>
 */
@Service
@Transactional
public class OrgHierarchieService {

    /**
     * Maximale Hierarchie-Tiefe (Konzern → Tochter → Abteilung).
     * Wird auch von {@link AccessControl} für die Eltern-Traversierung
     * referenziert — Single Source of Truth.
     */
    public static final int MAX_TIEFE = 3;

    private final OrganisationRepository repository;
    private final SlugGenerator slugGenerator;

    public OrgHierarchieService(OrganisationRepository repository, SlugGenerator slugGenerator) {
        this.repository = repository;
        this.slugGenerator = slugGenerator;
    }

    /**
     * Erstellt eine Unterorganisation.
     *
     * @throws IllegalArgumentException wenn Eltern-Org nicht UNTERNEHMEN oder Tiefe überschritten
     */
    public Organisation erstelleUnterorganisation(UUID elternOrgId, String name, Branche branche, String beschreibung) {
        Organisation eltern = repository.findById(elternOrgId)
                .orElseThrow(() -> new IllegalArgumentException("Eltern-Organisation nicht gefunden"));

        if (eltern.getTyp() != OrgTyp.UNTERNEHMEN) {
            throw new IllegalArgumentException("Nur Unternehmen können Unterorganisationen haben");
        }

        int aktuelletiefe = berechneTiefe(eltern);
        if (aktuelletiefe >= MAX_TIEFE) {
            throw new IllegalArgumentException(
                    "Maximale Hierarchie-Tiefe (" + MAX_TIEFE + " Stufen) erreicht");
        }

        Organisation sub = new Organisation();
        sub.setName(name.trim());
        sub.setSlug(slugGenerator.findeFreienSlug(name, repository::existsBySlug));
        sub.setTyp(OrgTyp.UNTERNEHMEN);
        sub.setBranche(branche != null ? branche : eltern.getBranche());
        sub.setBeschreibung(beschreibung);
        sub.setStatus(eltern.getStatus()); // erbt Status der Eltern-Org
        sub.setUebergeordneteOrg(eltern);
        return repository.save(sub);
    }

    /**
     * Gibt die Elternkette zurück (von Wurzel bis zur aktuellen Org, inkl. der Org selbst).
     * Für Breadcrumb-Navigation.
     */
    @Transactional(readOnly = true)
    public List<BrotkrumenEintrag> findeElternkette(Organisation org) {
        List<BrotkrumenEintrag> kette = new ArrayList<>();
        Organisation aktuell = org;
        // Defense-in-Depth: Cycle-Schutz für den Fall einer kaputten DB-Inkonsistenz
        // (A.parent=B, B.parent=A) — kann via Service nicht passieren, aber via
        // Direkt-SQL theoretisch. Limit > MAX_TIEFE, damit reguläre Hierarchien
        // garantiert vollständig durchlaufen werden.
        int sicherheit = 0;
        while (aktuell != null && sicherheit < 10) {
            kette.add(new BrotkrumenEintrag(aktuell.getName(), aktuell.getSlug()));
            aktuell = aktuell.getUebergeordneteOrg();
            sicherheit++;
        }
        Collections.reverse(kette);
        return kette;
    }

    /**
     * Gibt die direkten Unterorganisationen zurück.
     */
    @Transactional(readOnly = true)
    public List<Organisation> findeUntergeordnete(UUID orgId) {
        return repository.findByUebergeordneteOrgIdOrderByNameAsc(orgId);
    }

    /**
     * Prüft ob eine Organisation Unterorganisationen hat.
     */
    @Transactional(readOnly = true)
    public boolean hatUnterorganisationen(UUID orgId) {
        return repository.existsByUebergeordneteOrgId(orgId);
    }

    /**
     * Berechnet die aktuelle Tiefe der Org in der Hierarchie (Root = 1).
     */
    int berechneTiefe(Organisation org) {
        int tiefe = 1;
        Organisation aktuell = org;
        // Cycle-Schutz analog zu findeElternkette — Limit liegt absichtlich
        // höher als MAX_TIEFE, damit echte Hierarchien nicht versehentlich
        // beschnitten werden.
        while (aktuell.getUebergeordneteOrg() != null && tiefe < 10) {
            tiefe++;
            aktuell = aktuell.getUebergeordneteOrg();
        }
        return tiefe;
    }

    /**
     * Breadcrumb-Eintrag für die Elternkette.
     */
    public record BrotkrumenEintrag(String name, String slug) {}
}



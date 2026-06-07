package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persönliche Übersicht „Verträge &amp; Rechnungen" über alle Organisationen,
 * in denen der User Edit-Recht hat — eine Seite, beide Listen untereinander,
 * jeweils mit Link zur Detailseite und direktem PDF-/Dokument-Download.
 *
 * <p>Analog zu {@code MeineAnfragenController} (globale Route, aggregiert über
 * die Org-Mitgliedschaften). Sichtbarkeit ist auf Orgs mit Bearbeitungsrolle
 * beschränkt — reine Lese-Mitglieder sehen hier nichts.
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class VertraegeRechnungenController {

    /** Rollen mit Bearbeitungsrecht — nur diese Orgs werden aggregiert. */
    private static final Set<Rolle> EDIT_ROLLEN = Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR);

    private final VertragService vertragService;
    private final RechnungService rechnungService;
    private final AppUserService appUserService;
    private final MitgliedschaftService mitgliedschaftService;

    public VertraegeRechnungenController(VertragService vertragService,
            RechnungService rechnungService,
            AppUserService appUserService,
            MitgliedschaftService mitgliedschaftService) {
        this.vertragService = vertragService;
        this.rechnungService = rechnungService;
        this.appUserService = appUserService;
        this.mitgliedschaftService = mitgliedschaftService;
    }

    @GetMapping("/vertraege-rechnungen")
    public String uebersicht(Authentication auth, Model model) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        List<UUID> editOrgIds = mitgliedschaftService.findeOrgIdsVonUserMitRollen(userId, EDIT_ROLLEN);

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "vertraege-rechnungen");
        model.addAttribute("vertraege", vertragService.findeViewsNachOrgs(editOrgIds));
        model.addAttribute("rechnungen", rechnungService.findeViewsNachOrgs(editOrgIds));
        return "anfrage/vertraege-rechnungen";
    }
}

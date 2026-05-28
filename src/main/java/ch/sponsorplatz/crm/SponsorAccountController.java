package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * UI für die private Sponsor-CRM-Layer (ADR-0011). Alle Routen sind unter
 * {@code /crm/{sponsorSlug}} verankert; der {@link SponsorAccountService}
 * setzt die Mandanten-Isolation durch ({@code kannSponsorDatenSehen}) — der
 * Controller reicht nur den aufgelösten Sponsor-Org-Schlüssel durch.
 *
 * <p>View-DTO-Pflicht: Model bekommt ausschliesslich {@code SponsorAccountView}
 * + {@code OrganisationView}, nie Entities.
 */
@Controller
@RequestMapping("/crm/{sponsorSlug}")
public class SponsorAccountController {

    private final SponsorAccountService accountService;
    private final KontaktPersonService kontaktService;
    private final OrganisationService organisationService;

    public SponsorAccountController(SponsorAccountService accountService,
                                    KontaktPersonService kontaktService,
                                    OrganisationService organisationService) {
        this.accountService = accountService;
        this.kontaktService = kontaktService;
        this.organisationService = organisationService;
    }

    /** Portfolio-Liste der gesponserten Vereine. */
    @GetMapping
    public String portfolio(@PathVariable String sponsorSlug, Authentication auth, Model model) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        // Zugriffs-Schranke ZUERST — wirft AccessDenied bevor irgendwelche
        // Org-Daten (z.B. der Name) geladen werden.
        var accounts = accountService.findePortfolio(sponsorOrgId, auth);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("sponsorSlug", sponsorSlug);
        model.addAttribute("sponsorName", organisationService.findeKopfNachSlug(sponsorSlug).name());
        model.addAttribute("accounts", accounts);
        model.addAttribute("statusWerte", AccountStatus.values());
        model.addAttribute("tierWerte", AccountTier.values());
        return "crm/portfolio";
    }

    /** Formular: neuen Account anlegen (Verein-Picker). */
    @GetMapping("/neu")
    public String neuesFormular(@PathVariable String sponsorSlug, Authentication auth, Model model) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        // Zugriffs-Schranke früh ziehen — findePortfolio wirft AccessDenied falls fremd.
        accountService.findePortfolio(sponsorOrgId, auth);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("sponsorSlug", sponsorSlug);
        model.addAttribute("vereine", organisationService.findeAktiveVereineAlsViews());
        return "crm/account-form";
    }

    /** Account anlegen. */
    @PostMapping
    public String erstelle(@PathVariable String sponsorSlug,
                           @RequestParam UUID vereinOrgId,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        accountService.erstelle(sponsorOrgId, vereinOrgId, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Account angelegt");
        return "redirect:/crm/" + sponsorSlug;
    }

    /** Status / Tier / Notiz eines Accounts aktualisieren. */
    @PostMapping("/{accountId}")
    public String aktualisiere(@PathVariable String sponsorSlug,
                               @PathVariable UUID accountId,
                               @RequestParam AccountStatus status,
                               @RequestParam(required = false) AccountTier tier,
                               @RequestParam(required = false) String notiz,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        accountService.aktualisiere(accountId, status, tier, notiz, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Account aktualisiert");
        return "redirect:/crm/" + sponsorSlug + "/" + accountId;
    }

    /** Account-Detail (Master-Detail): Account-Daten + Kontakte (Dynamics Account↔Contact). */
    @GetMapping("/{accountId}")
    public String accountDetail(@PathVariable String sponsorSlug, @PathVariable UUID accountId,
                                Authentication auth, Model model) {
        // findeAccount zieht die Mandanten-Schranke; findeKontakte ebenfalls.
        model.addAttribute("account", accountService.findeAccount(accountId, auth));
        model.addAttribute("kontakte", kontaktService.findeKontakte(accountId, auth));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("sponsorSlug", sponsorSlug);
        model.addAttribute("statusWerte", AccountStatus.values());
        model.addAttribute("tierWerte", AccountTier.values());
        model.addAttribute("kontaktRollen", KontaktRolle.values());
        return "crm/account-detail";
    }

    /** Kontakt anlegen (Dynamics Contact unter Account). */
    @PostMapping("/{accountId}/kontakte")
    public String kontaktErstellen(@PathVariable String sponsorSlug, @PathVariable UUID accountId,
                                   @RequestParam String vorname,
                                   @RequestParam String nachname,
                                   @RequestParam(required = false) String funktion,
                                   @RequestParam(required = false) KontaktRolle kontaktRolle,
                                   @RequestParam(required = false) String email,
                                   @RequestParam(required = false) String telefon,
                                   @RequestParam(required = false) String mobile,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        kontaktService.erstelle(accountId, vorname, nachname, funktion, kontaktRolle,
                email, telefon, mobile, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Kontakt angelegt");
        return "redirect:/crm/" + sponsorSlug + "/" + accountId;
    }

    /** Kontakt löschen. */
    @PostMapping("/{accountId}/kontakte/{kontaktId}/loeschen")
    public String kontaktLoeschen(@PathVariable String sponsorSlug, @PathVariable UUID accountId,
                                  @PathVariable UUID kontaktId, Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        kontaktService.loesche(kontaktId, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Kontakt entfernt");
        return "redirect:/crm/" + sponsorSlug + "/" + accountId;
    }
}

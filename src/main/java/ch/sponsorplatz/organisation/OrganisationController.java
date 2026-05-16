package ch.sponsorplatz.organisation;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/organisationen")
public class OrganisationController {

    private static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

    private final OrganisationService service;
    private final AccessControl accessControl;
    private final OrgHierarchieService hierarchieService;
    private final AppUserService appUserService;

    public OrganisationController(OrganisationService service, AccessControl accessControl,
                                  OrgHierarchieService hierarchieService,
                                  AppUserService appUserService) {
        this.service = service;
        this.accessControl = accessControl;
        this.hierarchieService = hierarchieService;
        this.appUserService = appUserService;
    }

    /**
     * Org-Liste — gefiltert nach Berechtigung.
     *
     * <ul>
     *   <li><b>Anonyme User:</b> alle Orgs (öffentliche Übersicht).</li>
     *   <li><b>Eingeloggte User:</b> nur Orgs, in denen sie Mitglied sind
     *       (jede Rolle, also auch ORG_VIEWER zählt).</li>
     *   <li><b>Plattform-Admins:</b> alle Orgs (für Admin-Zwecke wie
     *       Verifizierungs-Queue).</li>
     * </ul>
     */
    @GetMapping
    public String liste(Authentication auth, Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("organisationen", ladeListeViews(auth));
        return "organisation/organisationen";
    }

    private List<OrganisationView> ladeListeViews(Authentication auth) {
        boolean eingeloggt = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
        if (!eingeloggt || istPlattformAdmin(auth)) {
            return service.alleViews();
        }
        return appUserService.findeOptionalIdNachEmail(auth.getName())
                .map(service::findeViewsFuerUserMitgliedschaften)
                .orElseGet(List::of);
    }

    private boolean istPlattformAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .anyMatch(ROLE_PLATFORM_ADMIN::equals);
    }

    /**
     * Wizard-Schritt 1: ohne Typ-Param zeigen wir die Typ-Auswahl-Seite.
     * Mit {@code ?typ=VEREIN|UNTERNEHMEN|...} gehen wir direkt zum
     * typ-spezifischen Formular.
     */
    @GetMapping("/neu")
    public String neuesFormular(@RequestParam(required = false) OrgTyp typ, Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        if (typ == null) {
            model.addAttribute("typen", OrgTyp.values());
            return "organisation/organisation-typ-waehlen";
        }
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(typ);
        model.addAttribute("orgForm", dto);
        zeigeFormularModelDaten(model);
        return "organisation/organisation-form";
    }

    /**
     * Create — POST /organisationen. Der eingeloggte User wird automatisch
     * als ORG_OWNER verknüpft, damit er die neue Org sofort bearbeiten und
     * verwalten kann.
     */
    @PostMapping
    public String erstelle(@Valid @ModelAttribute("orgForm") OrganisationFormDto dto,
                           BindingResult br,
                           Authentication auth,
                           Model model,
                           RedirectAttributes redirect) {
        if (br.hasErrors()) {
            return zeigeFormular(model);
        }
        try {
            OrganisationView neu;
            UUID userId = (auth != null && auth.isAuthenticated())
                    ? appUserService.findeOptionalIdNachEmail(auth.getName()).orElse(null)
                    : null;
            if (userId != null) {
                neu = service.erstelleMitEigentuemerAlsView(dto, userId);
            } else {
                neu = service.erstelleAlsView(dto);
            }
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation \"" + neu.name() + "\" erstellt.");
            return "redirect:/organisationen/" + neu.slug();
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return zeigeFormular(model);
        }
    }

    /** Update — POST /organisationen/{slug}, Slug aus URL. AccessControl-Check vorab. */
    @PostMapping("/{slug}")
    public String aktualisiere(@PathVariable String slug,
                               @Valid @ModelAttribute("orgForm") OrganisationFormDto dto,
                               BindingResult br,
                               Authentication auth,
                               Model model,
                               RedirectAttributes redirect) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        if (br.hasErrors()) {
            return zeigeFormular(model);
        }
        try {
            OrganisationView aktualisiert = service.aktualisiereAlsView(slug, dto);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation \"" + aktualisiert.name() + "\" aktualisiert.");
            return "redirect:/organisationen/" + aktualisiert.slug();
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return zeigeFormular(model);
        }
    }

    private String zeigeFormular(Model model) {
        zeigeFormularModelDaten(model);
        return "organisation/organisation-form";
    }

    /** Pure Model-Befüllung — von zeigeFormular und vom Pre-Fill-GET genutzt. */
    private void zeigeFormularModelDaten(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("typen", OrgTyp.values());
        model.addAttribute("branchen", Branche.values());
        model.addAttribute("sponsorBranchen", SponsorBranche.values());
        model.addAttribute("verfuegbareElternOrgs", service.alleViews());
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        OrganisationView org = service.findeViewNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("org", org);
        model.addAttribute("statusOk", org.status() == OrgStatus.ACTIVE || org.status() == OrgStatus.VERIFIED);
        model.addAttribute("untergeordneteOrgs", service.findeUntergeordneteViews(org.id()));
        model.addAttribute("elternkette", hierarchieService.findeElternketteNachSlug(slug));
        return "organisation/organisation-detail";
    }

    @GetMapping("/{slug}/bearbeiten")
    public String bearbeitenFormular(@PathVariable String slug, Authentication auth, Model model) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        model.addAttribute("orgForm", service.findeFormularNachSlug(slug));
        model.addAttribute("bearbeitenSlug", slug);
        zeigeFormularModelDaten(model);
        return "organisation/organisation-form";
    }

    @PostMapping("/{slug}/loeschen")
    public String loesche(@PathVariable String slug, Authentication auth, RedirectAttributes redirect) {
        if (!accessControl.kannOrgVerwaltenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Verwalten-Berechtigung für Org: " + slug);
        }
        String name = service.loescheNachSlug(slug);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
            "Organisation \"" + name + "\" gelöscht.");
        return "redirect:/organisationen";
    }
}

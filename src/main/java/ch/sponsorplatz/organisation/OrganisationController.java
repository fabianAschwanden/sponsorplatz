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

@Controller
@RequestMapping("/organisationen")
public class OrganisationController {

    private static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

    private final OrganisationService service;
    private final AccessControl accessControl;
    private final OrgHierarchieService hierarchieService;
    private final AppUserService appUserService;
    private final MitgliedschaftService mitgliedschaftService;

    public OrganisationController(OrganisationService service, AccessControl accessControl,
                                  OrgHierarchieService hierarchieService,
                                  AppUserService appUserService,
                                  MitgliedschaftService mitgliedschaftService) {
        this.service = service;
        this.accessControl = accessControl;
        this.hierarchieService = hierarchieService;
        this.appUserService = appUserService;
        this.mitgliedschaftService = mitgliedschaftService;
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
        model.addAttribute("organisationen", OrganisationView.von(ladeListe(auth)));
        return "organisationen";
    }

    private java.util.List<Organisation> ladeListe(Authentication auth) {
        boolean eingeloggt = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
        if (!eingeloggt || istPlattformAdmin(auth)) {
            return service.alle();
        }
        return appUserService.findeNachEmail(auth.getName())
                .map(user -> mitgliedschaftService
                        .findeMitgliedschaftenVonUser(
                                user.getId(),
                                java.util.Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR, Rolle.ORG_VIEWER))
                        .stream()
                        .map(Mitgliedschaft::getOrg)
                        // de-duplizieren falls jemand mehrere Mitgliedschaften
                        // (z.B. via Hierarchie) auf derselben Org hätte
                        .distinct()
                        // alphabetisch sortieren wie service.alle()
                        .sorted(java.util.Comparator.comparing(Organisation::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList())
                .orElseGet(java.util.List::of);
    }

    private boolean istPlattformAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .anyMatch(ROLE_PLATFORM_ADMIN::equals);
    }

    /**
     * Wizard-Schritt 1: ohne Typ-Param zeigen wir die Typ-Auswahl-Seite.
     * Mit {@code ?typ=VEREIN|UNTERNEHMEN|...} gehen wir direkt zum
     * typ-spezifischen Formular — das hält die UX konsistent (Verein-Form
     * fragt anders als Sponsor-Form).
     */
    @GetMapping("/neu")
    public String neuesFormular(@RequestParam(required = false) OrgTyp typ, Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        if (typ == null) {
            model.addAttribute("typen", OrgTyp.values());
            return "organisation-typ-waehlen";
        }
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(typ);
        model.addAttribute("orgForm", dto);
        zeigeFormularModelDaten(model);
        return "organisation-form";
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
            Organisation neu;
            if (auth != null && auth.isAuthenticated()) {
                var userId = appUserService.findeNachEmail(auth.getName())
                        .map(ch.sponsorplatz.benutzer.AppUser::getId)
                        .orElse(null);
                if (userId != null) {
                    neu = service.erstelleMitEigentuemer(dto, userId);
                } else {
                    neu = service.erstelle(dto);
                }
            } else {
                neu = service.erstelle(dto);
            }
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation \"" + neu.getName() + "\" erstellt.");
            return "redirect:/organisationen/" + neu.getSlug();
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
            Organisation aktualisiert = service.aktualisiere(slug, dto);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation \"" + aktualisiert.getName() + "\" aktualisiert.");
            return "redirect:/organisationen/" + aktualisiert.getSlug();
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return zeigeFormular(model);
        }
    }

    private String zeigeFormular(Model model) {
        zeigeFormularModelDaten(model);
        return "organisation-form";
    }

    /** Pure Model-Befüllung — von zeigeFormular und vom Pre-Fill-GET genutzt. */
    private void zeigeFormularModelDaten(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("typen", OrgTyp.values());
        model.addAttribute("branchen", Branche.values());
        model.addAttribute("sponsorBranchen", SponsorBranche.values());
        // Mögliche Eltern-Orgs für die Hierarchie-Auswahl. Konzeptionell sind das
        // alle Orgs (UNTERNEHMEN sowie STIFTUNG), nicht VEREINE — Vereine haben
        // keine Sub-Strukturen. Service-Validierung greift zusätzlich.
        model.addAttribute("verfuegbareElternOrgs",
                OrganisationView.von(service.alle()));
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("statusOk", org.getStatus() == OrgStatus.ACTIVE || org.getStatus() == OrgStatus.VERIFIED);
        // Untergeordnete Orgs separat laden — der OneToMany ist LAZY und das
        // Sammeln über die Eltern-Beziehung wäre N+1. Eigene Repo-Query reicht.
        model.addAttribute("untergeordneteOrgs",
                OrganisationView.von(service.findeUntergeordnete(org.getId())));
        // Eltern-Kette von Wurzel bis zur aktuellen Org (inkl. sich selbst) —
        // für die visuelle Hierarchie-Darstellung im Template.
        model.addAttribute("elternkette", hierarchieService.findeElternkette(org));
        return "organisation-detail";
    }

    @GetMapping("/{slug}/bearbeiten")
    public String bearbeitenFormular(@PathVariable String slug, Authentication auth, Model model) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        model.addAttribute("orgForm", inFormDto(org));
        model.addAttribute("bearbeitenSlug", slug);
        zeigeFormularModelDaten(model);
        return "organisation-form";
    }

    @PostMapping("/{slug}/loeschen")
    public String loesche(@PathVariable String slug, Authentication auth, RedirectAttributes redirect) {
        if (!accessControl.kannOrgVerwaltenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Verwalten-Berechtigung für Org: " + slug);
        }
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        String name = org.getName();
        service.loesche(org.getId());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
            "Organisation \"" + name + "\" gelöscht.");
        return "redirect:/organisationen";
    }

    private OrganisationFormDto inFormDto(Organisation org) {
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(org.getTyp());
        dto.setName(org.getName());
        dto.setSlug(org.getSlug());
        dto.setRechtsform(org.getRechtsform());
        dto.setBranche(org.getBranche());
        dto.setSponsorBranche(org.getSponsorBranche());
        dto.setBeschreibung(org.getBeschreibung());
        dto.setWebsiteUrl(org.getWebsiteUrl());
        dto.setIban(org.getIban());
        dto.setStrasse(org.getStrasse());
        dto.setPostleitzahl(org.getPostleitzahl());
        dto.setOrt(org.getOrt());
        if (org.getUebergeordneteOrg() != null) {
            dto.setUebergeordneteOrgId(org.getUebergeordneteOrg().getId());
        }
        return dto;
    }
}

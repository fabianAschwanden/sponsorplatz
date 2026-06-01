package ch.sponsorplatz.projekt;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.shared.util.ListenSeite;
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

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/organisationen/{orgSlug}/projekte")
public class ProjektController {

    private final ProjektService projektService;
    private final SponsoringPaketService paketService;
    private final OrganisationService orgService;
    private final AccessControl accessControl;
    private final MedienAssetService medienAssetService;

    public ProjektController(ProjektService projektService,
                             SponsoringPaketService paketService,
                             OrganisationService orgService,
                             AccessControl accessControl,
                             MedienAssetService medienAssetService) {
        this.projektService = projektService;
        this.paketService = paketService;
        this.orgService = orgService;
        this.accessControl = accessControl;
        this.medienAssetService = medienAssetService;
    }

    @GetMapping
    public String liste(@PathVariable String orgSlug, Authentication auth,
                        @RequestParam(required = false) String suche,
                        @RequestParam(required = false) Sichtbarkeit sichtbarkeit,
                        @RequestParam(required = false) String sort,
                        @RequestParam(required = false, defaultValue = "asc") String dir,
                        @RequestParam(required = false, defaultValue = "1") int seite,
                        @RequestParam(required = false, defaultValue = "25") int groesse,
                        Model model) {
        pruefeEditRecht(orgSlug, auth);
        OrganisationView org = ladeOrgView(orgSlug);
        List<ProjektView> alle = projektService.findeViewsNachOrg(org.id());

        String such = (suche != null && !suche.isBlank()) ? suche.trim().toLowerCase() : null;
        List<ProjektView> gefiltert = alle.stream()
                .filter(p -> sichtbarkeit == null || p.sichtbarkeit() == sichtbarkeit)
                .filter(p -> such == null || passtZurSuche(p, such))
                .toList();
        boolean absteigend = "desc".equalsIgnoreCase(dir);
        List<ProjektView> sortiert = sortiere(gefiltert, sort, absteigend);
        var liste = ListenSeite.von(sortiert, seite, groesse, sort, absteigend);

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("org", org);
        model.addAttribute("projekte", liste.inhalt());
        model.addAttribute("liste", liste);
        model.addAttribute("anzahlGesamt", alle.size());
        model.addAttribute("anzahlGezeigt", gefiltert.size());
        model.addAttribute("filterAktiv", such != null || sichtbarkeit != null);
        model.addAttribute("filterSuche", suche);
        model.addAttribute("filterSichtbarkeit", sichtbarkeit);
        model.addAttribute("sichtbarkeitWerte", Sichtbarkeit.values());
        model.addAttribute("seitenGroessen", ListenSeite.SEITENGROESSEN);
        return "projekt/projekt-liste";
    }

    private static boolean passtZurSuche(ProjektView p, String such) {
        return (p.name() != null && p.name().toLowerCase().contains(such))
                || (p.kategorie() != null && p.kategorie().toLowerCase().contains(such))
                || (p.ort() != null && p.ort().toLowerCase().contains(such));
    }

    private static List<ProjektView> sortiere(List<ProjektView> liste, String sort, boolean absteigend) {
        if (sort == null) return liste;
        Comparator<ProjektView> c = switch (sort) {
            case "name" -> Comparator.comparing(ProjektView::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "sichtbarkeit" -> Comparator.comparing(p -> p.sichtbarkeit() != null ? p.sichtbarkeit().ordinal() : -1);
            case "kategorie" -> Comparator.comparing(ProjektView::kategorie, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "ort" -> Comparator.comparing(ProjektView::ort, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> null;
        };
        if (c == null) return liste;
        return liste.stream().sorted(absteigend ? c.reversed() : c).toList();
    }

    @GetMapping("/neu")
    public String neuesFormular(@PathVariable String orgSlug, Authentication auth, Model model) {
        pruefeEditRecht(orgSlug, auth);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("org", ladeOrgView(orgSlug));
        model.addAttribute("projektForm", new ProjektFormDto());
        return "projekt/projekt-form";
    }

    @PostMapping("/speichern")
    public String speichere(@PathVariable String orgSlug,
                            @Valid @ModelAttribute("projektForm") ProjektFormDto dto,
                            BindingResult br,
                            Authentication auth,
                            Model model,
                            RedirectAttributes redirect) {
        pruefeEditRecht(orgSlug, auth);
        OrganisationView org = ladeOrgView(orgSlug);
        if (br.hasErrors()) {
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
            model.addAttribute("org", org);
            return "projekt/projekt-form";
        }
        ProjektView projekt = projektService.erstelleAusFormAlsView(
                org.id(), dto.getName(), dto.getBeschreibung(),
                dto.getKategorie(), dto.getOrt(),
                dto.getStartDatum(), dto.getEndDatum());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Projekt \"" + projekt.name() + "\" erstellt.");
        return "redirect:/organisationen/" + orgSlug + "/projekte/" + projekt.slug();
    }

    @GetMapping("/{projektSlug}")
    public String detail(@PathVariable String orgSlug,
                         @PathVariable String projektSlug,
                         Authentication auth,
                         Model model) {
        pruefeEditRecht(orgSlug, auth);
        OrganisationView org = ladeOrgView(orgSlug);
        ProjektView projekt = projektService.findeViewNachSlugOderWirf(projektSlug);
        MedienAssetService.BilderUndAnhaenge medien =
                medienAssetService.findeBilderUndAnhaengeViews(EntityTyp.PROJEKT, projekt.id());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("org", org);
        model.addAttribute("projekt", projekt);
        model.addAttribute("pakete", paketService.findeViewsNachProjekt(projekt.id()));
        model.addAttribute("medien", medien.bilder());
        model.addAttribute("anhaenge", medien.anhaenge());
        model.addAttribute("paketForm", new SponsoringPaketFormDto());
        return "projekt/projekt-detail";
    }

    @PostMapping("/{projektSlug}/veroeffentlichen")
    public String veroeffentliche(@PathVariable String orgSlug,
                                  @PathVariable String projektSlug,
                                  Authentication auth,
                                  RedirectAttributes redirect) {
        pruefeEditRecht(orgSlug, auth);
        String projektName = projektService.veroeffentlicheNachSlug(projektSlug);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Projekt \"" + projektName + "\" veröffentlicht.");
        return "redirect:/organisationen/" + orgSlug + "/projekte/" + projektSlug;
    }

    @PostMapping("/{projektSlug}/pakete/speichern")
    public String paketSpeichern(@PathVariable String orgSlug,
                                 @PathVariable String projektSlug,
                                 @Valid @ModelAttribute("paketForm") SponsoringPaketFormDto dto,
                                 BindingResult br,
                                 Authentication auth,
                                 Model model,
                                 RedirectAttributes redirect) {
        pruefeEditRecht(orgSlug, auth);
        if (br.hasErrors()) {
            ProjektView projekt = projektService.findeViewNachSlugOderWirf(projektSlug);
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
            model.addAttribute("org", ladeOrgView(orgSlug));
            model.addAttribute("projekt", projekt);
            model.addAttribute("pakete", paketService.findeViewsNachProjekt(projekt.id()));
            return "projekt/projekt-detail";
        }
        paketService.erstelleNachProjektSlug(projektSlug, dto.getName(), dto.getBeschreibung(), dto.getPreisChf());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Paket \"" + dto.getName() + "\" hinzugefügt.");
        return "redirect:/organisationen/" + orgSlug + "/projekte/" + projektSlug;
    }

    private void pruefeEditRecht(String orgSlug, Authentication auth) {
        if (!accessControl.kannOrgEditierenNachSlug(orgSlug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + orgSlug);
        }
    }

    private OrganisationView ladeOrgView(String slug) {
        return orgService.findeViewNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
    }
}

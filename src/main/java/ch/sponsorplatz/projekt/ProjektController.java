package ch.sponsorplatz.projekt;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.OrganisationService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String liste(@PathVariable String orgSlug, Authentication auth, Model model) {
        pruefeEditRecht(orgSlug, auth);
        OrganisationView org = ladeOrgView(orgSlug);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("org", org);
        model.addAttribute("projekte", projektService.findeViewsNachOrg(org.id()));
        return "projekt-liste";
    }

    @GetMapping("/neu")
    public String neuesFormular(@PathVariable String orgSlug, Authentication auth, Model model) {
        pruefeEditRecht(orgSlug, auth);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("org", ladeOrgView(orgSlug));
        model.addAttribute("projektForm", new ProjektFormDto());
        return "projekt-form";
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
            return "projekt-form";
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
        return "projekt-detail";
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
            return "projekt-detail";
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

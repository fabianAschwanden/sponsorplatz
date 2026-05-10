package ch.sponsorplatz.projekt;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.Organisation;
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
    public String liste(@PathVariable String orgSlug, Authentication auth, Model model) {
        pruefeEditRecht(orgSlug, auth);
        Organisation org = ladeOrg(orgSlug);
        List<Projekt> projekte = projektService.findeNachOrg(org.getId());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("projekte", projekte.stream().map(ProjektView::von).toList());
        return "projekt-liste";
    }

    @GetMapping("/neu")
    public String neuesFormular(@PathVariable String orgSlug, Authentication auth, Model model) {
        pruefeEditRecht(orgSlug, auth);
        Organisation org = ladeOrg(orgSlug);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("org", OrganisationView.von(org));
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
        Organisation org = ladeOrg(orgSlug);
        if (br.hasErrors()) {
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
            model.addAttribute("org", OrganisationView.von(org));
            return "projekt-form";
        }
        Projekt projekt = projektService.erstelleAusForm(
                org, dto.getName(), dto.getBeschreibung(),
                dto.getKategorie(), dto.getOrt(),
                dto.getStartDatum(), dto.getEndDatum());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Projekt \"" + projekt.getName() + "\" erstellt.");
        return "redirect:/organisationen/" + orgSlug + "/projekte/" + projekt.getSlug();
    }

    @GetMapping("/{projektSlug}")
    public String detail(@PathVariable String orgSlug,
                         @PathVariable String projektSlug,
                         Authentication auth,
                         Model model) {
        pruefeEditRecht(orgSlug, auth);
        Organisation org = ladeOrg(orgSlug);
        Projekt projekt = projektService.findeNachSlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));
        List<SponsoringPaket> pakete = paketService.findeNachProjekt(projekt.getId());
        List<MedienAsset> medien = medienAssetService.findeNachEntity(EntityTyp.PROJEKT, projekt.getId());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("projekt", ProjektView.von(projekt));
        model.addAttribute("pakete", pakete.stream().map(SponsoringPaketView::von).toList());
        model.addAttribute("medien", MedienAssetView.von(medien));
        model.addAttribute("paketForm", new SponsoringPaketFormDto());
        return "projekt-detail";
    }

    @PostMapping("/{projektSlug}/veroeffentlichen")
    public String veroeffentliche(@PathVariable String orgSlug,
                                  @PathVariable String projektSlug,
                                  Authentication auth,
                                  RedirectAttributes redirect) {
        pruefeEditRecht(orgSlug, auth);
        Projekt projekt = projektService.findeNachSlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));
        projektService.veroeffentliche(projekt.getId());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Projekt \"" + projekt.getName() + "\" veröffentlicht.");
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
        Projekt projekt = projektService.findeNachSlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));
        if (br.hasErrors()) {
            Organisation org = ladeOrg(orgSlug);
            List<SponsoringPaket> pakete = paketService.findeNachProjekt(projekt.getId());
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
            model.addAttribute("org", OrganisationView.von(org));
            model.addAttribute("projekt", ProjektView.von(projekt));
            model.addAttribute("pakete", pakete.stream().map(SponsoringPaketView::von).toList());
            return "projekt-detail";
        }
        paketService.erstelle(projekt, dto.getName(), dto.getBeschreibung(), dto.getPreisChf());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Paket \"" + dto.getName() + "\" hinzugefügt.");
        return "redirect:/organisationen/" + orgSlug + "/projekte/" + projektSlug;
    }

    private void pruefeEditRecht(String orgSlug, Authentication auth) {
        if (!accessControl.kannOrgEditierenNachSlug(orgSlug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + orgSlug);
        }
    }

    private Organisation ladeOrg(String slug) {
        return orgService.findeNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
    }
}

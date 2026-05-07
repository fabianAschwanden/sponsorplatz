package ch.sponsorplatz.controller;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.dto.ProjektView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.model.EntityTyp;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.service.MedienAssetService;
import ch.sponsorplatz.service.ProjektService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/marktplatz")
public class MarktplatzController {

    private final ProjektService projektService;
    private final MedienAssetService medienAssetService;

    public MarktplatzController(ProjektService projektService, MedienAssetService medienAssetService) {
        this.projektService = projektService;
        this.medienAssetService = medienAssetService;
    }

    @GetMapping
    public String liste(@RequestParam(required = false) String kategorie,
                        @RequestParam(required = false) String ort,
                        @RequestParam(required = false) String q,
                        Model model) {
        List<Projekt> projekte;

        if (q != null && !q.isBlank()) {
            projekte = projektService.suche(q);
        } else {
            projekte = projektService.findeOeffentliche();
        }

        if (kategorie != null && !kategorie.isBlank()) {
            projekte = projekte.stream()
                    .filter(p -> kategorie.equalsIgnoreCase(p.getKategorie()))
                    .toList();
        }
        if (ort != null && !ort.isBlank()) {
            projekte = projekte.stream()
                    .filter(p -> ort.equalsIgnoreCase(p.getOrt()))
                    .toList();
        }

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "marktplatz");
        model.addAttribute("projekte", projekte.stream()
                .map(p -> {
                    String coverUrl = medienAssetService.findeCover(EntityTyp.PROJEKT, p.getId())
                            .map(a -> "/medien/" + a.getId())
                            .orElse(null);
                    return ProjektView.von(p, coverUrl);
                })
                .toList());
        model.addAttribute("filterKategorie", kategorie);
        model.addAttribute("filterOrt", ort);
        model.addAttribute("suchbegriff", q);
        return "marktplatz";
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Projekt projekt = projektService.findeNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + slug));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "marktplatz");
        model.addAttribute("projekt", ProjektView.von(projekt));
        return "marktplatz-detail";
    }
}


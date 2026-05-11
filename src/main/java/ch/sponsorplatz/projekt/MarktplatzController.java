package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/marktplatz")
public class MarktplatzController {

    private final ProjektService projektService;
    private final MedienAssetService medienAssetService;
    private final SponsoringPaketService paketService;

    public MarktplatzController(ProjektService projektService,
                                MedienAssetService medienAssetService,
                                SponsoringPaketService paketService) {
        this.projektService = projektService;
        this.medienAssetService = medienAssetService;
        this.paketService = paketService;
    }

    @GetMapping
    public String liste(@RequestParam(required = false) String kategorie,
                        @RequestParam(required = false) String ort,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) Set<Branche> branche,
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
        if (branche != null && !branche.isEmpty()) {
            projekte = projekte.stream()
                    .filter(p -> p.getOrg() != null && branche.contains(p.getOrg().getBranche()))
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
        model.addAttribute("alleBranchen", Branche.values());
        model.addAttribute("filterBranchen", branche != null ? branche : Set.of());

        // Neueste 3 Projekte als Highlight-Preview (nur auf der ungefilterten Startansicht)
        boolean istGefiltertOderGesucht = (q != null && !q.isBlank())
                || (kategorie != null && !kategorie.isBlank())
                || (ort != null && !ort.isBlank())
                || (branche != null && !branche.isEmpty());
        if (!istGefiltertOderGesucht) {
            List<ProjektView> neueste = projektService.findeNeuesteOeffentliche(3).stream()
                    .map(p -> {
                        String cover = medienAssetService.findeCover(EntityTyp.PROJEKT, p.getId())
                                .map(a -> "/medien/" + a.getId())
                                .orElse(null);
                        return ProjektView.von(p, cover);
                    })
                    .toList();
            model.addAttribute("neueste", neueste);
        } else {
            model.addAttribute("neueste", List.of());
        }

        return "marktplatz";
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Projekt projekt = projektService.findeNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + slug));
        List<SponsoringPaketView> pakete = SponsoringPaketView.von(
                paketService.findeAktiveNachProjekt(projekt.getId()));
        List<MedienAssetView> anhaenge = MedienAssetView.von(
                medienAssetService.findeAnhaenge(EntityTyp.PROJEKT, projekt.getId()));
        List<MedienAssetView> galerie = MedienAssetView.von(
                medienAssetService.findeGalerie(EntityTyp.PROJEKT, projekt.getId()));
        String coverUrl = medienAssetService.findeCover(EntityTyp.PROJEKT, projekt.getId())
                .map(a -> "/medien/" + a.getId())
                .orElse(null);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "marktplatz");
        model.addAttribute("projekt", ProjektView.von(projekt, coverUrl));
        model.addAttribute("pakete", pakete);
        model.addAttribute("anhaenge", anhaenge);
        model.addAttribute("galerie", galerie);
        return "marktplatz-detail";
    }
}


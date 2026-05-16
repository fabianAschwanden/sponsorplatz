package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        List<ProjektView> projekte = (q != null && !q.isBlank())
                ? projektService.sucheAlsViews(q)
                : projektService.findeOeffentlicheAlsViews();

        if (kategorie != null && !kategorie.isBlank()) {
            projekte = projekte.stream()
                    .filter(p -> kategorie.equalsIgnoreCase(p.kategorie()))
                    .toList();
        }
        if (ort != null && !ort.isBlank()) {
            projekte = projekte.stream()
                    .filter(p -> ort.equalsIgnoreCase(p.ort()))
                    .toList();
        }
        if (branche != null && !branche.isEmpty()) {
            projekte = projekte.stream()
                    .filter(p -> p.org() != null && branche.contains(p.org().branche()))
                    .toList();
        }

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "marktplatz");
        model.addAttribute("filterKategorie", kategorie);
        model.addAttribute("filterOrt", ort);
        model.addAttribute("suchbegriff", q);
        model.addAttribute("alleBranchen", Branche.values());
        model.addAttribute("filterBranchen", branche != null ? branche : Set.of());

        // Neueste 3 Projekte als Highlight-Preview (nur auf der ungefilterten
        // Startansicht). Damit die Hauptliste keine Duplikate enthält, werden
        // diese IDs anschließend aus der Hauptliste herausgefiltert.
        boolean istGefiltertOderGesucht = (q != null && !q.isBlank())
                || (kategorie != null && !kategorie.isBlank())
                || (ort != null && !ort.isBlank())
                || (branche != null && !branche.isEmpty());

        Set<UUID> neuesteIds = Set.of();
        if (!istGefiltertOderGesucht) {
            List<ProjektView> neuesteViews = projektService.findeNeuesteOeffentlicheAlsViews(3);
            neuesteIds = neuesteViews.stream()
                    .map(ProjektView::id)
                    .collect(Collectors.toUnmodifiableSet());
            model.addAttribute("neueste", neuesteViews.stream()
                    .map(this::mitCover)
                    .toList());
        } else {
            model.addAttribute("neueste", List.of());
        }

        Set<UUID> idsZumAusblenden = neuesteIds;
        model.addAttribute("projekte", projekte.stream()
                .filter(p -> !idsZumAusblenden.contains(p.id()))
                .map(this::mitCover)
                .toList());

        return "projekt/marktplatz";
    }

    /** Holt das Cover-Asset des Projekts (falls vorhanden) und reichert die ProjektView an. */
    private ProjektView mitCover(ProjektView view) {
        String coverUrl = medienAssetService.findeCoverUrl(EntityTyp.PROJEKT, view.id()).orElse(null);
        return view.mitCoverUrl(coverUrl);
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        ProjektView projekt = projektService.findeViewNachSlugOderWirf(slug);
        List<SponsoringPaketView> pakete = paketService.findeAktiveViewsNachProjekt(projekt.id());
        List<MedienAssetView> anhaenge = medienAssetService.findeAnhaengeViews(EntityTyp.PROJEKT, projekt.id());
        List<MedienAssetView> galerie = medienAssetService.findeGalerieViews(EntityTyp.PROJEKT, projekt.id());
        String coverUrl = medienAssetService.findeCoverUrl(EntityTyp.PROJEKT, projekt.id()).orElse(null);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "marktplatz");
        model.addAttribute("projekt", projekt.mitCoverUrl(coverUrl));
        model.addAttribute("pakete", pakete);
        model.addAttribute("anhaenge", anhaenge);
        model.addAttribute("galerie", galerie);
        return "projekt/marktplatz-detail";
    }
}

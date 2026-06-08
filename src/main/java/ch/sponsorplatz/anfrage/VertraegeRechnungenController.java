package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.util.ListenSeite;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persönliche Übersicht „Verträge &amp; Rechnungen" über alle Organisationen,
 * in denen der User Edit-Recht hat. Eine Seite, zwei Listen — beide nutzen das
 * bewährte Filter-Listen-UI (Such-Toolbar + sortierbare Spaltenköpfe +
 * Paginierung über {@link ListenSeite}), mit getrennten Query-Parametern:
 * Verträge {@code v*} (vSuche/vSort/vDir/vSeite/vGroesse), Rechnungen {@code r*}.
 *
 * <p>Analog zu {@code MeineAnfragenController}/{@code AufgabenController} —
 * globale Route, aggregiert über die Org-Mitgliedschaften mit Bearbeitungsrolle.
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
    public String uebersicht(Authentication auth,
            @RequestParam(required = false) String vSuche,
            @RequestParam(required = false) String vSort,
            @RequestParam(required = false, defaultValue = "desc") String vDir,
            @RequestParam(required = false, defaultValue = "1") int vSeite,
            @RequestParam(required = false, defaultValue = "25") int vGroesse,
            @RequestParam(required = false) String rSuche,
            @RequestParam(required = false) String rSort,
            @RequestParam(required = false, defaultValue = "desc") String rDir,
            @RequestParam(required = false, defaultValue = "1") int rSeite,
            @RequestParam(required = false, defaultValue = "25") int rGroesse,
            Model model) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        List<UUID> editOrgIds = mitgliedschaftService.findeOrgIdsVonUserMitRollen(userId, EDIT_ROLLEN);

        // ---- Verträge ----
        List<VertragView> alleVertraege = vertragService.findeViewsNachOrgs(editOrgIds);
        String vSuch = normalisiere(vSuche);
        List<VertragView> vGefiltert = alleVertraege.stream()
                .filter(v -> vSuch == null || passtVertrag(v, vSuch))
                .toList();
        boolean vAbsteigend = "desc".equalsIgnoreCase(vDir);
        List<VertragView> vSortiert = sortiereVertraege(vGefiltert, vSort, vAbsteigend);
        var vListe = ListenSeite.von(vSortiert, vSeite, vGroesse, vSort, vAbsteigend);

        model.addAttribute("vertraege", vListe.inhalt());
        model.addAttribute("vListe", vListe);
        model.addAttribute("vAnzahlGesamt", alleVertraege.size());
        model.addAttribute("vAnzahlGezeigt", vGefiltert.size());
        model.addAttribute("vFilterAktiv", vSuch != null);
        model.addAttribute("vFilterSuche", vSuche);

        // ---- Rechnungen ----
        List<RechnungView> alleRechnungen = rechnungService.findeViewsNachOrgs(editOrgIds);
        String rSuch = normalisiere(rSuche);
        List<RechnungView> rGefiltert = alleRechnungen.stream()
                .filter(r -> rSuch == null || passtRechnung(r, rSuch))
                .toList();
        boolean rAbsteigend = "desc".equalsIgnoreCase(rDir);
        List<RechnungView> rSortiert = sortiereRechnungen(rGefiltert, rSort, rAbsteigend);
        var rListe = ListenSeite.von(rSortiert, rSeite, rGroesse, rSort, rAbsteigend);

        model.addAttribute("rechnungen", rListe.inhalt());
        model.addAttribute("rListe", rListe);
        model.addAttribute("rAnzahlGesamt", alleRechnungen.size());
        model.addAttribute("rAnzahlGezeigt", rGefiltert.size());
        model.addAttribute("rFilterAktiv", rSuch != null);
        model.addAttribute("rFilterSuche", rSuche);

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "vertraege-rechnungen");
        model.addAttribute("seitenGroessen", ListenSeite.SEITENGROESSEN);
        return "anfrage/vertraege-rechnungen";
    }

    private static String normalisiere(String suche) {
        return (suche != null && !suche.isBlank()) ? suche.trim().toLowerCase() : null;
    }

    private static boolean passtVertrag(VertragView v, String such) {
        return enthaelt(v.sponsorName(), such) || enthaelt(v.sponsorOrgName(), such)
                || enthaelt(v.orgName(), such) || enthaelt(v.paketName(), such);
    }

    private static boolean passtRechnung(RechnungView r, String such) {
        return enthaelt(r.rechnungsnummer(), such) || enthaelt(r.sponsorName(), such)
                || enthaelt(r.orgName(), such);
    }

    private static boolean enthaelt(String feld, String such) {
        return feld != null && feld.toLowerCase().contains(such);
    }

    private static List<VertragView> sortiereVertraege(List<VertragView> liste, String sort, boolean absteigend) {
        // Default (kein Spalten-Sort): aktive Verträge zuerst, innerhalb der
        // Gruppe die neuesten oben (DB liefert bereits erstelltAm DESC → stable
        // sort erhält die Datumsordnung).
        if (sort == null) {
            return liste.stream()
                    .sorted(Comparator.comparing((VertragView v) -> v.status().istAktiv()).reversed())
                    .toList();
        }
        Comparator<VertragView> c = switch (sort) {
            case "partner" -> Comparator.comparing(v -> partnerName(v), nullsLastCi());
            case "paket" -> Comparator.comparing(VertragView::paketName, nullsLastCi());
            case "status" -> Comparator.comparing(v -> v.status().name(), nullsLastCi());
            case "betrag" -> Comparator.comparing(VertragView::preisChf, Comparator.nullsLast(BigDecimal::compareTo));
            case "erstellt" -> Comparator.comparing(VertragView::erstelltAm, Comparator.nullsFirst(Comparator.naturalOrder()));
            default -> null;
        };
        if (c == null) return liste;
        return liste.stream().sorted(absteigend ? c.reversed() : c).toList();
    }

    private static List<RechnungView> sortiereRechnungen(List<RechnungView> liste, String sort, boolean absteigend) {
        // Default (kein Spalten-Sort): offene Rechnungen zuerst, innerhalb der
        // Gruppe die neuesten oben (DB liefert bereits erstelltAm DESC).
        if (sort == null) {
            return liste.stream()
                    .sorted(Comparator.comparing((RechnungView r) -> r.status().istAktiv()).reversed())
                    .toList();
        }
        Comparator<RechnungView> c = switch (sort) {
            case "nummer" -> Comparator.comparing(RechnungView::rechnungsnummer, nullsLastCi());
            case "schuldner" -> Comparator.comparing(RechnungView::sponsorName, nullsLastCi());
            case "status" -> Comparator.comparing(r -> r.status().name(), nullsLastCi());
            case "betrag" -> Comparator.comparing(RechnungView::betragChf, Comparator.nullsLast(BigDecimal::compareTo));
            case "faellig" -> Comparator.comparing(RechnungView::faelligAm, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
        if (c == null) return liste;
        return liste.stream().sorted(absteigend ? c.reversed() : c).toList();
    }

    private static String partnerName(VertragView v) {
        if (v.sponsorName() != null) return v.sponsorName();
        return v.sponsorOrgName() != null ? v.sponsorOrgName() : "";
    }

    private static Comparator<String> nullsLastCi() {
        return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    }
}

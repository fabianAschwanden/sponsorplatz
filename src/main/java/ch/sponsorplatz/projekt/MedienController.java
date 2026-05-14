package ch.sponsorplatz.projekt;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.shared.storage.StorageService;

/**
 * Medien-Auslieferung und Upload-Endpunkte.
 */
@Controller
public class MedienController {

    private final MedienAssetService medienAssetService;
    private final StorageService storageService;
    private final ProjektService projektService;
    private final OrganisationService organisationService;
    private final AccessControl accessControl;
    private final AppUserService appUserService;

    public MedienController(MedienAssetService medienAssetService,
            StorageService storageService,
            ProjektService projektService,
            OrganisationService organisationService,
            AccessControl accessControl,
            AppUserService appUserService) {
        this.medienAssetService = medienAssetService;
        this.storageService = storageService;
        this.projektService = projektService;
        this.organisationService = organisationService;
        this.accessControl = accessControl;
        this.appUserService = appUserService;
    }

    /** Öffentliche Auslieferung eines Medien-Assets. Bilder inline, Dokumente als Download. */
    @GetMapping("/medien/{id}")
    public ResponseEntity<Resource> ausliefern(@PathVariable UUID id) {
        MedienAsset asset = medienAssetService.findeNachId(id)
                .orElseThrow(() -> new NotFoundException("Medien-Asset nicht gefunden"));

        Resource resource = storageService.ladeAlsResource(asset.getStoragePfad());

        // Spring's ContentDisposition.builder() encoded den Filename gemäss
        // RFC 5987 (UTF-8) und filtert Quotes/Newlines — schützt gegen
        // Header-Injection durch user-uploaded Dateinamen.
        boolean istBild = asset.getContentType() != null && asset.getContentType().startsWith("image/");
        ContentDisposition disposition = istBild
                ? ContentDisposition.inline().build()
                : ContentDisposition.attachment().filename(asset.getDateiname()).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, asset.getContentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }

    /** Upload eines Cover-Bildes für ein Projekt. */
    @PostMapping("/organisationen/{orgSlug}/projekte/{projektSlug}/medien")
    public String uploadProjektMedien(@PathVariable String orgSlug,
            @PathVariable String projektSlug,
            @RequestParam("datei") MultipartFile datei,
            @RequestParam(value = "assetTyp", defaultValue = "COVER") String assetTypStr,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        if (!accessControl.kannOrgEditierenNachSlug(orgSlug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung");
        }

        var projekt = projektService.findeNachSlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));

        AssetTyp assetTyp = AssetTyp.valueOf(assetTypStr);
        medienAssetService.speichere(datei, EntityTyp.PROJEKT, projekt.getId(), assetTyp);

        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Bild hochgeladen");
        return "redirect:/organisationen/" + orgSlug + "/projekte/" + projektSlug;
    }

    /** Upload eines Logos/Covers für eine Organisation. */
    @PostMapping("/organisationen/{slug}/medien")
    public String uploadOrgMedien(@PathVariable String slug,
            @RequestParam("datei") MultipartFile datei,
            @RequestParam(value = "assetTyp", defaultValue = "LOGO") String assetTypStr,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung");
        }

        var org = organisationService.findeNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));

        AssetTyp assetTyp = AssetTyp.valueOf(assetTypStr);
        medienAssetService.speichere(datei, EntityTyp.ORGANISATION, org.getId(), assetTyp);

        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Bild hochgeladen");
        return "redirect:/organisationen/" + slug;
    }

    /**
     * Löscht ein Medien-Asset. Prüft Edit-Recht über die assoziierte Org —
     * für ORGANISATION-Assets direkt, für PROJEKT-Assets über die Org des
     * Projekts (IDOR-Schutz: ohne diesen Check könnte jeder über die geratene
     * Asset-UUID fremde Anhänge/Bilder löschen).
     */
    @PostMapping("/medien/{id}/loeschen")
    public String loeschen(@PathVariable UUID id,
            Authentication auth,
            @RequestParam(value = "redirect", defaultValue = "/dashboard") String redirect) {
        MedienAsset asset = medienAssetService.findeNachId(id)
                .orElseThrow(() -> new NotFoundException("Asset nicht gefunden"));

        boolean erlaubt = switch (asset.getEntityTyp()) {
            case ORGANISATION -> organisationService.findeNachId(asset.getEntityId())
                    .map(org -> accessControl.kannOrgEditierenNachSlug(org.getSlug(), auth))
                    .orElse(false);
            case PROJEKT -> projektService.findeNachId(asset.getEntityId())
                    .map(p -> accessControl.kannOrgEditieren(p.getOrg().getId(), auth))
                    .orElse(false);
            // USER-Assets (Profilbild) darf nur der User selbst löschen.
            case USER -> appUserService.findeNachEmail(auth.getName())
                    .map(u -> u.getId().equals(asset.getEntityId()))
                    .orElse(false);
        };
        if (!erlaubt) {
            throw new AccessDeniedException("Keine Berechtigung");
        }

        medienAssetService.loesche(id);
        return "redirect:" + redirect;
    }
}

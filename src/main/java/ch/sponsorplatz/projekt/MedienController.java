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
import ch.sponsorplatz.shared.storage.StorageObjectNotFoundException;
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
        MedienAssetService.AuslieferungsSnapshot snap = medienAssetService.findeAuslieferungsSnapshot(id);

        // Orphaned MedienAsset (DB-Record vorhanden, Storage-Objekt fehlt) →
        // 404 statt 500. Vermeidet Sentry-Flood durch broken-image-Loads.
        Resource resource;
        try {
            resource = storageService.ladeAlsResource(snap.storagePfad());
        } catch (StorageObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        // SVG kann <script> + externe Refs enthalten. Würde es inline
        // ausgeliefert, könnte ein direkter Aufruf von /medien/{id}
        // (Top-Level-Navigation) das SVG als Dokument im Plattform-Origin
        // rendern → Stored XSS, weil die globale CSP 'script-src self
        // unsafe-inline' inline-Scripts erlaubt. Daher SVG IMMER als
        // attachment: '<img src>' rendert es trotzdem (Content-Disposition
        // gilt nicht für Subresource-Loads), ein direkter URL-Aufruf lädt
        // dagegen herunter statt auszuführen.
        boolean istSvg = "image/svg+xml".equals(snap.contentType());
        boolean istInlineBild = snap.contentType() != null
                && snap.contentType().startsWith("image/")
                && !istSvg;

        // Spring's ContentDisposition.builder() encoded den Filename gemäss
        // RFC 5987 (UTF-8) und filtert Quotes/Newlines — schützt gegen
        // Header-Injection durch user-uploaded Dateinamen.
        ContentDisposition disposition = istInlineBild
                ? ContentDisposition.inline().build()
                : ContentDisposition.attachment().filename(snap.dateiname()).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, snap.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                // Kein MIME-Sniffing — Browser respektiert den deklarierten Typ,
                // verhindert dass ein als image/png getarntes HTML als HTML läuft.
                .header("X-Content-Type-Options", "nosniff")
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
        ProjektView projekt = projektService.findeViewNachSlugOderWirf(projektSlug);
        AssetTyp assetTyp = AssetTyp.valueOf(assetTypStr);
        medienAssetService.speichere(datei, EntityTyp.PROJEKT, projekt.id(), assetTyp);

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
        UUID orgId = organisationService.findeIdNachSlug(slug);
        AssetTyp assetTyp = AssetTyp.valueOf(assetTypStr);
        medienAssetService.speichere(datei, EntityTyp.ORGANISATION, orgId, assetTyp);

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
        MedienAssetService.BerechtigungsSnapshot snap = medienAssetService.findeBerechtigungsSnapshot(id);

        boolean erlaubt = switch (snap.entityTyp()) {
            case ORGANISATION -> organisationService.findeSlugNachId(snap.entityId())
                    .map(slug -> accessControl.kannOrgEditierenNachSlug(slug, auth))
                    .orElse(false);
            case PROJEKT -> projektService.findeOrgIdNachProjektId(snap.entityId())
                    .map(orgId -> accessControl.kannOrgEditieren(orgId, auth))
                    .orElse(false);
            case USER -> appUserService.findeOptionalIdNachEmail(auth.getName())
                    .map(uid -> uid.equals(snap.entityId()))
                    .orElse(false);
        };
        if (!erlaubt) {
            throw new AccessDeniedException("Keine Berechtigung");
        }

        medienAssetService.loesche(id);
        return "redirect:" + redirect;
    }
}

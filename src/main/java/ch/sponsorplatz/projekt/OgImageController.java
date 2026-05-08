package ch.sponsorplatz.projekt;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;

/**
 * Liefert Open-Graph-Preview-Bilder (1200×630 PNG) für Social-Media-Shares.
 * Das eigentliche Rendering passiert in {@link OgImageRenderer} — der Controller
 * delegiert nur und legt {@code Cache-Control}-Header.
 *
 * <p>Routen: {@code /og/verein/{slug}.png} und {@code /og/projekt/{slug}.png}.
 */
@Controller
@RequestMapping("/og")
public class OgImageController {

    private final OgImageRenderer renderer;

    public OgImageController(OgImageRenderer renderer) {
        this.renderer = renderer;
    }

    @GetMapping("/verein/{slug}.png")
    public ResponseEntity<byte[]> vereinCard(@PathVariable String slug) {
        return antwortMitCache(renderer.rendereVerein(slug));
    }

    @GetMapping("/projekt/{slug}.png")
    public ResponseEntity<byte[]> projektCard(@PathVariable String slug) {
        return antwortMitCache(renderer.rendereProjekt(slug));
    }

    private ResponseEntity<byte[]> antwortMitCache(byte[] png) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(png);
    }
}

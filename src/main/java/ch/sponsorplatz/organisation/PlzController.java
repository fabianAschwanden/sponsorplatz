package ch.sponsorplatz.organisation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Adress-Auswahlhilfe: schlägt zu einer PLZ Ort + Kanton nach (offizielles
 * {@link PlzVerzeichnis}). Wird vom Adressformular per fetch aufgerufen, um den
 * Ort vorzubefüllen. Öffentliche Daten → {@code /plz/**} ist permitAll.
 */
@RestController
@RequestMapping("/plz")
public class PlzController {

    public record PlzInfo(String ort, String kanton, String kantonName) {}

    @GetMapping("/{plz}")
    public ResponseEntity<PlzInfo> nachschlagen(@PathVariable String plz) {
        Optional<Kanton> kanton = PlzVerzeichnis.kantonVon(plz);
        Optional<String> ort = PlzVerzeichnis.ortVon(plz);
        if (kanton.isEmpty() && ort.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new PlzInfo(
                ort.orElse(null),
                kanton.map(Kanton::name).orElse(null),
                kanton.map(Kanton::getAnzeige).orElse(null)));
    }
}

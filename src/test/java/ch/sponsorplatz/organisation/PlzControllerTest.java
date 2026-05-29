package ch.sponsorplatz.organisation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLZ-CTRL-01..02 — Adress-Auswahlhilfe-Endpoint (PLZ → Ort/Kanton).
 */
class PlzControllerTest {

    private final PlzController controller = new PlzController();

    /** PLZ-CTRL-01: bekannte PLZ → 200 + Ort/Kanton-Code/Kanton-Name. */
    @Test
    @DisplayName("PLZ-CTRL-01: bekannte PLZ → 200 + Ort + Kanton")
    void bekanntePlz() {
        ResponseEntity<PlzController.PlzInfo> antwort = controller.nachschlagen("8001");

        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(antwort.getBody()).isNotNull();
        assertThat(antwort.getBody().ort()).isEqualTo("Zürich");
        assertThat(antwort.getBody().kanton()).isEqualTo("ZH");
        assertThat(antwort.getBody().kantonName()).isEqualTo("Zürich");
    }

    /** PLZ-CTRL-02: unbekannte PLZ → 404. */
    @Test
    @DisplayName("PLZ-CTRL-02: unbekannte PLZ → 404")
    void unbekanntePlz() {
        assertThat(controller.nachschlagen("0000").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

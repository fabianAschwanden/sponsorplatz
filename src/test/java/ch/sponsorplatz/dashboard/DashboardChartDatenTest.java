package ch.sponsorplatz.dashboard;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DASHCHART-06..08 — SVG-Geometrie + Prozent-Rundung im View-DTO (ohne DB/Service).
 */
class DashboardChartDatenTest {

    /** DASHCHART-06: Polyline-Punkte liegen im viewBox, höchster Wert sitzt am weitesten oben. */
    @Test
    void entwicklungSkaliertAufMaximum() {
        var ent = DashboardChartDaten.entwicklungAus(
                List.of(new BigDecimal("1000"), new BigDecimal("4000"), new BigDecimal("2000")),
                List.of("KW1", "KW2", "KW3"));

        assertThat(ent.hatDaten()).isTrue();
        // Drei Punkte „x,y" durch Leerzeichen getrennt.
        String[] punkte = ent.punkte().split(" ");
        assertThat(punkte).hasSize(3);
        // Der zweite (höchste) Punkt muss das kleinste y haben (oben im SVG).
        double y1 = Double.parseDouble(punkte[0].split(",")[1]);
        double y2 = Double.parseDouble(punkte[1].split(",")[1]);
        double y3 = Double.parseDouble(punkte[2].split(",")[1]);
        assertThat(y2).isLessThan(y1).isLessThan(y3);
        assertThat(ent.aktuellerWert()).contains("2'000");
    }

    /** DASHCHART-07: Leere Eingabe → hatDaten=false, keine Punkte. */
    @Test
    void entwicklungLeer() {
        var ent = DashboardChartDaten.entwicklungAus(
                List.of(BigDecimal.ZERO, BigDecimal.ZERO), List.of("a", "b"));
        assertThat(ent.hatDaten()).isFalse();
        assertThat(ent.punkte()).isEmpty();
    }

    /** DASHCHART-08: Donut-Dasharray summiert auf den Kreisumfang, Prozent rundet korrekt. */
    @Test
    void finanzierungDasharrayDecktUmfang() {
        var fin = DashboardChartDaten.finanzierungAus(new BigDecimal("3000"), new BigDecimal("1000"));
        assertThat(fin.prozentGesammelt()).isEqualTo(75);
        assertThat(fin.prozentOffen()).isEqualTo(25);

        String[] arc = fin.dashArray().split(" ");
        double summe = Double.parseDouble(arc[0]) + Double.parseDouble(arc[1]);
        double umfang = 2 * Math.PI * 48;
        assertThat(summe).isCloseTo(umfang, org.assertj.core.data.Offset.offset(0.5));
        // 75% des Umfangs als vorderer Arc.
        assertThat(Double.parseDouble(arc[0])).isCloseTo(umfang * 0.75, org.assertj.core.data.Offset.offset(0.5));
    }
}

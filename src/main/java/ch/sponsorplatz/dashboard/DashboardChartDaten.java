package ch.sponsorplatz.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * View-ready Chart-Daten fürs Dashboard: Sponsoring-Entwicklung (Linien-Chart)
 * + Finanzierungs-Status (Donut). Alle SVG-tauglichen Werte (Polyline-Punkte,
 * Donut-Dasharray) sind hier vorberechnet, damit das Template View-logik-frei
 * bleibt (CLAUDE.md View-DTO-Pflicht).
 *
 * @param entwicklung Linien-Chart über das gewählte Zeitfenster
 * @param finanzierung Donut „gesammelt vs. offen"
 */
public record DashboardChartDaten(Entwicklung entwicklung, Finanzierung finanzierung) {

    /** SVG-Geometrie des Linien-Charts (passt zum bestehenden viewBox 0 0 480 180). */
    private static final double CHART_BREITE = 480;
    private static final double CHART_HOEHE = 180;
    private static final double CHART_PAD_X = 20;
    private static final double CHART_PAD_Y = 25;

    /** Donut-Geometrie (viewBox 0 0 120 120, r=48 → Umfang 2πr). */
    private static final double DONUT_UMFANG = 2 * Math.PI * 48;

    private static final DecimalFormat CHF = new DecimalFormat(
            "#,##0", new DecimalFormatSymbols(Locale.GERMAN) {{ setGroupingSeparator('\''); }});

    /**
     * Sponsoring-Entwicklung: pro Zeit-Bucket die Summe neu unterzeichneter
     * Verträge (CHF). {@code punkte} ist die fertige SVG-Polyline, {@code labels}
     * die X-Achsen-Beschriftung, {@code aktuellerWert} der zuletzt formatierte Wert.
     */
    public record Entwicklung(
            String punkte,
            String letzterPunktX,
            String letzterPunktY,
            List<String> labels,
            String aktuellerWert,
            String aktuellesLabel,
            boolean hatDaten
    ) {}

    /**
     * Finanzierungs-Status: bezahlte vs. offene Rechnungssummen als Donut.
     * {@code dashArray} kodiert „gesammelt offen" für den vorderen Kreis-Arc.
     */
    public record Finanzierung(
            String gesammeltChf,
            String offenChf,
            int prozentGesammelt,
            int prozentOffen,
            String dashArray,
            boolean hatDaten
    ) {}

    /**
     * Baut die Entwicklungskurve aus Bucket-Summen. Skaliert auf das Maximum,
     * verteilt die Punkte gleichmässig über die Chart-Breite. Leere/0-Eingabe →
     * {@code hatDaten=false} (Template zeigt Empty-State).
     */
    public static Entwicklung entwicklungAus(List<BigDecimal> bucketSummen, List<String> labels) {
        boolean hatDaten = bucketSummen.stream().anyMatch(b -> b.signum() > 0);
        if (bucketSummen.isEmpty() || !hatDaten) {
            return new Entwicklung("", "0", "0", labels, CHF.format(0) + " CHF",
                    labels.isEmpty() ? "" : labels.get(labels.size() - 1), false);
        }
        BigDecimal max = bucketSummen.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        if (max.signum() == 0) {
            max = BigDecimal.ONE;
        }
        int n = bucketSummen.size();
        double spanX = CHART_BREITE - 2 * CHART_PAD_X;
        double spanY = CHART_HOEHE - 2 * CHART_PAD_Y;

        StringBuilder punkte = new StringBuilder();
        String letztX = "0";
        String letztY = "0";
        for (int i = 0; i < n; i++) {
            double x = CHART_PAD_X + (n == 1 ? spanX / 2 : spanX * i / (n - 1));
            double anteil = bucketSummen.get(i).doubleValue() / max.doubleValue();
            double y = CHART_PAD_Y + spanY * (1 - anteil); // y invertiert: oben = hoch
            if (i > 0) {
                punkte.append(' ');
            }
            punkte.append(format(x)).append(',').append(format(y));
            letztX = format(x);
            letztY = format(y);
        }
        BigDecimal aktuell = bucketSummen.get(n - 1);
        return new Entwicklung(punkte.toString(), letztX, letztY, labels,
                CHF.format(aktuell) + " CHF",
                labels.isEmpty() ? "" : labels.get(labels.size() - 1), true);
    }

    /**
     * Baut den Finanzierungs-Donut aus bezahlten + offenen Rechnungssummen.
     * Prozent = gesammelt / (gesammelt + offen). Gesamtsumme 0 → {@code hatDaten=false}.
     */
    public static Finanzierung finanzierungAus(BigDecimal gesammelt, BigDecimal offen) {
        BigDecimal sicheresGesammelt = gesammelt == null ? BigDecimal.ZERO : gesammelt;
        BigDecimal sicheresOffen = offen == null ? BigDecimal.ZERO : offen;
        BigDecimal gesamt = sicheresGesammelt.add(sicheresOffen);
        if (gesamt.signum() == 0) {
            return new Finanzierung(CHF.format(0) + " CHF", CHF.format(0) + " CHF",
                    0, 0, "0 " + format(DONUT_UMFANG), false);
        }
        int prozentGesammelt = sicheresGesammelt
                .multiply(BigDecimal.valueOf(100))
                .divide(gesamt, 0, RoundingMode.HALF_UP)
                .intValue();
        int prozentOffen = 100 - prozentGesammelt;
        double arcGesammelt = DONUT_UMFANG * prozentGesammelt / 100.0;
        double arcRest = DONUT_UMFANG - arcGesammelt;
        return new Finanzierung(
                CHF.format(sicheresGesammelt) + " CHF",
                CHF.format(sicheresOffen) + " CHF",
                prozentGesammelt, prozentOffen,
                format(arcGesammelt) + " " + format(arcRest),
                true);
    }

    /** Trimmt Koordinaten auf 1 Nachkommastelle für kompaktes SVG. */
    private static String format(double wert) {
        return BigDecimal.valueOf(wert).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}

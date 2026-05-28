package ch.sponsorplatz.crm;

/**
 * Vertriebs-Pipeline-Stufe eines {@link SponsorAccount} (CRM-Lücke #4). Bildet
 * die Akquise-Reise zu einem Sponsoring-Abschluss ab — orthogonal zu
 * {@link AccountStatus} (der den laufenden Beziehungs-Zustand beschreibt).
 *
 * <p>Jede Stufe trägt eine {@code standardWahrscheinlichkeit} (0–100 %), aus der
 * sich der gewichtete Forecast ergibt ({@code forecastBetrag × Wahrscheinlichkeit}).
 * So lässt sich die Pipeline ohne pro-Account-Eingabe einer Prozentzahl bewerten.
 */
public enum PipelineStage {

    LEAD(10),
    QUALIFIZIERT(30),
    ANGEBOT(60),
    GEWONNEN(100),
    VERLOREN(0);

    private final int standardWahrscheinlichkeit;

    PipelineStage(int standardWahrscheinlichkeit) {
        this.standardWahrscheinlichkeit = standardWahrscheinlichkeit;
    }

    /** Standard-Abschluss-Wahrscheinlichkeit dieser Stufe in Prozent (0–100). */
    public int standardWahrscheinlichkeit() {
        return standardWahrscheinlichkeit;
    }
}

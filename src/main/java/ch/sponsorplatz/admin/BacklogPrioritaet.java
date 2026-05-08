package ch.sponsorplatz.admin;

/**
 * Priorität eines Backlog-Items. Reihenfolge HOCH → MITTEL → NIEDRIG
 * wird via {@code ordinal()} für die Sortierung genutzt.
 */
public enum BacklogPrioritaet {
    HOCH("Hoch"),
    MITTEL("Mittel"),
    NIEDRIG("Niedrig");

    private final String label;

    BacklogPrioritaet(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

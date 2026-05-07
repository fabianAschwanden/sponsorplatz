package ch.sponsorplatz.model;

/**
 * Lebenszyklus eines Backlog-Items.
 *
 * <p>Reihenfolge bewusst — wird in {@code BacklogService.SORT_ORDER}
 * via {@code ordinal()} für die Sortierung verwendet.
 */
public enum BacklogStatus {
    OFFEN("Offen"),
    IN_ARBEIT("In Arbeit"),
    ERLEDIGT("Erledigt"),
    VERWORFEN("Verworfen");

    private final String label;

    BacklogStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean istAbgeschlossen() {
        return this == ERLEDIGT || this == VERWORFEN;
    }
}

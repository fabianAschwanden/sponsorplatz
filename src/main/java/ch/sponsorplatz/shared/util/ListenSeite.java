package ch.sponsorplatz.shared.util;

import java.util.List;

/**
 * Generische, server-seitige Paginierungs-Sicht für Listen-Seiten (Projekt-
 * Konvention: kein SPA). Hält den Seiten-Slice, die Gesamtzahl der (gefilterten)
 * Treffer, Seiten-Metadaten und den echo-zurückgegebenen Sortier-Zustand für die
 * Toolbar/Spaltenköpfe.
 *
 * <p>Jeder Listen-Controller filtert + sortiert seine Liste typspezifisch und
 * wickelt das Ergebnis via {@link #von} ein — die Slice-/Seiten-Mathematik liegt
 * damit an einer Stelle und ist ohne DB testbar.
 */
public record ListenSeite<T>(
        List<T> inhalt,
        int gesamt,
        int seite,
        int seitenGroesse,
        int anzahlSeiten,
        String sort,
        boolean absteigend
) {

    /** Zulässige Seitengrössen für das UI-Dropdown. Server akzeptiert auch andere > 0. */
    public static final List<Integer> SEITENGROESSEN = List.of(25, 50, 100);
    private static final int STANDARD_GROESSE = 25;

    public boolean hatMehrereSeiten() {
        return anzahlSeiten > 1;
    }

    public boolean istLeer() {
        return gesamt == 0;
    }

    /**
     * Paginiert eine bereits gefilterte + sortierte Liste. {@code seite} ist
     * 1-basiert und wird auf den gültigen Bereich geklemmt; ungültige
     * {@code groesse} (≤ 0) fällt auf den Standard zurück.
     */
    public static <T> ListenSeite<T> von(List<T> alle, int seite, int groesse,
                                         String sort, boolean absteigend) {
        int g = groesse > 0 ? groesse : STANDARD_GROESSE;
        int anzahlSeiten = Math.max(1, (int) Math.ceil((double) alle.size() / g));
        int aktuelle = Math.min(Math.max(1, seite), anzahlSeiten);
        int von = (aktuelle - 1) * g;
        int bis = Math.min(von + g, alle.size());
        List<T> slice = von >= alle.size() ? List.of() : alle.subList(von, bis);
        return new ListenSeite<>(slice, alle.size(), aktuelle, g, anzahlSeiten, sort, absteigend);
    }
}

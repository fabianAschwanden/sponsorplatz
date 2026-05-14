package ch.sponsorplatz.aufgabe;

/**
 * Lifecycle einer einzelnen {@link Aufgabe}:
 * <ul>
 *   <li>{@code OFFEN} — wartet auf Bearbeitung durch den Assignee.</li>
 *   <li>{@code ERLEDIGT} — der zugrundeliegende Trigger hat seinen Ziel-Status erreicht
 *       (z.B. Org wurde verifiziert, Anfrage angenommen).</li>
 *   <li>{@code ENTFALLEN} — der Trigger-Status der Entity wurde verlassen, ohne dass
 *       der Ziel-Status erreicht wurde (z.B. Anfrage abgelehnt → Aufgabe „Anfrage
 *       bearbeiten" entfällt, weil der Arbeitsschritt aus anderen Gründen schon
 *       abgehakt ist).</li>
 * </ul>
 */
public enum AufgabenStatus {
    OFFEN,
    ERLEDIGT,
    ENTFALLEN
}

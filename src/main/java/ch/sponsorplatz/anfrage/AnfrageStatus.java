package ch.sponsorplatz.anfrage;

/**
 * Status einer Sponsoring-Anfrage.
 *
 * <p>Workflow: {@code NEU} → terminal {@code ANGENOMMEN} oder {@code ABGELEHNT}.
 * Es gibt bewusst keinen Zwischenschritt — Empfänger entscheiden direkt aus
 * der Anfragen-Übersicht. Frühere Werte {@code IN_PRUEFUNG} und
 * {@code ZURUECKGEZOGEN} wurden mit V29 entfernt.
 */
public enum AnfrageStatus {
    NEU,
    ANGENOMMEN,
    ABGELEHNT
}

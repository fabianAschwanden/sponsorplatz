package ch.sponsorplatz.event;

import ch.sponsorplatz.organisation.Rolle;

/**
 * Domain-Event: eine Einladung wurde erfolgreich in der DB persistiert.
 *
 * <p>Wird vom {@code EinladungsService} publiziert und vom
 * {@code EinladungsMailListener} per {@code @TransactionalEventListener(AFTER_COMMIT)}
 * konsumiert — die Mail wird also erst nach erfolgreichem DB-Commit versendet.</p>
 *
 * <p>Damit kann eine Mail-Failure nicht den DB-State korrumpieren (H4-Fix):</p>
 * <ul>
 *   <li>Tx-Rollback (z.B. Constraint-Violation) → Event wird nicht zugestellt → keine Mail</li>
 *   <li>Mail-Send schlägt fehl → DB-Commit ist schon durch → Einladung existiert,
 *       Listener loggt + schluckt die Exception</li>
 * </ul>
 */
public record EinladungErstelltEvent(
        String token,
        String empfaengerEmail,
        String orgName,
        String eingeladenVonName,
        Rolle rolle
) {}

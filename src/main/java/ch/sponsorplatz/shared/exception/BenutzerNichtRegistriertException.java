package ch.sponsorplatz.shared.exception;

/**
 * Wird geworfen wenn eine Aktion auf einen User-Account angewiesen ist, der
 * noch nicht
 * existiert (z.B. Annahme einer Einladung, der Eingeladene ist aber noch nicht
 * registriert). Der Controller fängt diese und leitet zur Registrierung weiter
 * (M3-Fix: bessere UX als 409).
 */
public class BenutzerNichtRegistriertException extends RuntimeException {

    private final String email;

    public BenutzerNichtRegistriertException(String email) {
        super("Bitte zuerst registrieren mit der E-Mail: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}

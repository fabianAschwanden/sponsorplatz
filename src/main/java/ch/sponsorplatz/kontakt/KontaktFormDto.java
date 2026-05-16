package ch.sponsorplatz.kontakt;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// Bewusst keine @Size(max=0) auf homepage — siehe Kommentar dort.

/**
 * Anonyme Plattform-Kontakt-Anfrage — wird vom {@link KontaktController}
 * entgegengenommen und vom {@link KontaktService} per Mail an die
 * PLATFORM_ADMINs weitergeleitet. Nicht zu verwechseln mit der
 * {@code KontaktAnfrageFormDto} im {@code anfrage}-Paket (Verein → Sponsor).
 */
public class KontaktFormDto {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 200)
    private String betreff;

    @NotBlank
    @Size(max = 4000)
    private String nachricht;

    /**
     * Honeypot — Bots füllen jedes Feld; Menschen sehen das versteckte Feld
     * nicht. Bewusst ohne Bean-Validation: der Controller prüft das Feld
     * VOR der Bindung-Auswertung und liefert Silent-Success, damit Bots
     * keine Fehler-Hints zurückbekommen.
     */
    private String homepage;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBetreff() { return betreff; }
    public void setBetreff(String betreff) { this.betreff = betreff; }
    public String getNachricht() { return nachricht; }
    public void setNachricht(String nachricht) { this.nachricht = nachricht; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
}

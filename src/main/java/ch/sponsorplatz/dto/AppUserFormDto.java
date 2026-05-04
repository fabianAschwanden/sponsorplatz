package ch.sponsorplatz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form-DTO für Benutzer-Registrierung.
 */
public class AppUserFormDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 2, max = 100)
    private String anzeigename;

    @NotBlank
    @Size(min = 8)
    private String passwort;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAnzeigename() { return anzeigename; }
    public void setAnzeigename(String anzeigename) { this.anzeigename = anzeigename; }

    public String getPasswort() { return passwort; }
    public void setPasswort(String passwort) { this.passwort = passwort; }
}


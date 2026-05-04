package ch.sponsorplatz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Form-DTO für eine neue Sponsoring-Anfrage.
 */
public class AnfrageFormDto {

    private UUID paketId;

    @NotBlank(message = "Nachricht ist Pflicht")
    @Size(min = 10, max = 2000, message = "Nachricht muss zwischen 10 und 2000 Zeichen lang sein")
    private String nachricht;

    @Size(max = 255)
    private String kontaktName;

    @Email(message = "Ungültige E-Mail-Adresse")
    @Size(max = 255)
    private String kontaktEmail;

    public UUID getPaketId() { return paketId; }
    public void setPaketId(UUID paketId) { this.paketId = paketId; }

    public String getNachricht() { return nachricht; }
    public void setNachricht(String nachricht) { this.nachricht = nachricht; }

    public String getKontaktName() { return kontaktName; }
    public void setKontaktName(String kontaktName) { this.kontaktName = kontaktName; }

    public String getKontaktEmail() { return kontaktEmail; }
    public void setKontaktEmail(String kontaktEmail) { this.kontaktEmail = kontaktEmail; }
}


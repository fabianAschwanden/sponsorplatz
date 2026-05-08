package ch.sponsorplatz.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form-DTO für Backlog-Item anlegen / bearbeiten.
 */
public class BacklogFormDto {

    @NotBlank(message = "Titel ist Pflichtfeld")
    @Size(max = 200)
    private String titel;

    @Size(max = 5000)
    private String beschreibung;

    private BacklogPrioritaet prioritaet = BacklogPrioritaet.MITTEL;

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public BacklogPrioritaet getPrioritaet() { return prioritaet; }
    public void setPrioritaet(BacklogPrioritaet prioritaet) { this.prioritaet = prioritaet; }
}

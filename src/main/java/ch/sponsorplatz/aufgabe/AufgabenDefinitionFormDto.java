package ch.sponsorplatz.aufgabe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form-DTO für das Admin-UI {@code /admin/aufgaben-definitionen/{id}/bearbeiten}.
 * System-Definitionen (V36-Seeds) erlauben nur Edit der Felder
 * {@code titel}/{@code beschreibung}/{@code linkTemplate}/{@code aktiv} — die
 * Trigger-Felder werden in dem Fall serverseitig wieder auf die ursprünglichen
 * Werte gesetzt.
 */
public class AufgabenDefinitionFormDto {

    @NotBlank
    @Size(max = 200)
    private String titel;

    @Size(max = 1000)
    private String beschreibung;

    @NotNull
    private TriggerEntityTyp triggerEntityTyp;

    @NotBlank
    @Size(max = 40)
    private String triggerStatus;

    @Size(max = 40)
    private String zielStatus;

    @NotNull
    private AssigneeRegel assigneeRegel;

    @Size(max = 200)
    private String linkTemplate;

    private boolean aktiv = true;

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public TriggerEntityTyp getTriggerEntityTyp() { return triggerEntityTyp; }
    public void setTriggerEntityTyp(TriggerEntityTyp triggerEntityTyp) { this.triggerEntityTyp = triggerEntityTyp; }

    public String getTriggerStatus() { return triggerStatus; }
    public void setTriggerStatus(String triggerStatus) { this.triggerStatus = triggerStatus; }

    public String getZielStatus() { return zielStatus; }
    public void setZielStatus(String zielStatus) { this.zielStatus = zielStatus; }

    public AssigneeRegel getAssigneeRegel() { return assigneeRegel; }
    public void setAssigneeRegel(AssigneeRegel assigneeRegel) { this.assigneeRegel = assigneeRegel; }

    public String getLinkTemplate() { return linkTemplate; }
    public void setLinkTemplate(String linkTemplate) { this.linkTemplate = linkTemplate; }

    public boolean isAktiv() { return aktiv; }
    public void setAktiv(boolean aktiv) { this.aktiv = aktiv; }
}

package ch.sponsorplatz.aufgabe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * "Workflow-Vorlage" — beschreibt, wann eine {@link Aufgabe} erzeugt werden soll
 * (Trigger-Entity-Typ + Trigger-Status), wann sie als erledigt gilt (Ziel-Status)
 * und wer sie sieht (Assignee-Regel).
 *
 * <p>Definitionen sind im Admin-UI editierbar. {@code system_definition=true}
 * markiert die fünf mit V36 eingespielten Default-Workflows — diese sind im
 * Admin-UI nicht löschbar, aber deaktivierbar.
 */
@Entity
@Table(name = "aufgaben_definition")
public class AufgabenDefinition {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "titel", nullable = false, length = 200)
    private String titel;

    @Size(max = 1000)
    @Column(name = "beschreibung", length = 1000)
    private String beschreibung;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_entity_typ", nullable = false, length = 30)
    private TriggerEntityTyp triggerEntityTyp;

    @NotBlank
    @Size(max = 40)
    @Column(name = "trigger_status", nullable = false, length = 40)
    private String triggerStatus;

    @Size(max = 40)
    @Column(name = "ziel_status", length = 40)
    private String zielStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_regel", nullable = false, length = 40)
    private AssigneeRegel assigneeRegel;

    @Size(max = 200)
    @Column(name = "link_template", length = 200)
    private String linkTemplate;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "system_definition", nullable = false)
    private boolean systemDefinition = false;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private Instant erstelltAm;

    @Column(name = "erstellt_von", length = 100)
    private String erstelltVon;

    @PrePersist
    private void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (erstelltAm == null) erstelltAm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public boolean isSystemDefinition() { return systemDefinition; }
    public void setSystemDefinition(boolean systemDefinition) { this.systemDefinition = systemDefinition; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public String getErstelltVon() { return erstelltVon; }
    public void setErstelltVon(String erstelltVon) { this.erstelltVon = erstelltVon; }
}

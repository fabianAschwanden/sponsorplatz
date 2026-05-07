package ch.sponsorplatz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Eintrag im internen Feature-Backlog. Schnelle Notiz für Verbesserungs-
 * Ideen, sichtbar/editierbar nur für PLATFORM_ADMIN.
 */
@Entity
@Table(name = "backlog_item")
public class BacklogItem {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotBlank(message = "Titel ist Pflichtfeld")
    @Size(max = 200)
    @Column(name = "titel", nullable = false, length = 200)
    private String titel;

    @Column(name = "beschreibung", columnDefinition = "TEXT")
    private String beschreibung;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BacklogStatus status = BacklogStatus.OFFEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioritaet", nullable = false, length = 10)
    private BacklogPrioritaet prioritaet = BacklogPrioritaet.MITTEL;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private Instant erstelltAm;

    @Column(name = "erstellt_von", length = 100)
    private String erstelltVon;

    @Column(name = "erledigt_am")
    private Instant erledigtAm;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (erstelltAm == null) erstelltAm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public BacklogStatus getStatus() { return status; }
    public void setStatus(BacklogStatus status) { this.status = status; }

    public BacklogPrioritaet getPrioritaet() { return prioritaet; }
    public void setPrioritaet(BacklogPrioritaet prioritaet) { this.prioritaet = prioritaet; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public String getErstelltVon() { return erstelltVon; }
    public void setErstelltVon(String erstelltVon) { this.erstelltVon = erstelltVon; }

    public Instant getErledigtAm() { return erledigtAm; }
    public void setErledigtAm(Instant erledigtAm) { this.erledigtAm = erledigtAm; }
}

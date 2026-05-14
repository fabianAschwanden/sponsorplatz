package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.organisation.Organisation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Instanz einer {@link AufgabenDefinition} für ein konkretes Domain-Aggregat
 * (z.B. eine konkrete Org, Anfrage, Vertrag). Wird von {@link AufgabenEngine}
 * beim Status-Wechsel der Entity erzeugt und auch wieder geschlossen.
 *
 * <p>Sichtbarkeit ergibt sich aus {@link #assigneeOrg} (alle Mitglieder dieser Org
 * sehen die Aufgabe) bzw. {@link #nurPlatformAdmin}=true (nur Plattform-Admins).
 */
@Entity
@Table(name = "aufgabe")
public class Aufgabe {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    private AufgabenDefinition definition;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_typ", nullable = false, length = 30)
    private TriggerEntityTyp entityTyp;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "titel", nullable = false, length = 250)
    private String titel;

    @Column(name = "link", length = 300)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AufgabenStatus status = AufgabenStatus.OFFEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_org_id")
    private Organisation assigneeOrg;

    @Column(name = "nur_platform_admin", nullable = false)
    private boolean nurPlatformAdmin = false;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private Instant erstelltAm;

    @Column(name = "erledigt_am")
    private Instant erledigtAm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "erledigt_von_user_id")
    private AppUser erledigtVon;

    @PrePersist
    private void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (erstelltAm == null) erstelltAm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AufgabenDefinition getDefinition() { return definition; }
    public void setDefinition(AufgabenDefinition definition) { this.definition = definition; }

    public TriggerEntityTyp getEntityTyp() { return entityTyp; }
    public void setEntityTyp(TriggerEntityTyp entityTyp) { this.entityTyp = entityTyp; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public AufgabenStatus getStatus() { return status; }
    public void setStatus(AufgabenStatus status) { this.status = status; }

    public Organisation getAssigneeOrg() { return assigneeOrg; }
    public void setAssigneeOrg(Organisation assigneeOrg) { this.assigneeOrg = assigneeOrg; }

    public boolean isNurPlatformAdmin() { return nurPlatformAdmin; }
    public void setNurPlatformAdmin(boolean nurPlatformAdmin) { this.nurPlatformAdmin = nurPlatformAdmin; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public Instant getErledigtAm() { return erledigtAm; }
    public void setErledigtAm(Instant erledigtAm) { this.erledigtAm = erledigtAm; }

    public AppUser getErledigtVon() { return erledigtVon; }
    public void setErledigtVon(AppUser erledigtVon) { this.erledigtVon = erledigtVon; }
}

package ch.sponsorplatz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Singleton-Row mit Plattform-weiten Einstellungen — aktuell nur SMTP.
 *
 * <p>Wird via Admin-UI ({@code /admin/mail-einstellungen}) editiert. Werte
 * überschreiben ENV-Properties zur Laufzeit (siehe {@code MailService}).
 */
@Entity
@Table(name = "plattform_einstellungen")
public class PlattformEinstellungen {

    /** Hardcoded Singleton-ID — gleiche wie in V15-Migration. */
    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    @Column(name = "id", nullable = false)
    private UUID id = SINGLETON_ID;

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_user")
    private String smtpUser;

    @Column(name = "smtp_password")
    private String smtpPassword;

    @Column(name = "smtp_auth", nullable = false)
    private boolean smtpAuth = true;

    @Column(name = "smtp_starttls", nullable = false)
    private boolean smtpStarttls = true;

    @Column(name = "mail_absender")
    private String mailAbsender;

    @Column(name = "mail_test_empfaenger")
    private String mailTestEmpfaenger;

    @Column(name = "aktualisiert_am", nullable = false)
    private Instant aktualisiertAm = Instant.now();

    @Column(name = "aktualisiert_von")
    private String aktualisiertVon;

    @PrePersist
    @PreUpdate
    void touchTimestamp() {
        this.aktualisiertAm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }

    public String getSmtpUser() { return smtpUser; }
    public void setSmtpUser(String smtpUser) { this.smtpUser = smtpUser; }

    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }

    public boolean isSmtpAuth() { return smtpAuth; }
    public void setSmtpAuth(boolean smtpAuth) { this.smtpAuth = smtpAuth; }

    public boolean isSmtpStarttls() { return smtpStarttls; }
    public void setSmtpStarttls(boolean smtpStarttls) { this.smtpStarttls = smtpStarttls; }

    public String getMailAbsender() { return mailAbsender; }
    public void setMailAbsender(String mailAbsender) { this.mailAbsender = mailAbsender; }

    public String getMailTestEmpfaenger() { return mailTestEmpfaenger; }
    public void setMailTestEmpfaenger(String mailTestEmpfaenger) { this.mailTestEmpfaenger = mailTestEmpfaenger; }

    public Instant getAktualisiertAm() { return aktualisiertAm; }
    public void setAktualisiertAm(Instant aktualisiertAm) { this.aktualisiertAm = aktualisiertAm; }

    public String getAktualisiertVon() { return aktualisiertVon; }
    public void setAktualisiertVon(String aktualisiertVon) { this.aktualisiertVon = aktualisiertVon; }
}

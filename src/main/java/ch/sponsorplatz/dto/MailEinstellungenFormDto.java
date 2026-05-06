package ch.sponsorplatz.dto;

/**
 * Form-DTO für /admin/mail-einstellungen — alle Felder optional, leere Felder
 * fallen auf ENV-Defaults zurück.
 */
public class MailEinstellungenFormDto {

    private String smtpHost;
    private Integer smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private boolean smtpAuth = true;
    private boolean smtpStarttls = true;
    private String mailAbsender;
    private String mailTestEmpfaenger;

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
}

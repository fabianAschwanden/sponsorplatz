package ch.sponsorplatz.admin;

import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungenService.MailKonfigurationsSnapshot;

/**
 * View-DTO für die Mail-Einstellungs-Seite.
 *
 * <p>
 * Stellt sowohl die DB-Werte (zum Editieren) als auch effektive Werte
 * (DB &gt; ENV) dar, plus den Live-Modus-Status (read-only via ENV).
 */
public record MailEinstellungenView(
        String smtpHost,
        Integer smtpPort,
        String smtpUser,
        boolean smtpPasswordGesetzt,
        boolean smtpAuth,
        boolean smtpStarttls,
        String mailAbsender,
        String mailTestEmpfaenger,
        String effektiverHost,
        String effektiverAbsender,
        String effektiverTestEmpfaenger,
        boolean liveModus,
        boolean konfiguriert) {
    /**
     * Baut die View aus dem Service-Snapshot + den effektiven Werten aus dem
     * MailService. Controller bekommt keine Entity (ARCH-02).
     */
    public static MailEinstellungenView von(MailKonfigurationsSnapshot s,
            String effektiverHost,
            String effektiverAbsender,
            String effektiverTestEmpfaenger,
            boolean liveModus,
            boolean konfiguriert) {
        return new MailEinstellungenView(
                s.smtpHost(),
                s.smtpPort(),
                s.smtpUser(),
                s.smtpPasswordGesetzt(),
                s.smtpAuth(),
                s.smtpStarttls(),
                s.mailAbsender(),
                s.mailTestEmpfaenger(),
                effektiverHost,
                effektiverAbsender,
                effektiverTestEmpfaenger,
                liveModus,
                konfiguriert);
    }
}

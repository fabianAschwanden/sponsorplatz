package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.PlattformEinstellungen;

/**
 * View-DTO für die Mail-Einstellungs-Seite.
 *
 * <p>Stellt sowohl die DB-Werte (zum Editieren) als auch effektive Werte
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
        boolean konfiguriert
) {
    /**
     * Baut die View aus DB-Entity + den effektiven Werten aus dem MailService.
     * Passwort wird nur als Boolean (gesetzt/leer) exponiert — Klartext bleibt in der DB.
     */
    public static MailEinstellungenView von(PlattformEinstellungen e,
                                            String effektiverHost,
                                            String effektiverAbsender,
                                            String effektiverTestEmpfaenger,
                                            boolean liveModus,
                                            boolean konfiguriert) {
        return new MailEinstellungenView(
                e.getSmtpHost(),
                e.getSmtpPort(),
                e.getSmtpUser(),
                e.getSmtpPassword() != null && !e.getSmtpPassword().isBlank(),
                e.isSmtpAuth(),
                e.isSmtpStarttls(),
                e.getMailAbsender(),
                e.getMailTestEmpfaenger(),
                effektiverHost,
                effektiverAbsender,
                effektiverTestEmpfaenger,
                liveModus,
                konfiguriert);
    }
}

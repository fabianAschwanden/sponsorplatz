package ch.sponsorplatz.shared.einstellungen;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lese-/Schreib-Zugriff auf die Singleton-Row {@link PlattformEinstellungen}.
 * Die Row wird bei Migration V15 angelegt und niemals gelöscht.
 */
@Service
@Transactional
public class PlattformEinstellungenService {

    private final PlattformEinstellungenRepository repository;

    public PlattformEinstellungenService(PlattformEinstellungenRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PlattformEinstellungen lade() {
        return repository.findById(PlattformEinstellungen.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Singleton-Row für PlattformEinstellungen fehlt — Migration V15 nicht gelaufen?"));
    }

    public PlattformEinstellungen speichere(PlattformEinstellungen einstellungen, String aktualisiertVon) {
        einstellungen.setId(PlattformEinstellungen.SINGLETON_ID);
        einstellungen.setAktualisiertVon(aktualisiertVon);
        return repository.save(einstellungen);
    }

    /**
     * Aktualisiert die SMTP-/Mail-Konfigurationsfelder ohne dass der Aufrufer
     * die {@link PlattformEinstellungen}-Entity selbst anfassen muss (ARCH-02).
     * Leere Strings werden zu {@code null} normalisiert; ein leeres Passwort
     * lässt den bestehenden Wert unverändert (damit die Maske kein Password
     * im Klartext rendern muss).
     */
    public void aktualisiereMailKonfig(String host, Integer port, String user, String password,
                                        boolean smtpAuth, boolean smtpStarttls,
                                        String absender, String testEmpfaenger,
                                        String aktualisiertVon) {
        PlattformEinstellungen e = lade();
        e.setSmtpHost(blankToNull(host));
        e.setSmtpPort(port);
        e.setSmtpUser(blankToNull(user));
        if (password != null && !password.isBlank()) {
            e.setSmtpPassword(password);
        }
        e.setSmtpAuth(smtpAuth);
        e.setSmtpStarttls(smtpStarttls);
        e.setMailAbsender(blankToNull(absender));
        e.setMailTestEmpfaenger(blankToNull(testEmpfaenger));
        speichere(e, aktualisiertVon);
    }

    /**
     * Liest die für die Admin-UI relevanten Mail-Felder als immutable Snapshot,
     * sodass der Controller die Entity nicht selbst aufruft (ARCH-02).
     */
    @Transactional(readOnly = true)
    public MailKonfigurationsSnapshot ladeMailKonfig() {
        PlattformEinstellungen e = lade();
        boolean passwordGesetzt = e.getSmtpPassword() != null && !e.getSmtpPassword().isBlank();
        return new MailKonfigurationsSnapshot(
                e.getSmtpHost(), e.getSmtpPort(), e.getSmtpUser(), passwordGesetzt,
                e.isSmtpAuth(), e.isSmtpStarttls(),
                e.getMailAbsender(), e.getMailTestEmpfaenger());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /**
     * Read-only Snapshot der Mail-Konfiguration für die Admin-UI. Lebt im
     * {@code shared/einstellungen}-Package, weil er die Entity-Felder eins-zu-
     * eins spiegelt — wäre er im {@code admin/}-Package, würde {@code shared/}
     * das nicht importieren dürfen (ARCH-07).
     */
    public record MailKonfigurationsSnapshot(
            String smtpHost, Integer smtpPort, String smtpUser, boolean smtpPasswordGesetzt,
            boolean smtpAuth, boolean smtpStarttls,
            String mailAbsender, String mailTestEmpfaenger) {}
}

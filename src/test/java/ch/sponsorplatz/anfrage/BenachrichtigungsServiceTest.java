package ch.sponsorplatz.anfrage;
import ch.sponsorplatz.shared.mail.MailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BenachrichtigungsServiceTest {

    private MailService mailService;
    private BenachrichtigungsService service;

    @BeforeEach
    void setUp() {
        mailService = mock(MailService.class);
        service = new BenachrichtigungsService(mailService);
    }

    /** BEN-01: Neue Anfrage sendet E-Mail an Kontakt-E-Mail der Anfrage. */
    @Test
    void neueAnfrageSendetMail() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setKontaktEmail("verein@example.com");
        anfrage.setKontaktName("Max Muster");
        anfrage.setNachricht("Interesse am Gold-Paket");

        service.benachrichtigeUeberNeueAnfrage(anfrage, "verein@example.com");

        verify(mailService).sendePlain(eq("verein@example.com"), any(String.class), any(String.class));
    }

    /** BEN-02: Ohne Empfänger-E-Mail wird keine Mail gesendet. */
    @Test
    void ohneEmpfaengerKeineMail() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setNachricht("Interesse");

        service.benachrichtigeUeberNeueAnfrage(anfrage, null);

        verify(mailService, never()).sendePlain(any(), any(), any());
    }

    /** BEN-03: Anfrage angenommen sendet E-Mail an Anfragenden. */
    @Test
    void anfrageAngenommenSendetMail() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setKontaktEmail("sponsor@example.com");
        anfrage.setStatus(AnfrageStatus.ANGENOMMEN);
        anfrage.setAntwort("Gerne, melden Sie sich!");

        service.benachrichtigeUeberAntwort(anfrage, "sponsor@example.com");

        verify(mailService).sendePlain(eq("sponsor@example.com"), any(String.class), any(String.class));
    }
}

package ch.sponsorplatz.service;

import ch.sponsorplatz.model.SponsoringAnfrage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class BenachrichtigungsServiceTest {

    private JavaMailSender mailSender;
    private BenachrichtigungsService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new BenachrichtigungsService(mailSender, "noreply@sponsorplatz.ch");
    }

    /** BEN-01: Neue Anfrage sendet E-Mail an Kontakt-E-Mail der Anfrage. */
    @Test
    void neueAnfrageSendetMail() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setKontaktEmail("verein@example.com");
        anfrage.setKontaktName("Max Muster");
        anfrage.setNachricht("Interesse am Gold-Paket");

        service.benachrichtigeUeberNeueAnfrage(anfrage, "verein@example.com");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    /** BEN-02: Ohne Empfänger-E-Mail wird keine Mail gesendet. */
    @Test
    void ohneMpfaengerKeineMail() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setNachricht("Interesse");

        service.benachrichtigeUeberNeueAnfrage(anfrage, null);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    /** BEN-03: Anfrage angenommen sendet E-Mail an Anfragenden. */
    @Test
    void anfrageAngenommenSendetMail() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setKontaktEmail("sponsor@example.com");
        anfrage.setAntwort("Gerne, melden Sie sich!");

        service.benachrichtigeUeberAntwort(anfrage, "sponsor@example.com");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}


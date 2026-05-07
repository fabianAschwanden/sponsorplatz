package ch.sponsorplatz.service;
import ch.sponsorplatz.shared.mail.MailService;

import ch.sponsorplatz.event.EinladungErstelltEvent;
import ch.sponsorplatz.organisation.Rolle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Tests für den {@code @TransactionalEventListener(AFTER_COMMIT)}-basierten
 * Mail-Versand. H4-Fix: Mail wird erst nach erfolgreichem DB-Commit versendet,
 * eine Mail-Failure korrumpiert nicht den DB-State.
 */
@ExtendWith(MockitoExtension.class)
class EinladungsMailListenerTest {

    @Mock
    private MailService mailService;

    private EinladungsMailListener listener;

    @BeforeEach
    void setUp() {
        listener = new EinladungsMailListener(mailService, "http://localhost:8090");
    }

    /** EINL-12a: Listener sendet Mail beim Event. */
    @Test
    void eventLoestMailAus() {
        EinladungErstelltEvent event = neuerEvent();

        listener.aufEinladungErstellt(event);

        verify(mailService).sendeHtml(eq("neu@example.ch"), any(String.class), any());
    }

    /** EINL-12b: Mail-Failure führt nicht zur Exception nach oben — DB-Commit ist schon durch. */
    @Test
    void mailFailureWirdGeschluckt() {
        doThrow(new MailSendException("SMTP down"))
                .when(mailService).sendeHtml(any(), any(), any());

        EinladungErstelltEvent event = neuerEvent();

        // Wichtig: Listener darf KEINE Exception werfen — sonst rollt Spring nichts mehr
        // zurück (Tx schon committed), und der Aufrufer (Service) bekommt einen Fehler
        // obwohl die Einladung erfolgreich gespeichert wurde.
        assertThatCode(() -> listener.aufEinladungErstellt(event))
                .doesNotThrowAnyException();
    }

    private EinladungErstelltEvent neuerEvent() {
        return new EinladungErstelltEvent(
                "fc-token-1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                "neu@example.ch",
                "FC Beispiel",
                "Admin User",
                Rolle.ORG_EDITOR);
    }
}

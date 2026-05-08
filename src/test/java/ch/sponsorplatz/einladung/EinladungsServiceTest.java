package ch.sponsorplatz.einladung;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.Mitgliedschaft;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.OrganisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EinladungsServiceTest {

    @Mock private EinladungRepository einladungRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private MitgliedschaftService mitgliedschaftService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private EinladungsService service;
    private Organisation testOrg;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        service = new EinladungsService(einladungRepository, appUserRepository,
                organisationRepository, mitgliedschaftService, eventPublisher);

        testOrg = new Organisation();
        testOrg.setId(UUID.randomUUID());
        testOrg.setName("Testverein");
        testOrg.setTyp(OrgTyp.VEREIN);

        testUser = new AppUser();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("admin@example.ch");
        testUser.setAnzeigename("Admin User");
    }

    /** EINL-01: erstelleEinladung generiert Token + speichert Einladung. */
    @Test
    void erstelleEinladungGeneriertTokenUndSpeichert() {
        when(organisationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(appUserRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(einladungRepository.findByOrgIdAndEmail(any(), any())).thenReturn(Optional.empty());
        when(einladungRepository.save(any(Einladung.class))).thenAnswer(inv -> inv.getArgument(0));

        service.erstelleEinladung(testOrg.getId(), "neu@example.ch", Rolle.ORG_EDITOR, testUser.getId());

        ArgumentCaptor<Einladung> captor = ArgumentCaptor.forClass(Einladung.class);
        verify(einladungRepository).save(captor.capture());
        Einladung gespeichert = captor.getValue();
        assertThat(gespeichert.getToken()).isNotBlank().hasSize(64);
        assertThat(gespeichert.getRolle()).isEqualTo(Rolle.ORG_EDITOR);
        assertThat(gespeichert.getEmail()).isEqualTo("neu@example.ch");
    }

    /** EINL-11: erstelleEinladung publishes EinladungErstelltEvent — Mail wird vom Listener nach AFTER_COMMIT versendet. */
    @Test
    void erstelleEinladungPublishesEvent() {
        when(organisationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(appUserRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(einladungRepository.findByOrgIdAndEmail(any(), any())).thenReturn(Optional.empty());
        when(einladungRepository.save(any(Einladung.class))).thenAnswer(inv -> inv.getArgument(0));

        service.erstelleEinladung(testOrg.getId(), "neu@example.ch", Rolle.ORG_EDITOR, testUser.getId());

        ArgumentCaptor<EinladungErstelltEvent> captor =
                ArgumentCaptor.forClass(EinladungErstelltEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        EinladungErstelltEvent event = captor.getValue();
        assertThat(event.empfaengerEmail()).isEqualTo("neu@example.ch");
        assertThat(event.orgName()).isEqualTo("Testverein");
        assertThat(event.eingeladenVonName()).isEqualTo("Admin User");
        assertThat(event.token()).isNotBlank().hasSize(64);
    }

    /** EINL-02: nimmAn erstellt Mitgliedschaft + markiert Einladung als angenommen (statt löschen). */
    @Test
    void nimmAnErstelltMitgliedschaftUndMarkiertAngenommen() {
        Einladung einladung = new Einladung();
        einladung.setOrg(testOrg);
        einladung.setEmail("neu@example.ch");
        einladung.setRolle(Rolle.ORG_EDITOR);
        einladung.setEingeladenVon(testUser);
        einladung.setGueltigBis(Instant.now().plus(1, ChronoUnit.DAYS));

        AppUser neuerUser = new AppUser();
        neuerUser.setId(UUID.randomUUID());
        neuerUser.setEmail("neu@example.ch");

        when(einladungRepository.findByToken("abc123")).thenReturn(Optional.of(einladung));
        when(appUserRepository.findByEmail("neu@example.ch")).thenReturn(Optional.of(neuerUser));

        service.nimmAn("abc123");

        verify(mitgliedschaftService).fuegeHinzu(
                testOrg.getId(), neuerUser.getId(), Rolle.ORG_EDITOR, testUser.getId());
        verify(einladungRepository).save(einladung);
        assertThat(einladung.getAngenommenAm()).isNotNull();
    }

    /** EINL-15: nimmAn ist idempotent — wiederholter Klick auf bereits angenommenen Token → kein doppelter fuegeHinzu. */
    @Test
    void nimmAnIstIdempotent() {
        Einladung schonAngenommen = new Einladung();
        schonAngenommen.setOrg(testOrg);
        schonAngenommen.setEmail("neu@example.ch");
        schonAngenommen.setRolle(Rolle.ORG_EDITOR);
        schonAngenommen.setEingeladenVon(testUser);
        schonAngenommen.setGueltigBis(Instant.now().plus(1, ChronoUnit.DAYS));
        schonAngenommen.setAngenommenAm(Instant.now().minus(1, ChronoUnit.HOURS));

        when(einladungRepository.findByToken("abc123")).thenReturn(Optional.of(schonAngenommen));

        service.nimmAn("abc123"); // darf NICHT werfen

        verifyNoInteractions(mitgliedschaftService);
        verify(einladungRepository, org.mockito.Mockito.never()).save(schonAngenommen);
    }

    /** EINL-03: nimmAn mit abgelaufenem Token → IllegalStateException + KEIN fuegeHinzu. */
    @Test
    void nimmAnMitAbgelaufenemTokenWirft() {
        Einladung einladung = new Einladung();
        einladung.setGueltigBis(Instant.now().minus(1, ChronoUnit.HOURS));

        when(einladungRepository.findByToken("expired")).thenReturn(Optional.of(einladung));

        assertThatThrownBy(() -> service.nimmAn("expired"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("abgelaufen");

        // M6-Fix: Defense gegen Regression — Failure darf NICHT zu Mitgliedschaft führen
        verifyNoInteractions(mitgliedschaftService);
    }

    /** EINL-04: nimmAn mit unbekanntem Token → IllegalArgumentException + KEIN fuegeHinzu. */
    @Test
    void nimmAnMitUnbekanntemTokenWirft() {
        when(einladungRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.nimmAn("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ungültig");

        // M6-Fix: Defense gegen Regression
        verifyNoInteractions(mitgliedschaftService);
    }

    /** EINL-13: erstelleEinladung mit existierender, abgelaufener Einladung → löscht alte + legt neue an. */
    @Test
    void erstelleEinladungLoeschtAbgelaufeneUndLegtNeuAn() {
        Einladung abgelaufen = new Einladung();
        abgelaufen.setOrg(testOrg);
        abgelaufen.setEmail("neu@example.ch");
        abgelaufen.setGueltigBis(Instant.now().minus(1, ChronoUnit.DAYS));

        when(organisationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(appUserRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(einladungRepository.findByOrgIdAndEmail(testOrg.getId(), "neu@example.ch"))
                .thenReturn(Optional.of(abgelaufen));
        when(einladungRepository.save(any(Einladung.class))).thenAnswer(inv -> inv.getArgument(0));

        service.erstelleEinladung(testOrg.getId(), "neu@example.ch", Rolle.ORG_EDITOR, testUser.getId());

        verify(einladungRepository).delete(abgelaufen);
        verify(einladungRepository).save(any(Einladung.class));
    }

    /** EINL-13b: erstelleEinladung mit existierender, gültiger Einladung → blockt. */
    @Test
    void erstelleEinladungBlocktBeiGueltigerExistierender() {
        Einladung gueltig = new Einladung();
        gueltig.setOrg(testOrg);
        gueltig.setEmail("neu@example.ch");
        gueltig.setGueltigBis(Instant.now().plus(3, ChronoUnit.DAYS));

        when(organisationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(appUserRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(einladungRepository.findByOrgIdAndEmail(testOrg.getId(), "neu@example.ch"))
                .thenReturn(Optional.of(gueltig));

        assertThatThrownBy(() -> service.erstelleEinladung(
                testOrg.getId(), "neu@example.ch", Rolle.ORG_EDITOR, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gültige Einladung");
    }

    /** EINL-10: erstelleEinladung mit ungültigem E-Mail-Format → IllegalArgumentException + KEIN Event. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "nicht-email",
            "a@b",
            "@example.ch",
            "kein@punkt",
            "leer @ space.ch",
            "doppelt@@example.ch"
    })
    void erstelleEinladungWirftBeiUngueltigemFormat(String ungueltig) {
        assertThatThrownBy(() ->
                service.erstelleEinladung(testOrg.getId(), ungueltig, Rolle.ORG_VIEWER, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-Mail");

        // Defense: bei Validation-Failure kein Event publizieren
        verifyNoInteractions(eventPublisher);
    }
}

package ch.sponsorplatz.anfrage;
import ch.sponsorplatz.benachrichtigung.NotificationService;
import ch.sponsorplatz.benachrichtigung.Benachrichtigung;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.SponsoringPaket;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;

class SponsoringAnfrageServiceTest {

    private SponsoringAnfrageRepository repository;
    private BenachrichtigungsService benachrichtigungsService;
    private NotificationService notificationService;
    private MitgliedschaftRepository mitgliedschaftRepository;
    private SponsoringAnfrageService service;

    @BeforeEach
    void setUp() {
        repository = mock(SponsoringAnfrageRepository.class);
        benachrichtigungsService = mock(BenachrichtigungsService.class);
        notificationService = mock(NotificationService.class);
        mitgliedschaftRepository = mock(MitgliedschaftRepository.class);
        service = new SponsoringAnfrageService(repository, benachrichtigungsService,
                notificationService, mitgliedschaftRepository);
        when(mitgliedschaftRepository.findByOrgId(any())).thenReturn(Collections.emptyList());
    }

    /** ANF-01: Anfrage erstellen setzt Status NEU. */
    @Test
    void erstelleSetztStatusNeu() {
        SponsoringPaket paket = new SponsoringPaket();
        paket.setId(UUID.randomUUID());
        Organisation anfragender = new Organisation();
        anfragender.setId(UUID.randomUUID());
        Organisation empfaenger = new Organisation();
        empfaenger.setId(UUID.randomUUID());

        when(repository.save(any(SponsoringAnfrage.class))).thenAnswer(inv -> inv.getArgument(0));

        SponsoringAnfrage anfrage = service.erstelle(paket, anfragender, empfaenger, "Interesse am Gold-Paket",
                "Max Muster", "max@example.com");

        assertThat(anfrage.getStatus()).isEqualTo(AnfrageStatus.NEU);
        assertThat(anfrage.getNachricht()).isEqualTo("Interesse am Gold-Paket");
        assertThat(anfrage.getKontaktName()).isEqualTo("Max Muster");
        assertThat(anfrage.getPaket()).isEqualTo(paket);
    }

    /** ANF-02: Erstellen ohne Nachricht wirft. */
    @Test
    void erstelleOhneNachrichtWirft() {
        SponsoringPaket paket = new SponsoringPaket();
        Organisation anfragender = new Organisation();
        Organisation empfaenger = new Organisation();

        assertThatThrownBy(() -> service.erstelle(paket, anfragender, empfaenger, "", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** ANF-03: Annehmen setzt Status und Zeitstempel. */
    @Test
    void annehmenSetztStatus() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setId(UUID.randomUUID());
        anfrage.setStatus(AnfrageStatus.NEU);
        Organisation anfOrg = new Organisation();
        anfOrg.setId(UUID.randomUUID());
        anfOrg.setSlug("sponsor-ag");
        anfrage.setAnfragenderOrg(anfOrg);
        when(repository.findById(anfrage.getId())).thenReturn(Optional.of(anfrage));
        when(repository.save(any(SponsoringAnfrage.class))).thenAnswer(inv -> inv.getArgument(0));

        SponsoringAnfrage result = service.annehme(anfrage.getId(), "Gerne, melden Sie sich!");

        assertThat(result.getStatus()).isEqualTo(AnfrageStatus.ANGENOMMEN);
        assertThat(result.getAntwort()).isEqualTo("Gerne, melden Sie sich!");
        assertThat(result.getBeantwortetAm()).isNotNull();
    }

    /** ANF-04: Ablehnen setzt Status ABGELEHNT. */
    @Test
    void ablehnenSetztStatus() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setId(UUID.randomUUID());
        anfrage.setStatus(AnfrageStatus.NEU);
        Organisation anfOrg = new Organisation();
        anfOrg.setId(UUID.randomUUID());
        anfOrg.setSlug("sponsor-ag");
        anfrage.setAnfragenderOrg(anfOrg);
        when(repository.findById(anfrage.getId())).thenReturn(Optional.of(anfrage));
        when(repository.save(any(SponsoringAnfrage.class))).thenAnswer(inv -> inv.getArgument(0));

        SponsoringAnfrage result = service.lehneAb(anfrage.getId(), "Leider kein Platz mehr.");

        assertThat(result.getStatus()).isEqualTo(AnfrageStatus.ABGELEHNT);
        assertThat(result.getAntwort()).isEqualTo("Leider kein Platz mehr.");
    }

    /** ANF-05: Bereits beantwortete Anfrage erneut annehmen wirft. */
    @Test
    void bereitsBeantwortetWirft() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setId(UUID.randomUUID());
        anfrage.setStatus(AnfrageStatus.ANGENOMMEN);
        when(repository.findById(anfrage.getId())).thenReturn(Optional.of(anfrage));

        assertThatThrownBy(() -> service.annehme(anfrage.getId(), "Nochmal"))
                .isInstanceOf(IllegalStateException.class);
    }

    /** ANF-06: Erstellen ruft BenachrichtigungsService auf. */
    @Test
    void erstelleRuftBenachrichtigungAuf() {
        SponsoringPaket paket = new SponsoringPaket();
        paket.setId(UUID.randomUUID());
        Organisation anfragender = new Organisation();
        anfragender.setId(UUID.randomUUID());
        Organisation empfaenger = new Organisation();
        empfaenger.setId(UUID.randomUUID());

        when(repository.save(any(SponsoringAnfrage.class))).thenAnswer(inv -> inv.getArgument(0));

        service.erstelle(paket, anfragender, empfaenger, "Test-Nachricht", "Max", "max@test.ch");

        verify(benachrichtigungsService).benachrichtigeUeberNeueAnfrage(any(SponsoringAnfrage.class), any());
    }

    /** ANF-07: Annehmen ruft Antwort-Benachrichtigung auf. */
    @Test
    void annehmenRuftAntwortBenachrichtigungAuf() {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setId(UUID.randomUUID());
        anfrage.setStatus(AnfrageStatus.NEU);
        anfrage.setKontaktEmail("sponsor@test.ch");
        Organisation anfOrg = new Organisation();
        anfOrg.setId(UUID.randomUUID());
        anfOrg.setSlug("sponsor-ag");
        anfrage.setAnfragenderOrg(anfOrg);
        when(repository.findById(anfrage.getId())).thenReturn(Optional.of(anfrage));
        when(repository.save(any(SponsoringAnfrage.class))).thenAnswer(inv -> inv.getArgument(0));

        service.annehme(anfrage.getId(), "Gerne!");

        verify(benachrichtigungsService).benachrichtigeUeberAntwort(any(SponsoringAnfrage.class), any());
    }
}

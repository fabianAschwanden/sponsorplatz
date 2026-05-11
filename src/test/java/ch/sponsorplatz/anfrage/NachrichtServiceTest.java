package ch.sponsorplatz.anfrage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import ch.sponsorplatz.benachrichtigung.BenachrichtigungTyp;
import ch.sponsorplatz.benachrichtigung.NotificationService;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;

/**
 * Unit-Tests für NachrichtService (MSG-01..04).
 */
@ExtendWith(MockitoExtension.class)
class NachrichtServiceTest {

    @Mock
    private NachrichtRepository nachrichtRepository;
    @Mock
    private SponsoringAnfrageRepository anfrageRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private MitgliedschaftRepository mitgliedschaftRepository;
    @Mock
    private NotificationService notificationService;

    private NachrichtService service;

    private UUID anfrageId;
    private UUID userId;
    private UUID anfragenderOrgId;
    private UUID empfaengerOrgId;

    @BeforeEach
    void setUp() {
        service = new NachrichtService(nachrichtRepository, anfrageRepository, appUserRepository,
                mitgliedschaftRepository, notificationService);
        anfrageId = UUID.randomUUID();
        userId = UUID.randomUUID();
        anfragenderOrgId = UUID.randomUUID();
        empfaengerOrgId = UUID.randomUUID();
    }

    private SponsoringAnfrage angenommeneAnfrage() {
        Organisation anfOrg = new Organisation();
        anfOrg.setId(anfragenderOrgId);
        Organisation empOrg = new Organisation();
        empOrg.setId(empfaengerOrgId);

        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setId(anfrageId);
        anfrage.setStatus(AnfrageStatus.ANGENOMMEN);
        anfrage.setAnfragenderOrg(anfOrg);
        anfrage.setEmpfaengerOrg(empOrg);
        return anfrage;
    }

    @Test
    @DisplayName("MSG-01: sendeNachricht erstellt Nachricht zu angenommener Anfrage")
    void sendeNachricht_erfolgreich() {
        SponsoringAnfrage anfrage = angenommeneAnfrage();
        when(anfrageRepository.findById(anfrageId)).thenReturn(Optional.of(anfrage));

        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.existsByUserIdAndOrgId(userId, anfragenderOrgId)).thenReturn(true);

        Nachricht gespeichert = new Nachricht();
        gespeichert.setId(UUID.randomUUID());
        when(nachrichtRepository.save(any(Nachricht.class))).thenReturn(gespeichert);

        Nachricht result = service.sendeNachricht(anfrageId, userId, "Hallo, alles klar!");

        assertThat(result).isNotNull();
        ArgumentCaptor<Nachricht> captor = ArgumentCaptor.forClass(Nachricht.class);
        verify(nachrichtRepository).save(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("Hallo, alles klar!");
        assertThat(captor.getValue().getAbsender()).isEqualTo(user);
        assertThat(captor.getValue().getAnfrage()).isEqualTo(anfrage);
    }

    @Test
    @DisplayName("MSG-02: sendeNachricht wirft IllegalStateException wenn Anfrage nicht ANGENOMMEN")
    void sendeNachricht_nichtAngenommen_wirft() {
        SponsoringAnfrage anfrage = angenommeneAnfrage();
        anfrage.setStatus(AnfrageStatus.NEU);
        when(anfrageRepository.findById(anfrageId)).thenReturn(Optional.of(anfrage));

        assertThatThrownBy(() -> service.sendeNachricht(anfrageId, userId, "Text"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("angenommenen Anfragen");
    }

    @Test
    @DisplayName("MSG-03: sendeNachricht wirft AccessDeniedException wenn User nicht beteiligt")
    void sendeNachricht_nichtBeteiligt_wirft() {
        SponsoringAnfrage anfrage = angenommeneAnfrage();
        when(anfrageRepository.findById(anfrageId)).thenReturn(Optional.of(anfrage));

        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.existsByUserIdAndOrgId(userId, anfragenderOrgId)).thenReturn(false);
        when(mitgliedschaftRepository.existsByUserIdAndOrgId(userId, empfaengerOrgId)).thenReturn(false);

        assertThatThrownBy(() -> service.sendeNachricht(anfrageId, userId, "Text"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("nicht an dieser Anfrage beteiligt");
    }

    @Test
    @DisplayName("MSG-04: findeNachAnfrage gibt Nachrichten chronologisch sortiert zurück")
    void findeNachAnfrage_chronologisch() {
        Nachricht n1 = new Nachricht();
        n1.setId(UUID.randomUUID());
        Nachricht n2 = new Nachricht();
        n2.setId(UUID.randomUUID());
        when(nachrichtRepository.findByAnfrageIdOrderByCreatedAtAsc(anfrageId))
                .thenReturn(List.of(n1, n2));

        List<Nachricht> result = service.findeNachAnfrage(anfrageId);

        assertThat(result).containsExactly(n1, n2);
    }

    @Test
    @DisplayName("MSG-02b: sendeNachricht wirft IllegalArgumentException bei leerem Text")
    void sendeNachricht_leererText_wirft() {
        assertThatThrownBy(() -> service.sendeNachricht(anfrageId, userId, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nicht leer");
    }

    @Test
    @DisplayName("MSG-09: sendeNachricht benachrichtigt Mitglieder der anderen Org, nicht den Absender")
    void sendeNachricht_publiziertNotifications() {
        SponsoringAnfrage anfrage = angenommeneAnfrage();
        anfrage.getEmpfaengerOrg().setSlug("empfaenger-slug");
        when(anfrageRepository.findById(anfrageId)).thenReturn(Optional.of(anfrage));

        AppUser absender = new AppUser();
        absender.setId(userId);
        absender.setAnzeigename("Max Muster");
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(absender));

        // Absender ist Mitglied der anfragenden Org → Empfänger-Org bekommt Notifications
        when(mitgliedschaftRepository.existsByUserIdAndOrgId(userId, anfragenderOrgId)).thenReturn(true);

        // Empfänger-Org hat zwei Mitglieder
        UUID empfaenger1 = UUID.randomUUID();
        UUID empfaenger2 = UUID.randomUUID();
        AppUser u1 = new AppUser();
        u1.setId(empfaenger1);
        AppUser u2 = new AppUser();
        u2.setId(empfaenger2);
        Mitgliedschaft m1 = new Mitgliedschaft();
        m1.setUser(u1);
        Mitgliedschaft m2 = new Mitgliedschaft();
        m2.setUser(u2);
        when(mitgliedschaftRepository.findByOrgId(empfaengerOrgId)).thenReturn(List.of(m1, m2));

        when(nachrichtRepository.save(any(Nachricht.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.sendeNachricht(anfrageId, userId, "Hallo zusammen!");

        verify(notificationService).benachrichtige(eq(empfaenger1),
                eq(BenachrichtigungTyp.NEUE_NACHRICHT), anyString(), anyString(),
                eq("/organisationen/empfaenger-slug/anfragen/" + anfrageId + "/nachrichten"));
        verify(notificationService).benachrichtige(eq(empfaenger2),
                eq(BenachrichtigungTyp.NEUE_NACHRICHT), anyString(), anyString(), anyString());
        // Absender bekommt KEINE Self-Notification, auch wenn er Doppel-Mitglied wäre
        verify(notificationService, never()).benachrichtige(eq(userId), any(), any(), any(), any());
    }
}

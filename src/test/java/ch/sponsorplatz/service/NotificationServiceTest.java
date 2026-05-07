package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Benachrichtigung;
import ch.sponsorplatz.model.BenachrichtigungTyp;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.repository.BenachrichtigungRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private BenachrichtigungRepository repository;
    @Mock private AppUserRepository appUserRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, appUserRepository);
    }

    @Test
    @DisplayName("NOTIF-01: benachrichtige erstellt Notification für gültigen Empfänger")
    void benachrichtigeErfolgreich() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("test@sp.ch");
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.benachrichtige(userId, BenachrichtigungTyp.NEUE_ANFRAGE,
                "Neue Anfrage", "Sie haben eine neue Anfrage", "/anfragen");

        ArgumentCaptor<Benachrichtigung> captor = ArgumentCaptor.forClass(Benachrichtigung.class);
        verify(repository).save(captor.capture());
        Benachrichtigung b = captor.getValue();
        assertThat(b.getTyp()).isEqualTo(BenachrichtigungTyp.NEUE_ANFRAGE);
        assertThat(b.getTitel()).isEqualTo("Neue Anfrage");
        assertThat(b.getLink()).isEqualTo("/anfragen");
        assertThat(b.isGelesen()).isFalse();
    }

    @Test
    @DisplayName("NOTIF-02: benachrichtige ignoriert unbekannten Empfänger")
    void benachrichtigeUnbekannterUser() {
        UUID userId = UUID.randomUUID();
        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        service.benachrichtige(userId, BenachrichtigungTyp.SYSTEM, "Test", null, null);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("NOTIF-03: zaehleUngelesen delegiert an Repository")
    void zaehleUngelesen() {
        UUID userId = UUID.randomUUID();
        when(repository.countByEmpfaengerIdAndGelesenFalse(userId)).thenReturn(5L);

        long result = service.zaehleUngelesen(userId);
        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("NOTIF-04: markiereAlleAlsGelesen ruft Repository Bulk-Update auf")
    void markiereAlle() {
        UUID userId = UUID.randomUUID();
        service.markiereAlleAlsGelesen(userId);
        verify(repository).markiereAlleAlsGelesen(userId);
    }

    @Test
    @DisplayName("NOTIF-05: markiereAlsGelesen setzt gelesen=true wenn User der Empfänger ist")
    void markiereEinzelne() {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        Benachrichtigung b = new Benachrichtigung();
        b.setId(notifId);
        b.setEmpfaenger(user);
        b.setGelesen(false);
        when(repository.findById(notifId)).thenReturn(Optional.of(b));

        service.markiereAlsGelesen(notifId, userId);

        verify(repository).save(b);
        assertThat(b.isGelesen()).isTrue();
    }

    @Test
    @DisplayName("NOTIF-06: markiereAlsGelesen wirft AccessDeniedException bei fremder Notif (IDOR)")
    void markiereFremdeNotifAccessDenied() {
        UUID empfaengerId = UUID.randomUUID();
        UUID fremderUserId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        AppUser empfaenger = new AppUser();
        empfaenger.setId(empfaengerId);
        Benachrichtigung b = new Benachrichtigung();
        b.setId(notifId);
        b.setEmpfaenger(empfaenger);
        when(repository.findById(notifId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.markiereAlsGelesen(notifId, fremderUserId))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).save(any());
        assertThat(b.isGelesen()).isFalse();
    }
}


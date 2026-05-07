package ch.sponsorplatz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import ch.sponsorplatz.model.AnfrageStatus;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.model.Nachricht;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.SponsoringAnfrage;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
import ch.sponsorplatz.repository.NachrichtRepository;
import ch.sponsorplatz.repository.SponsoringAnfrageRepository;

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

    private NachrichtService service;

    private UUID anfrageId;
    private UUID userId;
    private UUID anfragenderOrgId;
    private UUID empfaengerOrgId;

    @BeforeEach
    void setUp() {
        service = new NachrichtService(nachrichtRepository, anfrageRepository, appUserRepository,
                mitgliedschaftRepository);
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
}

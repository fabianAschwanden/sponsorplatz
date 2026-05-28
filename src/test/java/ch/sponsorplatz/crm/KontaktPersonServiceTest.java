package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.AccessControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KONTAKT-SVC-01..05 — Zugriffs-Schranke + Account-Scoping des KontaktPersonService.
 * Der Mandanten-Key wird beim Anlegen vom Account denormalisiert; jeder Zugriff
 * prüft kannSponsorDatenSehen gegen den Account-Besitzer.
 */
@ExtendWith(MockitoExtension.class)
class KontaktPersonServiceTest {

    @Mock private KontaktPersonRepository repository;
    @Mock private SponsorAccountRepository accountRepository;
    @Mock private AccessControl accessControl;
    @Mock private Authentication auth;

    private KontaktPersonService service;

    private final UUID sponsorOrgId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private SponsorAccount account;

    @BeforeEach
    void setUp() {
        service = new KontaktPersonService(repository, accountRepository, accessControl);
        account = new SponsorAccount();
        account.setId(accountId);
        account.setBesitzerSponsorOrgId(sponsorOrgId);
    }

    /** KONTAKT-SVC-01: findeKontakte ohne Zugriff → AccessDenied. */
    @Test
    @DisplayName("KONTAKT-SVC-01: findeKontakte ohne Zugriff wirft AccessDenied")
    void findeOhneZugriffWirft() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.findeKontakte(accountId, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).findByAccountIdOrderByNachnameAscVornameAsc(any());
    }

    /** KONTAKT-SVC-02: findeKontakte mit Zugriff → Liste. */
    @Test
    @DisplayName("KONTAKT-SVC-02: findeKontakte mit Zugriff liefert Kontakte")
    void findeMitZugriff() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(repository.findByAccountIdOrderByNachnameAscVornameAsc(accountId))
                .thenReturn(List.of(kontakt("Anna", "Muster")));

        List<KontaktPersonView> kontakte = service.findeKontakte(accountId, auth);

        assertThat(kontakte).hasSize(1);
        assertThat(kontakte.get(0).name()).isEqualTo("Anna Muster");
    }

    /** KONTAKT-SVC-03: erstelle ohne Zugriff → AccessDenied, kein save. */
    @Test
    @DisplayName("KONTAKT-SVC-03: erstelle ohne Zugriff wirft AccessDenied")
    void erstelleOhneZugriffWirft() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.erstelle(accountId, "Tom", "Trainer",
                "Trainer", KontaktRolle.SONSTIGE, "t@v.ch", "044", "079", auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    /** KONTAKT-SVC-04: erstelle denormalisiert Mandanten-Key vom Account. */
    @Test
    @DisplayName("KONTAKT-SVC-04: erstelle setzt besitzer vom Account + Felder")
    void erstelleDenormalisiert() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KontaktPersonView view = service.erstelle(accountId, "Eva", "Präsidentin",
                "Präsidentin", KontaktRolle.HAUPTANSPRECHPARTNER, "eva@v.ch", "044", "079", auth);

        ArgumentCaptor<KontaktPerson> cap = ArgumentCaptor.forClass(KontaktPerson.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getBesitzerSponsorOrgId()).isEqualTo(sponsorOrgId);
        assertThat(cap.getValue().getAccount()).isEqualTo(account);
        assertThat(cap.getValue().getKontaktRolle()).isEqualTo(KontaktRolle.HAUPTANSPRECHPARTNER);
        assertThat(view.name()).isEqualTo("Eva Präsidentin");
    }

    /** KONTAKT-SVC-05: loesche prüft Zugriff über den Kontakt-Besitzer. */
    @Test
    @DisplayName("KONTAKT-SVC-05: loesche ohne Zugriff wirft AccessDenied, kein delete")
    void loescheOhneZugriffWirft() {
        UUID kontaktId = UUID.randomUUID();
        KontaktPerson k = kontakt("X", "Y");
        k.setId(kontaktId);
        when(repository.findById(kontaktId)).thenReturn(Optional.of(k));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.loesche(kontaktId, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).delete(any());
    }

    private KontaktPerson kontakt(String vorname, String nachname) {
        KontaktPerson k = new KontaktPerson();
        k.setId(UUID.randomUUID());
        k.setBesitzerSponsorOrgId(sponsorOrgId);
        k.setAccount(account);
        k.setVorname(vorname);
        k.setNachname(nachname);
        k.setKontaktRolle(KontaktRolle.SONSTIGE);
        return k;
    }
}

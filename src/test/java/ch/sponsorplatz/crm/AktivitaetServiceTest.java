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

import java.time.LocalDate;
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
 * AKT-SVC-01..06 — Zugriffs-Schranke, Account-Scoping + Kontakt-Validierung
 * des AktivitaetService.
 */
@ExtendWith(MockitoExtension.class)
class AktivitaetServiceTest {

    @Mock private AktivitaetRepository repository;
    @Mock private SponsorAccountRepository accountRepository;
    @Mock private KontaktPersonRepository kontaktRepository;
    @Mock private AccessControl accessControl;
    @Mock private Authentication auth;

    private AktivitaetService service;

    private final UUID sponsorOrgId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private SponsorAccount account;

    @BeforeEach
    void setUp() {
        service = new AktivitaetService(repository, accountRepository, kontaktRepository, accessControl);
        account = new SponsorAccount();
        account.setId(accountId);
        account.setBesitzerSponsorOrgId(sponsorOrgId);
    }

    /** AKT-SVC-01: findeTimeline ohne Zugriff → AccessDenied. */
    @Test
    @DisplayName("AKT-SVC-01: findeTimeline ohne Zugriff wirft AccessDenied")
    void timelineOhneZugriffWirft() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.findeTimeline(accountId, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).findByAccountIdOrderByDatumDescErstelltAmDesc(any());
    }

    /** AKT-SVC-02: findeTimeline mit Zugriff → Liste. */
    @Test
    @DisplayName("AKT-SVC-02: findeTimeline mit Zugriff liefert Aktivitäten")
    void timelineMitZugriff() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(repository.findByAccountIdOrderByDatumDescErstelltAmDesc(accountId))
                .thenReturn(List.of(aktivitaet("Erstkontakt")));

        List<AktivitaetView> timeline = service.findeTimeline(accountId, auth);

        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).betreff()).isEqualTo("Erstkontakt");
    }

    /** AKT-SVC-03: erstelle ohne Zugriff → AccessDenied, kein save. */
    @Test
    @DisplayName("AKT-SVC-03: erstelle ohne Zugriff wirft AccessDenied")
    void erstelleOhneZugriffWirft() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.erstelle(accountId, AktivitaetTyp.ANRUF,
                LocalDate.now(), "Call", "Notiz", null, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    /** AKT-SVC-04: erstelle denormalisiert Mandanten-Key + setzt Felder. */
    @Test
    @DisplayName("AKT-SVC-04: erstelle setzt besitzer/typ/datum/betreff")
    void erstelleSpeichert() {
        LocalDate datum = LocalDate.of(2026, 5, 28);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AktivitaetView view = service.erstelle(accountId, AktivitaetTyp.MEETING,
                datum, "Jahresgespräch", "Lief gut", null, auth);

        ArgumentCaptor<Aktivitaet> cap = ArgumentCaptor.forClass(Aktivitaet.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getBesitzerSponsorOrgId()).isEqualTo(sponsorOrgId);
        assertThat(cap.getValue().getTyp()).isEqualTo(AktivitaetTyp.MEETING);
        assertThat(cap.getValue().getDatum()).isEqualTo(datum);
        assertThat(view.betreff()).isEqualTo("Jahresgespräch");
    }

    /** AKT-SVC-05: Kontakt der NICHT zum Account gehört → IllegalArgumentException. */
    @Test
    @DisplayName("AKT-SVC-05: erstelle mit accountfremdem Kontakt wirft IllegalArgument")
    void erstelleMitFremdemKontaktWirft() {
        UUID kontaktId = UUID.randomUUID();
        KontaktPerson fremderKontakt = new KontaktPerson();
        fremderKontakt.setId(kontaktId);
        SponsorAccount andererAccount = new SponsorAccount();
        andererAccount.setId(UUID.randomUUID());
        fremderKontakt.setAccount(andererAccount); // gehört zu einem ANDEREN Account

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(kontaktRepository.findById(kontaktId)).thenReturn(Optional.of(fremderKontakt));

        assertThatThrownBy(() -> service.erstelle(accountId, AktivitaetTyp.ANRUF,
                LocalDate.now(), "Call", null, kontaktId, auth))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    /** AKT-SVC-06: loesche prüft Zugriff über den Aktivitäts-Besitzer. */
    @Test
    @DisplayName("AKT-SVC-06: loesche ohne Zugriff wirft AccessDenied")
    void loescheOhneZugriffWirft() {
        UUID aktId = UUID.randomUUID();
        Aktivitaet a = aktivitaet("X");
        a.setId(aktId);
        when(repository.findById(aktId)).thenReturn(Optional.of(a));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.loesche(aktId, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).delete(any());
    }

    private Aktivitaet aktivitaet(String betreff) {
        Aktivitaet a = new Aktivitaet();
        a.setId(UUID.randomUUID());
        a.setBesitzerSponsorOrgId(sponsorOrgId);
        a.setAccount(account);
        a.setTyp(AktivitaetTyp.NOTIZ);
        a.setDatum(LocalDate.now());
        a.setBetreff(betreff);
        return a;
    }
}

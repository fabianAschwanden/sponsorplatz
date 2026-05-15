package ch.sponsorplatz.aufgabe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link AufgabenBadgeAdvice} — Sidebar-Badge mit Anzahl
 * offener Aufgaben des eingeloggten Users.
 * Test-IDs: AUFG-BADGE-01..04.
 */
class AufgabenBadgeAdviceTest {

    private AufgabenService service;
    @SuppressWarnings("unchecked")
    private final ObjectProvider<AufgabenService> provider = mock(ObjectProvider.class);
    private AufgabenBadgeAdvice advice;

    @BeforeEach
    void setUp() {
        service = mock(AufgabenService.class);
        when(provider.getIfAvailable()).thenReturn(service);
        advice = new AufgabenBadgeAdvice(provider);
    }

    /** AUFG-BADGE-01: User mit offenen Aufgaben → Anzahl als Long. */
    @Test
    @DisplayName("AUFG-BADGE-01: User mit offenen Aufgaben → Anzahl als Long")
    void mitOffenenAufgabenGibtAnzahl() {
        when(service.zaehleMeineOffenen("u@test.ch")).thenReturn(7L);

        Long result = advice.badgeAufgaben(eingeloggt("u@test.ch"));

        assertThat(result).isEqualTo(7L);
    }

    /** AUFG-BADGE-02: User ohne offene Aufgaben → null (kein leerer Badge). */
    @Test
    @DisplayName("AUFG-BADGE-02: User ohne offene Aufgaben → null")
    void ohneOffeneAufgabenGibtNull() {
        when(service.zaehleMeineOffenen("u@test.ch")).thenReturn(0L);

        Long result = advice.badgeAufgaben(eingeloggt("u@test.ch"));

        assertThat(result).isNull();
    }

    /** AUFG-BADGE-03: Anonyme User → null (kein Service-Aufruf). */
    @Test
    @DisplayName("AUFG-BADGE-03: Anonyme User → null und keine Service-Last")
    void anonymGibtNull() {
        Authentication anonym = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        Long result = advice.badgeAufgaben(anonym);

        assertThat(result).isNull();
        verify(service, never()).zaehleMeineOffenen(any());
    }

    /** AUFG-BADGE-04: ObjectProvider liefert null (WebMvcTest-Slice) → null. */
    @Test
    @DisplayName("AUFG-BADGE-04: Service nicht im Kontext (Slice-Test) → null")
    void serviceFehltGibtNull() {
        when(provider.getIfAvailable()).thenReturn(null);

        Long result = advice.badgeAufgaben(eingeloggt("u@test.ch"));

        assertThat(result).isNull();
    }

    private Authentication eingeloggt(String email) {
        return new UsernamePasswordAuthenticationToken(email, "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}

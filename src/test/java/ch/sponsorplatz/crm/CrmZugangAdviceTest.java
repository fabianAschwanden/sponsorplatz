package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgTyp;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CRM-NAV-01..04 — {@link CrmZugangAdvice} liefert {@code crmSponsorSlug} für die
 * Sidebar: nur Firmen-Mitglieder mit Bearbeitungsrechten bekommen den CRM-Einstieg.
 */
class CrmZugangAdviceTest {

    private MitgliedschaftRepository repository;
    @SuppressWarnings("unchecked")
    private final ObjectProvider<MitgliedschaftRepository> provider = mock(ObjectProvider.class);
    private CrmZugangAdvice advice;

    @BeforeEach
    void setUp() {
        repository = mock(MitgliedschaftRepository.class);
        when(provider.getIfAvailable()).thenReturn(repository);
        advice = new CrmZugangAdvice(provider);
    }

    /** CRM-NAV-01: Firmen-Mitglied mit Edit-Rolle → erster Slug (E-Mail normalisiert). */
    @Test
    @DisplayName("CRM-NAV-01: Firmen-Editor bekommt CRM-Slug")
    void firmaMitgliedBekommtSlug() {
        when(repository.findSponsorOrgSlugs(eq("u@firma.ch"), any(), eq(OrgTyp.UNTERNEHMEN)))
                .thenReturn(List.of("css", "helsana"));

        String slug = advice.crmSponsorSlug(eingeloggt("U@Firma.ch"));

        assertThat(slug).isEqualTo("css");
    }

    /** CRM-NAV-02: keine passende Mitgliedschaft → null (kein CRM-Eintrag). */
    @Test
    @DisplayName("CRM-NAV-02: ohne Firma/Edit-Rolle → null")
    void ohneFirmaGibtNull() {
        when(repository.findSponsorOrgSlugs(any(), any(), any())).thenReturn(List.of());

        assertThat(advice.crmSponsorSlug(eingeloggt("verein@v.ch"))).isNull();
    }

    /** CRM-NAV-03: anonymer User → null, kein Repo-Aufruf. */
    @Test
    @DisplayName("CRM-NAV-03: anonym → null, keine DB-Last")
    void anonymGibtNull() {
        Authentication anonym = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(advice.crmSponsorSlug(anonym)).isNull();
        verify(repository, never()).findSponsorOrgSlugs(any(), any(), any());
    }

    /** CRM-NAV-04: Repository fehlt im Slice (ObjectProvider null) → null. */
    @Test
    @DisplayName("CRM-NAV-04: Repo nicht im Kontext → null")
    void repoFehltGibtNull() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThat(advice.crmSponsorSlug(eingeloggt("u@firma.ch"))).isNull();
    }

    private Authentication eingeloggt(String email) {
        return new UsernamePasswordAuthenticationToken(email, "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}

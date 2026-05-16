package ch.sponsorplatz.shared.einstellungen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link StyleAdvice}. Test-IDs: STYLE-ADV-01..03.
 */
class StyleAdviceTest {

    private PlattformEinstellungenService service;
    @SuppressWarnings("unchecked")
    private final ObjectProvider<PlattformEinstellungenService> provider = mock(ObjectProvider.class);
    private StyleAdvice advice;

    @BeforeEach
    void setUp() {
        service = mock(PlattformEinstellungenService.class);
        when(provider.getIfAvailable()).thenReturn(service);
        advice = new StyleAdvice(provider);
    }

    @Test
    @DisplayName("STYLE-ADV-01: Service liefert 'css-ch' → Advice gibt 'css-ch'")
    void liefertGesetztenStyle() {
        when(service.ladeAktivenStyle()).thenReturn("css-ch");
        assertThat(advice.aktiverStyle()).isEqualTo("css-ch");
    }

    @Test
    @DisplayName("STYLE-ADV-02: Service fehlt im Slice → Default 'default'")
    void serviceFehltGibtDefault() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThat(advice.aktiverStyle()).isEqualTo("default");
    }

    @Test
    @DisplayName("STYLE-ADV-03: Service wirft (z.B. Singleton fehlt) → Default 'default'")
    void serviceWirftGibtDefault() {
        when(service.ladeAktivenStyle()).thenThrow(new IllegalStateException("Singleton fehlt"));
        assertThat(advice.aktiverStyle()).isEqualTo("default");
    }
}

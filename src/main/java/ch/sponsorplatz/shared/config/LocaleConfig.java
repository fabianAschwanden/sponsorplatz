package ch.sponsorplatz.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Mehrsprachigkeits-Konfiguration: Cookie-basierter LocaleResolver +
 * {@code ?lang=de|fr|it|en} URL-Override.
 *
 * <p>Default-Locale: {@code de_CH}. Cookie-Name: {@code lang}, Dauer: 365 Tage.
 *
 * <p><b>Wichtig:</b> Die Resource-Bundles sind nach Sprach-und-Land benannt
 * ({@code messages_de_CH.properties}, {@code messages_fr_CH.properties} etc.,
 * Englisch nur {@code messages_en.properties}). Damit Spring's
 * {@code ResourceBundleMessageSource} den richtigen Bundle findet, MUSS das
 * Locale exakt das Country-Suffix tragen — sonst fällt Spring auf das
 * (kaputte) Default-Bundle {@code messages.properties} zurück.
 *
 * <p>Frühere Variante akzeptierte {@code Locale("fr")} ohne Land — was zu
 * "Sprache wechselt nicht" führte, weil {@code messages_fr.properties} nicht
 * existiert.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("lang");
        resolver.setDefaultLocale(Locale.forLanguageTag("de-CH"));
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookiePath("/");
        return resolver;
    }

    /**
     * Whitelist + Mapping auf das Locale, dessen Bundle wir tatsächlich haben.
     * Werte ausserhalb dieser Map werden vom Interceptor verworfen
     * ({@code parseLocaleValue} liefert {@code null}) — Spring fällt dann
     * auf das Default-Locale ({@code de_CH}) zurück.
     */
    private static final Map<String, Locale> ERLAUBTE_SPRACHEN = Map.of(
            "de", Locale.forLanguageTag("de-CH"),
            "fr", Locale.forLanguageTag("fr-CH"),
            "it", Locale.forLanguageTag("it-CH"),
            "en", Locale.ENGLISH);

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor() {
            @Override
            protected Locale parseLocaleValue(String wert) {
                if (wert == null) return null;
                String lang = wert.toLowerCase().split("[_-]")[0];
                return ERLAUBTE_SPRACHEN.get(lang);
            }
        };
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}


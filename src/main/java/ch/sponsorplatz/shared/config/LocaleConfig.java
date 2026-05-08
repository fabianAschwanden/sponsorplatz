package ch.sponsorplatz.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Mehrsprachigkeits-Konfiguration: Cookie-basierter LocaleResolver +
 * {@code ?lang=de|fr|it} URL-Override.
 *
 * <p>Default-Locale: {@code de_CH}. Cookie-Name: {@code lang}, Dauer: 365 Tage.
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
     * Whitelist gegen Cookie-/URL-Manipulation: nur diese Sprach-Codes werden
     * angenommen. {@code parseLocaleValue} liefert für Werte ausserhalb der
     * Whitelist {@code null} zurück — Spring fällt dann auf das Default-Locale
     * zurück. Der Override ist Spring-Version-stabil
     * (im Gegensatz zu {@code setLanguageTags}, das erst ab 6.1 existiert).
     */
    private static final List<String> ERLAUBTE_SPRACHEN = List.of("de", "fr", "it");

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor() {
            @Override
            protected Locale parseLocaleValue(String wert) {
                Locale geparst = super.parseLocaleValue(wert);
                if (geparst != null && ERLAUBTE_SPRACHEN.contains(geparst.getLanguage())) {
                    return geparst;
                }
                return null;
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


package ch.sponsorplatz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Duration;

/**
 * Sicherheits-Konfiguration für Sponsorplatz.
 *
 * Profile-Strategie:
 * - dev (default):     alles offen für lokale Entwicklung
 * - prod:              Form-Login + OIDC-Vorbereitung (Phase 1+)
 *
 * Public-Marktplatz-Routen werden in einer späteren Iteration ergänzt.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
            @Value("${sponsorplatz.rate-limit.capacity:30}") long capacity,
            @Value("${sponsorplatz.rate-limit.window-seconds:60}") long windowSeconds) {
        return new RateLimitFilter(capacity, Duration.ofSeconds(windowSeconds));
    }

    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/images/**", "/favicon.ico", "/sitemap.xml").permitAll()
                .requestMatchers("/impressum", "/datenschutz").permitAll()
                .requestMatchers("/login", "/registrieren", "/verifizieren").permitAll()
                .requestMatchers("/passwort-vergessen", "/passwort-reset").permitAll()
                .requestMatchers("/sponsor/**").permitAll()
                .requestMatchers("/einladung/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/organisationen").permitAll()
                .requestMatchers("/organisationen/{slug}").permitAll()
                .requestMatchers("/marktplatz/**").permitAll()
                .requestMatchers("/medien/**").permitAll()
                .requestMatchers("/vereine/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf
                // /benachrichtigungen/**: interne State-Mutation per fetch ohne CSRF-
                // Token. Owner-Check im Service schützt vor IDOR; Cross-Origin durch
                // SameSite-Cookies blockiert.
                .ignoringRequestMatchers("/h2-console/**", "/benachrichtigungen/**")
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(h -> h.frameOptions(f -> f.disable()));
        return http.build();
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain prodFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/images/**", "/favicon.ico", "/sitemap.xml").permitAll()
                .requestMatchers("/impressum", "/datenschutz").permitAll()
                .requestMatchers("/login", "/registrieren", "/verifizieren").permitAll()
                .requestMatchers("/passwort-vergessen", "/passwort-reset").permitAll()
                .requestMatchers("/sponsor/**").permitAll()
                .requestMatchers("/einladung/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/organisationen").permitAll()
                .requestMatchers("/organisationen/{slug}").permitAll()
                .requestMatchers("/marktplatz/**").permitAll()
                .requestMatchers("/medien/**").permitAll()
                .requestMatchers("/vereine/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .csrf(csrf -> csrf
                // siehe devFilterChain — gleiche Begründung
                .ignoringRequestMatchers("/benachrichtigungen/**")
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .logout(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

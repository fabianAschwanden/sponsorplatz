package ch.sponsorplatz.shared.config;

import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Sicherheits-Konfiguration für Sponsorplatz.
 *
 * Profile-Strategie:
 * - dev (default): alles offen für lokale Entwicklung
 * - prod: Form-Login + OIDC-Vorbereitung (Phase 1+)
 *
 * Public-Marktplatz-Routen werden in einer späteren Iteration ergänzt.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public LoginBruteForceSchutz loginBruteForceSchutz() {
        return new LoginBruteForceSchutz();
    }

    @Bean
    public LoginSperreFilter loginSperreFilter(LoginBruteForceSchutz bruteForceSchutz) {
        return new LoginSperreFilter(bruteForceSchutz);
    }

    @Bean
    public LoginFailureHandler loginFailureHandler(LoginBruteForceSchutz bruteForceSchutz) {
        return new LoginFailureHandler(bruteForceSchutz);
    }

    // Hinweis: Der konkrete LoginSuccessHandler wohnt im benutzer/-Package
    // (gehört dort hin — er liest AppUser-Spracheinstellung beim Login). Bean-
    // Definition steht in BenutzerSecurityConfig; SecurityConfig kennt nur den
    // Spring-Interface-Typ AuthenticationSuccessHandler (ARCH-07).
    //
    // Fallback unten greift in WebMvcTests, die nur SecurityConfig importieren —
    // sie sehen BenutzerSecurityConfig nicht und brauchen einen Default-Handler,
    // damit die FilterChain initialisierbar bleibt.
    @Bean
    @ConditionalOnMissingBean(name = "loginSuccessHandler")
    public AuthenticationSuccessHandler loginSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler h = new SavedRequestAwareAuthenticationSuccessHandler();
        h.setDefaultTargetUrl("/dashboard");
        return h;
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationSuccessHandler.class)
    public AuthenticationSuccessHandler defaultLoginSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/dashboard");
        return handler;
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
            @Value("${sponsorplatz.rate-limit.capacity:30}") long capacity,
            @Value("${sponsorplatz.rate-limit.window-seconds:60}") long windowSeconds) {
        return new RateLimitFilter(capacity, Duration.ofSeconds(windowSeconds));
    }

    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http,
            RateLimitFilter rateLimitFilter,
            LoginSperreFilter loginSperreFilter,
            LoginFailureHandler loginFailureHandler,
            AuthenticationSuccessHandler loginSuccessHandler,
            ObjectProvider<ClientRegistrationRepository> oauth2Clients,
            ObjectProvider<OAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/images/**", "/favicon.ico", "/sitemap.xml").permitAll()
                        .requestMatchers("/impressum", "/datenschutz").permitAll()
                        .requestMatchers("/login", "/registrieren", "/verifizieren").permitAll()
                        .requestMatchers("/passwort-vergessen", "/passwort-reset").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/sponsor/**").permitAll()
                        .requestMatchers("/einladung/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/organisationen").permitAll()
                        .requestMatchers("/organisationen/{slug}").permitAll()
                        .requestMatchers("/marktplatz/**").permitAll()
                        .requestMatchers("/medien/**").permitAll()
                        .requestMatchers("/vereine/**").permitAll()
                        .requestMatchers("/fuer-marken").permitAll()
                        .requestMatchers("/marken/*/engagements").permitAll()
                        .requestMatchers("/og/**").permitAll()
                        .requestMatchers("/payment/webhook/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**", "/benachrichtigungen/**", "/payment/webhook/**"))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginSperreFilter, RateLimitFilter.class)
                .headers(h -> h.frameOptions(f -> f.disable()));

        wendeOidcLoginAn(http, oauth2Clients, oidcUserService, loginSuccessHandler, loginFailureHandler);
        return http.build();
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain prodFilterChain(HttpSecurity http,
            RateLimitFilter rateLimitFilter,
            LoginSperreFilter loginSperreFilter,
            LoginFailureHandler loginFailureHandler,
            AuthenticationSuccessHandler loginSuccessHandler,
            ObjectProvider<ClientRegistrationRepository> oauth2Clients,
            ObjectProvider<OAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/images/**", "/favicon.ico", "/sitemap.xml").permitAll()
                        .requestMatchers("/impressum", "/datenschutz").permitAll()
                        .requestMatchers("/login", "/registrieren", "/verifizieren").permitAll()
                        .requestMatchers("/passwort-vergessen", "/passwort-reset").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/sponsor/**").permitAll()
                        .requestMatchers("/einladung/**").permitAll()
                        // Actuator: nur die K8s-Probes + info sind öffentlich auf
                        // dem Application-Port. /actuator/prometheus wandert in prod
                        // auf den Management-Port (application-prod.properties:
                        // management.server.port=9090, loopback-bind). Falls Operator
                        // den Management-Port auf 8080 zurückklappt, greifen alle
                        // Actuator-Routen unter /actuator/** den anyRequest-authenticated-
                        // Pfad — Prometheus erfordert dann Auth. Damit ist „permitAll
                        // versehentlich gesetzt" als Misconfiguration ausgeschlossen.
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness",
                                "/actuator/info").permitAll()
                        .requestMatchers("/organisationen").permitAll()
                        .requestMatchers("/organisationen/{slug}").permitAll()
                        .requestMatchers("/marktplatz/**").permitAll()
                        .requestMatchers("/medien/**").permitAll()
                        .requestMatchers("/vereine/**").permitAll()
                        .requestMatchers("/fuer-marken").permitAll()
                        .requestMatchers("/marken/*/engagements").permitAll()
                        .requestMatchers("/og/**").permitAll()
                        .requestMatchers("/payment/webhook/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/benachrichtigungen/**", "/payment/webhook/**"))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginSperreFilter, RateLimitFilter.class)
                .logout(Customizer.withDefaults());

        wendeOidcLoginAn(http, oauth2Clients, oidcUserService, loginSuccessHandler, loginFailureHandler);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Aktiviert den OAuth2/OIDC-Login NUR wenn ein
     * {@link ClientRegistrationRepository}
     * im Context vorhanden ist (d.h.
     * {@code spring.security.oauth2.client.registration.*}
     * ist konfiguriert). In dev ohne Entra-Konfig ist der ObjectProvider leer
     * und wir bleiben beim reinen Form-Login.
     */
    private void wendeOidcLoginAn(HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> oauth2Clients,
            ObjectProvider<OAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService,
            AuthenticationSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler) throws Exception {
        if (oauth2Clients.getIfAvailable() == null) {
            return;
        }
        OAuth2UserService<OidcUserRequest, OidcUser> userService = oidcUserService.getIfAvailable();
        http.oauth2Login(oauth2 -> {
            oauth2.loginPage("/login");
            oauth2.successHandler(loginSuccessHandler);
            oauth2.failureHandler(loginFailureHandler);
            if (userService != null) {
                oauth2.userInfoEndpoint(ui -> ui.oidcUserService(userService));
            }
        });
    }
}

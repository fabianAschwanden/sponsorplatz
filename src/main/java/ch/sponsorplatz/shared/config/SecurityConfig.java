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
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

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
            ApiKeyFilter apiKeyFilter,
            ObjectProvider<ClientRegistrationRepository> oauth2Clients,
            ObjectProvider<OAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Öffentlich für anonyme Besucher: Home + Kontakt-Formular + Auth-Flows.
                        // Marktplatz, Vereine, Marken-Landing, Org-Listing, Sponsor-Self-Reg
                        // erfordern jetzt Login — die /kontakt-Seite ist der einzige Anfrage-Funnel.
                        .requestMatchers("/", "/css/**", "/images/**", "/favicon.ico", "/sitemap.xml").permitAll()
                        .requestMatchers("/impressum", "/datenschutz", "/agb").permitAll()
                        .requestMatchers("/kontakt").permitAll()
                        .requestMatchers("/login", "/login/2fa", "/registrieren", "/verifizieren").permitAll()
                        .requestMatchers("/passwort-vergessen", "/passwort-reset").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/einladung/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // /medien/** + /og/** bleiben öffentlich aus technischen Gründen
                        // (OpenGraph-Crawler, Mail-Bilder), /payment/webhook/** für Stripe.
                        .requestMatchers("/medien/**").permitAll()
                        .requestMatchers("/og/**").permitAll()
                        .requestMatchers("/payment/webhook/**").permitAll()
                        // REST-API: permitAll auf HTTP-Ebene, ApiKeyFilter prüft X-API-Key
                        .requestMatchers("/api/**").permitAll()
                        // Spring-Boot-Error-Dispatch: ein nicht-2xx-Response auf irgendeiner
                        // Route triggert einen internen Forward nach /error. Wenn /error
                        // hier nicht permitAll wäre, würde die Security-Chain den Forward
                        // wieder umleiten — Validierungsfehler (400) auf der API würden
                        // als 302 → /login auftauchen statt sauberer 400-Status.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .logout(logout -> {
                    LogoutSuccessHandler oidcLogout = oidcLogoutHandler(oauth2Clients);
                    if (oidcLogout != null) {
                        // RP-initiated Logout (Spec §11): nach lokalem Session-Kill
                        // wird der User zum end_session_endpoint des IdP geleitet —
                        // verhindert Auto-Re-Login via noch aktive IdP-Session.
                        logout.logoutSuccessHandler(oidcLogout);
                    } else {
                        logout.logoutSuccessUrl("/");
                    }
                    logout.permitAll();
                })
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**", "/benachrichtigungen/**", "/payment/webhook/**", "/api/**"))
                // Filter-Reihenfolge explizit linear: LoginSperre → RateLimit →
                // ApiKey → UsernamePassword. So ist Brute-Force gegen den
                // API-Key über RateLimitFilter abgedeckt.
                .addFilterBefore(loginSperreFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, LoginSperreFilter.class)
                .addFilterAfter(apiKeyFilter, RateLimitFilter.class)
                .headers(h -> {
                    h.frameOptions(f -> f.disable());
                    // Security-Hardening (Phase 10.5) — lockerer in dev für H2-Console
                    h.contentTypeOptions(Customizer.withDefaults());
                    h.referrerPolicy(r -> r.policy(
                            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    h.permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()"));
                    h.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://browser.sentry-cdn.com; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data:; " +
                            "font-src 'self'; " +
                            "connect-src 'self' https://*.ingest.sentry.io; " +
                            "frame-src 'self'; " +
                            "frame-ancestors 'self'"
                    ));
                });

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
            ApiKeyFilter apiKeyFilter,
            ObjectProvider<ClientRegistrationRepository> oauth2Clients,
            ObjectProvider<OAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Öffentlich für anonyme Besucher: Home + Kontakt-Formular + Auth-Flows.
                        // Marktplatz, Vereine, Marken-Landing, Org-Listing, Sponsor-Self-Reg
                        // erfordern jetzt Login — die /kontakt-Seite ist der einzige Anfrage-Funnel.
                        .requestMatchers("/", "/css/**", "/images/**", "/favicon.ico", "/sitemap.xml").permitAll()
                        .requestMatchers("/impressum", "/datenschutz", "/agb").permitAll()
                        .requestMatchers("/kontakt").permitAll()
                        .requestMatchers("/login", "/login/2fa", "/registrieren", "/verifizieren").permitAll()
                        .requestMatchers("/passwort-vergessen", "/passwort-reset").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
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
                        // /medien/** + /og/** bleiben öffentlich aus technischen Gründen
                        // (OpenGraph-Crawler, Mail-Bilder), /payment/webhook/** für Stripe.
                        .requestMatchers("/medien/**").permitAll()
                        .requestMatchers("/og/**").permitAll()
                        .requestMatchers("/payment/webhook/**").permitAll()
                        // REST-API: permitAll auf HTTP-Ebene, ApiKeyFilter prüft X-API-Key
                        .requestMatchers("/api/**").permitAll()
                        // Spring-Boot-Error-Dispatch — siehe dev-Chain für die Begründung.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/benachrichtigungen/**", "/payment/webhook/**", "/api/**"))
                // Filter-Reihenfolge explizit linear: LoginSperre → RateLimit →
                // ApiKey → UsernamePassword. So ist Brute-Force gegen den
                // API-Key über RateLimitFilter abgedeckt.
                .addFilterBefore(loginSperreFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, LoginSperreFilter.class)
                .addFilterAfter(apiKeyFilter, RateLimitFilter.class)
                .headers(h -> {
                    // Security-Hardening (Phase 10.5)
                    h.contentTypeOptions(Customizer.withDefaults());
                    h.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000));
                    h.referrerPolicy(r -> r.policy(
                            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    h.permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()"));
                    h.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' https://browser.sentry-cdn.com; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data:; " +
                            "font-src 'self'; " +
                            "connect-src 'self' https://*.ingest.sentry.io; " +
                            "frame-ancestors 'none'"
                    ));
                })
                .logout(logout -> {
                    LogoutSuccessHandler oidcLogout = oidcLogoutHandler(oauth2Clients);
                    if (oidcLogout != null) {
                        logout.logoutSuccessHandler(oidcLogout);
                    }
                });

        wendeOidcLoginAn(http, oauth2Clients, oidcUserService, loginSuccessHandler, loginFailureHandler);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Liefert einen {@link OidcClientInitiatedLogoutSuccessHandler} wenn ein
     * {@link ClientRegistrationRepository} im Context ist, sonst {@code null}.
     * Konfiguriert {@code {baseUrl}/} als post_logout_redirect_uri — die URI
     * muss beim IdP als "Logout URI" registriert sein (Spec §4.1).
     * Aufrufer entscheiden ob sie auf den Default-Logout zurückfallen.
     */
    private LogoutSuccessHandler oidcLogoutHandler(
            ObjectProvider<ClientRegistrationRepository> oauth2Clients) {
        ClientRegistrationRepository repo = oauth2Clients.getIfAvailable();
        if (repo == null) {
            return null;
        }
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(repo);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
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

# Authentifizierung — SSO via OIDC (Entra ID)

> **Status:** Backlog (Mai 2026), Spec aktiv
> **Bezug:** Backlog-Item "OIDC SSO Entra ID" (V21)
> **Phase-Verortung:** Erweiterung Phase 1.1 (Form-Login) → neue Phase 1.4 oder eigenständig
> **Referenz-Specs:** `ROLLENKONZEPT.md`, `DATENMODELL.md`, `TESTSTRATEGIE.md`

---

## 1. Ziel

Single-Sign-On für Corporate-User (CSS-Sponsoring-Team, regionale CSS-Agenturen, mittelfristig auch Verbands-Partner) via **Microsoft Entra ID** (vormals Azure AD), **zusätzlich** zur bestehenden Form-Login-Authentication. Form-Login bleibt für externe Vereinsmitglieder ohne Entra-Account erhalten.

**Nicht-Ziele:**

- Single Logout (Provider-initiated Logout) — initial out of scope
- B2C-Federation (Vereinsmitglieder via Google/Apple) — Phase 2
- Mehrere parallele Identity-Provider — initial nur Entra ID
- MFA-Erzwingung in Sponsorplatz selbst — wird durch Entra-Conditional-Access geregelt

## 2. Use Cases

| ID | Use Case |
|---|---|
| **UC-SSO-1** | Als CSS-Mitarbeiter logge ich mich mit meinem Entra-Account auf Sponsorplatz ein, ohne separates Passwort zu setzen |
| **UC-SSO-2** | Als bestehender Form-Login-User mit derselben E-Mail wie mein Entra-Konto verknüpfe ich beim ersten SSO-Login automatisch beide Accounts |
| **UC-SSO-3** | Als neuer User wird ein AppUser Just-in-Time aus dem ID-Token-Claim erstellt (mit `email_verifiziert=true`, da Entra die Adresse validiert hat) |
| **UC-SSO-4** | Als Plattform-Admin sehe ich in der Benutzerverwaltung, ob ein User über Entra-SSO oder Form-Login authentifiziert ist |
| **UC-SSO-5** | Als Tenant-Admin gebe ich SSO für eine spezifische Entra-Group frei (Conditional-Access-Policy) |
| **UC-SSO-6** | Als Sicherheitsverantwortlicher kann ich Entra-Group-Membership auf Plattform-Rollen mappen (z. B. Group `sponsorplatz-admins` → `PLATFORM_ADMIN`) |

## 3. Architektur

### 3.1 Stack-Wahl

- **Spring Security OAuth2 Client** (`spring-boot-starter-oauth2-client`, ist im Spring-Boot-Stack enthalten)
- **OpenID Connect Discovery** für automatische Endpoint-Auflösung
- **Authorization Code Flow mit PKCE** (verbindlich — kein Implicit, kein Resource Owner Password)
- **ID-Token-Validierung** via Entra JWKS (auto-discovered)

### 3.2 Coexistence mit Form-Login

```
                ┌─────────────────────────────┐
                │   GET /login                │
                │                             │
                │   ┌─────────┐  ┌─────────┐  │
                │   │ Form    │  │ "Mit    │  │
                │   │ E-Mail  │  │  CSS-   │  │
                │   │ + PW    │  │  Konto" │  │
                │   └─────────┘  └─────────┘  │
                │        │            │       │
                └────────┼────────────┼───────┘
                         │            │
                         ▼            ▼
              SponsorplatzUserDetailsService    OidcUserService
                         │            │
                         └─────┬──────┘
                               ▼
                     ┌─────────────────┐
                     │   AppUser       │
                     │   (DB-backed)   │
                     └─────────────────┘
                               │
                               ▼
                     SecurityContext / Session
```

Beide Pfade landen am gleichen `AppUser` und teilen die gleiche `SecurityContext`-Logik. `AccessControl`, `@PreAuthorize` und `Mitgliedschaft`-Checks bleiben unverändert.

### 3.3 Authorization-Code-Flow (Entra)

```
User → /login → Klick "Mit CSS-Konto"
     → Redirect 302 → https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize
                       ?client_id=...&response_type=code&redirect_uri=...
                       &scope=openid+profile+email
                       &state=...&nonce=...&code_challenge=...
User → Authentifiziert sich bei Entra (inkl. MFA gemäss Conditional Access)
Entra → Redirect 302 → https://sponsorplatz.ch/login/oauth2/code/entra?code=...&state=...
Spring Security → Token-Endpoint-Call → ID-Token + Access-Token
              → JWKS-Verify → OidcUserService.loadUser → AppUser-Lookup/JIT-Provision
              → SecurityContext gefüllt → Redirect zur Original-URL oder /dashboard
```

## 4. Konfiguration

### 4.1 Entra App Registration (CSS IT, Tenant-Admin)

| Feld | Wert |
|---|---|
| App-Type | Web |
| Redirect URIs | `https://sponsorplatz.ch/login/oauth2/code/entra` (prod), `http://localhost:8080/login/oauth2/code/entra` (dev) |
| Logout URI | `https://sponsorplatz.ch/logout` |
| Supported Account Types | Single Tenant (Sponsorplatz initial nur für CSS-Tenant) |
| Client Secret | generiert, in Vault — nicht ins Repo |
| API Permissions | `openid`, `profile`, `email`, optional `User.Read`, `GroupMember.Read.All` für Group-Mapping |
| Token Configuration | optional Group-Claim für Rollen-Mapping |

### 4.2 Spring Boot Properties

`application-prod.properties` (Werte über ENV):

```properties
spring.security.oauth2.client.registration.entra.client-id=${ENTRA_CLIENT_ID}
spring.security.oauth2.client.registration.entra.client-secret=${ENTRA_CLIENT_SECRET}
spring.security.oauth2.client.registration.entra.scope=openid,profile,email
spring.security.oauth2.client.registration.entra.client-authentication-method=client_secret_basic
spring.security.oauth2.client.registration.entra.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.entra.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.entra.client-name=CSS-Konto

spring.security.oauth2.client.provider.entra.issuer-uri=https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0
```

`application-dev.properties` deaktiviert SSO standardmässig — Form-Login reicht für lokale Entwicklung. Optional via Profil `oidc-dev` eine Entra-Test-App.

### 4.3 SecurityConfig-Erweiterung

```java
http.oauth2Login(oauth2 -> oauth2
    .loginPage("/login")
    .userInfoEndpoint(ui -> ui.oidcUserService(sponsorplatzOidcUserService))
    .successHandler(loginSuccessHandler)  // bestehend
    .failureHandler(loginFailureHandler)  // bestehend
);
```

`SponsorplatzOidcUserService` ist die Custom-Implementation, die `OidcUser` auf `AppUser` mappt — siehe Abschnitt 6.

## 5. Datenmodell

### 5.1 Entscheidung: Eigene Tabelle `federierte_identitaet`

Variante A (Spalten am `app_user`) ist simpler, aber blockiert mehrere Provider und mehrere Tenants pro User. Wir nehmen **Variante B** (eigene Tabelle):

### 5.2 Migration V25

```sql
-- =============================================================================
-- V25 — Föderierte Identitäten (OIDC SSO)
-- =============================================================================
-- Speichert die Verknüpfung zwischen einem AppUser und seinem Identity-Provider-
-- Subject. Erlaubt mehrere Provider pro User (z. B. Entra ID + Google, später).
-- =============================================================================

CREATE TABLE federierte_identitaet (
    id                UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider          VARCHAR(50)  NOT NULL,
    subject           VARCHAR(255) NOT NULL,
    email_at_provider VARCHAR(255),
    verbunden_am      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    letzter_login_am  TIMESTAMP,

    CONSTRAINT chk_provider CHECK (provider IN ('ENTRA_ID')),
    CONSTRAINT uq_provider_subject UNIQUE (provider, subject)
);

CREATE INDEX idx_federierte_user_id ON federierte_identitaet(user_id);
```

Erweiterung: bei jedem späteren Provider die `chk_provider`-Constraint ergänzen.

### 5.3 Entity

```java
@Entity
@Table(name = "federierte_identitaet")
public class FederierteIdentitaet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private IdentityProvider provider;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "email_at_provider", length = 255)
    private String emailAtProvider;

    @Column(name = "verbunden_am", nullable = false, updatable = false)
    private Instant verbundenAm;

    @Column(name = "letzter_login_am")
    private Instant letzterLoginAm;
}

public enum IdentityProvider {
    ENTRA_ID
}
```

## 6. Identity-Mapping

### 6.1 Lookup-Logik (`SponsorplatzOidcUserService`)

```java
public OidcUser loadUser(OidcUserRequest request) {
    OidcUser oidc = super.loadUser(request);
    String subject = oidc.getSubject();          // entspricht Entra "oid"-Claim
    String email   = oidc.getEmail();            // muss in scope=email enthalten sein
    String name    = oidc.getFullName();

    // 1. Lookup via (provider, subject) — der stabile Pfad
    Optional<FederierteIdentitaet> identitaet =
        federierteIdentitaetRepository.findByProviderAndSubject(IdentityProvider.ENTRA_ID, subject);

    if (identitaet.isPresent()) {
        AppUser user = identitaet.get().getUser();
        identitaet.get().setLetzterLoginAm(Instant.now());
        return new SponsorplatzOidcUserAdapter(user, oidc);
    }

    // 2. Email-Match auf bestehenden AppUser → Verknüpfung
    Optional<AppUser> bestehend = appUserRepository.findByEmail(email);
    if (bestehend.isPresent()) {
        verknuepfeIdentitaet(bestehend.get(), subject, email);
        return new SponsorplatzOidcUserAdapter(bestehend.get(), oidc);
    }

    // 3. Just-in-Time-Provisionierung
    AppUser neu = createUserFromOidc(oidc);
    verknuepfeIdentitaet(neu, subject, email);
    return new SponsorplatzOidcUserAdapter(neu, oidc);
}
```

### 6.2 Just-in-Time-Provisionierung

Wenn ein User zum ersten Mal via Entra-SSO einloggt und es noch keinen AppUser mit derselben E-Mail gibt:

- Neuer `AppUser` mit:
  - `email` aus ID-Token
  - `anzeigename` aus `name`-Claim
  - `passwortHash = null` oder Marker `"OIDC-ONLY"` (Form-Login bleibt damit blockiert für diesen User)
  - `aktiv = true`
  - `emailVerifiziert = true` (Entra hat die Adresse bereits validiert)
  - `platformRolle = null` (default — Group-Mapping setzt ggf. später)
- Verknüpfter `FederierteIdentitaet`-Eintrag

### 6.3 Account-Verknüpfung (UC-SSO-2)

Bei Email-Match wird automatisch verknüpft. **Achtung:** Das ist eine Take-over-Schwachstelle, falls der Entra-Tenant nicht die E-Mail-Domain kontrolliert. Mitigation:

- Production-Restriction: nur User aus dem CSS-Tenant zugelassen (Single-Tenant-App-Reg)
- Optional: User-UI bei erstem Match zeigt Bestätigungs-Page "Verknüpfen mit bestehendem Konto X@Y.ch?"

Initial gehen wir vom Single-Tenant-Setup aus → automatische Verknüpfung ist sicher.

## 7. Rollen-Mapping

### 7.1 Default

Jeder Entra-User → kein `PlatformRolle`. Org-Mitgliedschaften werden nicht aus Entra abgeleitet — sie werden weiterhin via `EinladungsService` gesetzt.

### 7.2 Group-basiertes Plattform-Rollen-Mapping (optional)

Property:

```properties
sponsorplatz.oidc.rollen-mapping.PLATFORM_ADMIN=sponsorplatz-admins
sponsorplatz.oidc.rollen-mapping.PLATFORM_MODERATOR=sponsorplatz-moderatoren
```

Bei jedem Login werden die `groups`-Claims aus dem ID-Token gegen die Map geprüft. Treffer → `PlatformRolle` setzen, Kein-Treffer → `PlatformRolle = null` (auch bei bestehenden Admins, falls die Group entzogen wurde).

Voraussetzung: Entra-Group-Claim ist im ID-Token enthalten — wird in der App-Registration aktiviert (Token Configuration).

## 8. Sicherheit

| Bedrohung | Mitigation |
|---|---|
| Replay-Attack auf Authorization-Code | PKCE (`code_challenge_method=S256`), state + nonce |
| ID-Token-Tampering | JWKS-Signatur-Verify, automatisch durch Spring Security |
| Token-Exfiltration via Logs | Nicht in Logs schreiben (Spring Logger-Config), nur in Session |
| Session-Fixation | Spring Security Session-Fixation-Protection (default `migrateSession`) |
| Open-Redirect via `redirect_uri` | Nur explizit registrierte URIs in Entra App Registration |
| Account-Take-over via Email-Match | Single-Tenant-Restriction (s. 6.3) |
| Phishing | "Mit CSS-Konto"-Button mit klarem Hinweis, dass es zur `login.microsoftonline.com`-Domain weiterleitet |
| Unverschlüsselte Übertragung | HTTPS-Only via Strict-Transport-Security (bereits in prod gesetzt) |

## 9. Tests (TDD-Pflicht)

Test-IDs werden in `TESTSTRATEGIE.md` ergänzt — siehe Abschnitt SSO unten. Initial:

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **SSO-01** | `OidcLoginFlowIT` | Authorization-Code-Flow mit Mock-Authorization-Server: Login erfolgreich, AppUser via JIT erstellt |
| **SSO-02** | `SponsorplatzOidcUserServiceTest` | Bestehender User mit gleicher E-Mail wird via Email-Match verknüpft (Eintrag in `federierte_identitaet`) |
| **SSO-03** | `SponsorplatzOidcUserServiceTest` | Neuer User wird Just-in-Time erstellt mit `email_verifiziert=true` und `passwort_hash=null` |
| **SSO-04** | `SponsorplatzOidcUserServiceTest` | Subsequent Login findet User direkt via (provider, subject), aktualisiert `letzter_login_am` |
| **SSO-05** | `SecurityConfigTest` | Beide Login-Pfade aktiv: Form-Login UND OAuth2-Login auf `/login`-Page erreichbar |
| **SSO-06** | `SponsorplatzOidcUserServiceTest` | Group-Mapping setzt `PLATFORM_ADMIN`, wenn `groups`-Claim die konfigurierte Group enthält |
| **SSO-07** | `SponsorplatzOidcUserServiceTest` | Group-Mapping entzieht `PLATFORM_ADMIN`, wenn die Group nicht mehr im Claim ist |
| **SSO-08** | `OidcLoginFlowIT` | Ungültige ID-Token-Signatur → 401, kein User wird erstellt, kein DB-Side-Effect |
| **SSO-09** | `OidcLoginFlowIT` | Abgelaufenes ID-Token → 401 |
| **SSO-10** | `LogoutControllerTest` | Logout entfernt Spring-Session lokal (Provider-Logout out of scope für initial) |
| **SSO-11** | `FederierteIdentitaetRepositoryTest` | `findByProviderAndSubject` findet bei Treffer, gibt `Optional.empty()` bei Miss zurück |
| **SSO-12** | `FederierteIdentitaetRepositoryTest` | UNIQUE-Constraint auf (provider, subject) — zweiter Eintrag wirft DataIntegrityViolation |

## 10. Migration / Rollout

### Phase A — Setup (CSS IT, ~2 Tage)

1. Entra App Registration durch CSS IT, Client-ID + Tenant-ID liefern
2. Client-Secret generieren, in Vault ablegen
3. Test-Group `sponsorplatz-pilot` erstellen mit 3-5 Sponsoring-Team-Mitgliedern

### Phase B — Implementation (~5 Tage)

1. Spec-Update + Tests SSO-01..12 schreiben (rot)
2. Migration V25 + Entity + Repository + Tests grün
3. `SponsorplatzOidcUserService` + Mapping-Logik
4. `SecurityConfig`-Erweiterung
5. UI: Button "Mit CSS-Konto anmelden" auf `/login`
6. Admin-UI: User-Detail zeigt verknüpfte Identitäten

### Phase C — Pilot (~2 Wochen)

1. Deploy auf Staging mit `oidc`-Profil
2. Pilot-Group testet End-to-End-Flow
3. Issues sammeln, Fixes deployen
4. Sicherheits-Review (Pen-Test der OIDC-Endpunkte)

### Phase D — Rollout

1. Production-Deploy mit `oidc`-Profil
2. Alle CSS-Tenant-Mitglieder können SSO nutzen
3. Optional: Form-Login deaktivieren für CSS-Domain-User (`@css.ch`-Domain → SSO erzwingen)

## 11. Offene Entscheidungen

| Frage | Optionen | Empfehlung |
|---|---|---|
| Account-Verknüpfung automatisch oder mit User-Confirmation? | A: Auto-Link bei Email-Match. B: Confirmation-Page | **A** für Single-Tenant-Setup, **B** wenn später Multi-Tenant |
| Welche Entra-Groups → welche Plattform-Rollen? | offen | Klärung mit CSS IT + Sponsoring-Lead nötig |
| Form-Login für SSO-User komplett blockieren? | A: Beide aktiv. B: SSO-User können sich nicht mehr per Form anmelden | **B** für SSO-Only-User (passwort_hash=null verhindert Form-Login bereits) |
| `letzter_login_am` updaten — synchron oder async? | A: Synchron in Service. B: Async via Event | **A** initial, **B** falls Performance-Hotspot |
| Logout-Verhalten | A: Nur lokal. B: Provider-Logout via end_session_endpoint | **A** initial, **B** als Backlog |

## 12. Referenzen

- [Spring Security 6 — OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Spring Security 6 — OIDC](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)
- [Microsoft Identity Platform — Authentication Flows](https://learn.microsoft.com/entra/identity-platform/authentication-flows-app-scenarios)
- [Microsoft Identity Platform — App Registration](https://learn.microsoft.com/entra/identity-platform/quickstart-register-app)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [PKCE — RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636)

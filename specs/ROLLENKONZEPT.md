# Rollenkonzept

> Dieses Dokument ist die implementierungs-nahe Spec. High-Level-Kontext + Hintergrund: [`docs/konzept.md`](../docs/konzept.md) §4. Stand 26.05.2026 — synchron mit dem aktuellen Code.

## Modell: Kollaborativ

- Mehrere Organisationen teilen eine offene Datenbasis
- Alle eingeloggten Benutzer sehen alle Daten
- **Edit-Rechte** über `mitgliedschaft` definiert
- **Auth-Identifikator:** `auth.getName()` ist die **Email** (Form-Login wie OIDC). `SponsorplatzOidcUserService` setzt `nameAttributeKey="email"` damit das konsistent bleibt.

## Globale Plattform-Rollen

Definiert in `ch.sponsorplatz.benutzer.PlatformRolle`. Spring-Authority-Form `ROLE_PLATFORM_ADMIN` etc.

| Rolle | Zweck |
|---|---|
| `PLATFORM_ADMIN` | Plattform-Betrieb, Verifizierung, Lösch-Workflows, Ops-Dashboard, Backup/Restore |
| `PLATFORM_MODERATOR` | Inhalte moderieren, Orgs suspendieren (heute teilweise unter PLATFORM_ADMIN, Trennschärfe in Backlog) |
| `PLATFORM_SUPPORT` | Read-only zur Anwender-Hilfe (heute teilweise unter PLATFORM_ADMIN) |

## Organisations-Rollen (per Mitgliedschaft)

Definiert in `ch.sponsorplatz.organisation.Rolle`. Keine `SPONSOR_KONTAKT`-Rolle mehr — Sponsor-Org-Mitglieder nutzen direkt `ORG_OWNER`/`ORG_EDITOR` ihrer Sponsor-Org.

| Rolle | Rechte (in dieser Org) |
|---|---|
| `ORG_OWNER` | Alles + Mitglieder-Verwaltung + Org-Löschung |
| `ORG_EDITOR` | CRUD auf Inhalte (Projekte, Pakete, Anfragen, Medien) |
| `ORG_VIEWER` | Nur lesen + Sichtbarkeits-Marker + Aufgaben-Sicht (Reporting für Vorstand) |

## Implementation: `AccessControl`-Bean

Liegt in `ch.sponsorplatz.organisation.AccessControl`. Vier Public-Methoden + zwei Slug-Varianten. **Hierarchische Vererbung:** ein ORG_OWNER/ORG_EDITOR einer Eltern-Org hat implizit dieselben Rechte auf alle Kind-Orgs — wird via rekursiver CTE in einem Query gelöst (`MitgliedschaftRepository.zaehleMitgliedschaftenInHierarchie`).

```java
@Component("accessControl")
public class AccessControl {

    public boolean kannOrgEditieren(UUID orgId, Authentication auth) {
        if (!istAuthentifiziert(auth)) return false;
        if (istPlattformAdmin(auth)) return true;
        return findeUserId(auth)
                .map(userId -> hatBerechtigungMitVererbung(
                        userId, orgId, Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR)))
                .orElse(false);
    }

    public boolean kannOrgVerwalten(UUID orgId, Authentication auth) {
        // wie oben, nur Set.of(Rolle.ORG_OWNER)
    }

    public boolean kannOrgEditierenNachSlug(String slug, Authentication auth) { ... }
    public boolean kannOrgVerwaltenNachSlug(String slug, Authentication auth) { ... }
}
```

User-Identifikation: `appUserRepository.findByEmail(auth.getName())` — der OIDC-Pfad liefert ebenfalls die Email (siehe `SponsorplatzOidcUserService.loadUser` mit `nameAttributeKey="email"`).

Verwendung: **programmatisch** in Controllern (kein SpEL-Bean-Reference):
```java
@GetMapping("/{slug}/bearbeiten")
public String bearbeiten(@PathVariable String slug, Authentication auth) {
    if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
        throw new AccessDeniedException("...");
    }
    ...
}
```

## Permission-Matrix (Auszug)

`*` = nur in Orgs der Person; `(V)` = Org-Typ VEREIN; `(U)` = Org-Typ UNTERNEHMEN.

| Aktion | anonym | eingeloggt | ORG_VIEWER | ORG_EDITOR | ORG_OWNER | PLATFORM_ADMIN |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Public-Projekt ansehen | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Marktplatz / Verein-Profil ansehen | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `/organisationen`-Liste sehen | ✓ alle | ✓ nur eigene | ✓ nur eigene | ✓ nur eigene | ✓ nur eigene | ✓ alle |
| Sponsor-Anfrage stellen (Paket-Bezug) | – | – | – | ✓\* | ✓\* | ✓ |
| Kontakt-Anfrage Verein → Sponsor | – | – | – | ✓\*(V) | ✓\*(V) | – |
| Eingehende Anfragen ansehen | – | ✓ | ✓\* | ✓\* | ✓\* | ✓ |
| Ausgehende Anfragen ansehen | – | – | – | ✓\*(V) | ✓\*(V) | ✓ |
| Anfrage annehmen / ablehnen | – | – | – | ✓\* | ✓\* | ✓ |
| Sponsor anlegen | – | – | – | ✓\* | ✓\* | ✓ |
| Sponsor bearbeiten (eigener) | – | – | – | ✓\* | ✓\* | ✓ |
| Sponsor bearbeiten (fremder) | – | – | – | – | – | ✓ |
| Mitglieder verwalten | – | – | – | – | ✓\* | ✓ |
| Org verifizieren / suspendieren | – | – | – | – | – | ✓ |
| User sperren / entsperren | – | – | – | – | – | ✓ |
| Datei-Anhänge hochladen | – | – | – | ✓\* | ✓\* | ✓ |
| Datei-Anhänge / Bilder löschen | – | – | – | ✓\* | ✓\* | ✓ |
| Profilbild eigenes Konto | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| Onboarding-Wizard sehen | – | ✓ wenn ohne Org | – | – | – | – |
| Support-Anfrage stellen | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| Audit-Log lesen | – | – | – | – | – | ✓ |
| Audit-Datenexport pro User | – | ✓ eigene | ✓ eigene | ✓ eigene | ✓ eigene | ✓ alle |
| Backup erstellen / restore (DB + Files) | – | – | – | – | – | ✓ |
| Aufgaben-Definitionen pflegen (`/admin/aufgaben-definitionen`) | – | – | – | – | – | ✓ |
| `/aufgaben` öffnen (eigene Tasks abarbeiten) | – | ✓ leer | ✓\* | ✓\* | ✓\* | ✓ alle Admin-Tasks |
| Vertrag erstellen / unterzeichnen | – | – | – | ✓\* | ✓\* | ✓ |
| Vertrag kündigen | – | – | – | – | ✓\* | ✓ |
| Rechnung erzeugen + PDF herunterladen | – | – | – | ✓\* | ✓\* | ✓ |
| 2FA aktivieren / deaktivieren (eigenes Konto) | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2FA-Backup-Codes regenerieren | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2FA-Reset für anderen User | – | – | – | – | – | ✓ |
| OIDC-Login (Google/Entra/SwissID/edu-ID) | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| Eigene IdP-Verknüpfungen ansehen | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| IdP-Anzeige in `/admin/benutzer`-Liste | – | – | – | – | – | ✓ |
| Ops-Dashboard (Recent Errors, Heap, DB) | – | – | – | – | – | ✓ |
| REST-API (`/api/**` mit `X-API-Key`) | service-level | service-level | service-level | service-level | service-level | service-level |

**Aufgaben-Sichtbarkeit (Phase 12)**: Eine Aufgabe ist für einen User sichtbar, wenn
entweder (a) `aufgabe.assignee_org_id` zu einer seiner Mitgliedschaften gehört —
jede Org-Rolle reicht, weil ein VIEWER (oft der Vorstand) ebenfalls Reporting auf
offene Aufgaben braucht — oder (b) `aufgabe.nur_platform_admin = true` und der
User PLATFORM_ADMIN ist. Die Aufgabe kann **immer** manuell als erledigt
markiert werden, sobald sie sichtbar ist; Auto-Erledigung läuft separat über
`AufgabenEngine` beim Status-Wechsel der zugrundeliegenden Entity.

**REST-API-Authentifizierung (Phase 14)**: `/api/**` ist über `ApiKeyFilter`
geschützt und ignoriert die normale User-Session. Header `X-API-Key` muss zu
`sponsorplatz.api.key` matchen. Ohne konfigurierten Key antwortet der Filter
mit 503 (off-by-default). Brute-Force-Schutz via `RateLimitFilter` (IP-basiert,
liegt vor `ApiKeyFilter` im Filter-Chain).

## Endpoint-Schutz: Welcher Endpunkt ruft welche AccessControl-Methode?

Zwei Schutz-Stile parallel im Einsatz:

1. **Deklarativ via `@PreAuthorize`** für statische Authority-Checks (`isAuthenticated()`, `hasRole('PLATFORM_ADMIN')`) — z.B. alle `/admin/**`-Controller, `/aufgaben`, `/einstellungen`.
2. **Programmatisch via `AccessControl`-Aufruf** für kontextabhängige Org-Rollen-Checks — z.B. `accessControl.kannOrgEditierenNachSlug(slug, auth)` am Anfang der Controller-Methode. Bei `false` → `throw new AccessDeniedException(...)`. Der `GlobalExceptionHandler` mappt diese auf 403.

> Hinweis: SpEL-Bean-Referenzen `@PreAuthorize("@accessControl.method(...)")` werden NICHT genutzt. Unter früherem Spring Boot 3.4.1 + Java 25 + Mockito-Subclass-Mocks gab es nicht reproduzierbare „Failed to evaluate expression"-Symptome. Programmatische Checks sind genauso testbar (AccessControl ist als `@MockBean` verfügbar), vermeiden Compile-Flag-Abhängigkeiten (`-parameters`) und sind im Stack-Trace einfacher zu debuggen.

### Public + Auth-Einstieg

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/`, `/kontakt`, `/impressum`, `/datenschutz`, `/agb`, `/fuer-marken`, `/fuer-vereine` | public | Marketing-/Legal-Seiten |
| GET | `/sitemap.xml` | public | SEO — gerendert aus published Projekten + Orgs |
| GET | `/marktplatz/**` | public | Public-Liste + Detail-Seiten (Projekt-Slug, Org-Slug); SEO-optimiert mit Schema.org + OG-Tags |
| GET, POST | `/login` | public | Form-Login |
| GET, POST | `/login/2fa` | public, **State-gebunden** | TOTP/Backup-Code-Eingabe NACH Erst-Login mit Passwort; HTTP-Session-Stash hält den teil-authentifizierten User zwischen Schritt 1 und 2 |
| GET | `/oauth2/authorization/{registrationId}` | public | Spring-Security-Initiation für OIDC-Provider (entra/google/swissid/edu) |
| GET | `/login/oauth2/code/{registrationId}` | public | OIDC-Callback; `SponsorplatzOidcUserService.loadUser` macht Subject-Lookup → Email-Match → JIT-Provisioning |
| POST | `/logout` | `isAuthenticated()` | RP-initiated wenn `ClientRegistrationRepository` vorhanden, sonst lokales Session-Kill mit Redirect `/` |
| GET, POST | `/registrieren`, `/verifizieren` | public | Form-Login-Self-Reg + Mail-Verifizierung |
| GET, POST | `/sponsor/registrieren` | public | Self-Reg für Sponsor-Org + auto ORG_OWNER-Mitgliedschaft |
| GET, POST | `/passwort-vergessen`, `/passwort-reset` | public | Token-basierter Reset (Token 1h gültig) |
| GET, POST | `/einladung/annehmen` | public (Token in URL) | Mail-Link ist GET; POST = Annahme. K3-Fix verhindert CSRF-Replay |
| GET, POST | `/onboarding/**` | `isAuthenticated()` | Verein-Quick-Create + Einladungs-Token-Eingabe; `DashboardController` leitet User ohne Mitgliedschaft hierhin |
| GET, POST | `/support` | `isAuthenticated()` | Support-Mail-Form an `sponsorplatz.support.empfaenger` |

### Eigenes Konto + 2FA

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET, POST | `/einstellungen` | `isAuthenticated()` | Profil-Editor (Anzeigename, Email, Sprache, Profilbild) |
| GET, POST | `/einstellungen/2fa` | `isAuthenticated()` | TOTP-Setup-Flow + Aktivierung/Deaktivierung; zeigt QR-Code |
| POST | `/einstellungen/2fa/backup-codes/regenerieren` | `isAuthenticated()` + 2FA-aktiviert | Wirft alte Codes weg, erzeugt 10 neue (BCrypt-gehashed) |
| GET | `/einstellungen/datenexport` | `isAuthenticated()` | DSG-Datenexport: alle eigenen Audit-Events + AppUser-Daten als JSON-Download |

### Organisationen + Mitgliedschaften

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/organisationen` | public, **ergebnis-gefiltert** | Anonyme: alle Orgs; Eingeloggte (nicht-Admin): nur Orgs mit eigener Mitgliedschaft (jede Rolle); Plattform-Admins: alle Orgs. Implementierung in `OrganisationController.liste(Authentication)`. Filter via `?typ=…&status=…&branche=…&q=…` |
| GET | `/organisationen/{slug}` | public | Profil lesbar |
| GET | `/organisationen/neu` | `isAuthenticated()` | nur eingeloggt anlegen |
| POST | `/organisationen` (Create) | `isAuthenticated()` | Anlegender wird automatisch ORG_OWNER (Owner-on-Create-Pfad in `OrganisationService.erstelleMitEigentuemer`) |
| POST | `/organisationen/{slug}` (Update) | `kannOrgEditierenNachSlug(#slug)` | Edit-Recht; Slug aus URL, kein `id` im Body (K3-Fix) |
| GET | `/organisationen/{slug}/bearbeiten` | `kannOrgEditierenNachSlug(#slug)` | Edit-Form sichtbar nur für Editor+ |
| POST | `/organisationen/{slug}/loeschen` | `kannOrgVerwaltenNachSlug(#slug)` | nur Owner / Admin |
| GET | `/organisationen/{slug}/mitglieder` | `kannOrgEditierenNachSlug(#slug)` | Mitgliederliste = sensibel |
| POST | `/organisationen/{slug}/mitglieder/hinzufuegen` | `kannOrgVerwaltenNachSlug(#slug)` | Owner-Aktion |
| POST | `/organisationen/{slug}/mitglieder/{id}/entfernen` | `kannOrgVerwaltenNachSlug(#slug)` | Owner-Aktion |

### Projekte + Pakete + Watchlist

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/organisationen/{orgSlug}/projekte` | `kannOrgEditierenNachSlug(#orgSlug)` | interner View (Entwürfe etc.) |
| GET | `/organisationen/{orgSlug}/projekte/neu` | `kannOrgEditierenNachSlug(#orgSlug)` | Editor+ |
| POST | `/organisationen/{orgSlug}/projekte/speichern` | `kannOrgEditierenNachSlug(#orgSlug)` | Editor+ |
| GET | `/organisationen/{orgSlug}/projekte/{projektSlug}` | `kannOrgEditierenNachSlug(#orgSlug)` | interner Detail-View |
| POST | `/organisationen/{orgSlug}/projekte/{projektSlug}/veroeffentlichen` | `kannOrgEditierenNachSlug(#orgSlug)` | Editor+ |
| POST | `/organisationen/{orgSlug}/projekte/{projektSlug}/pakete/speichern` | `kannOrgEditierenNachSlug(#orgSlug)` | Editor+ |
| GET | `/meine-projekte` | `isAuthenticated()` | Aggregierte Sicht über alle Orgs des Users |
| POST | `/projekte/{projektSlug}/watchlist/hinzufuegen` / `…/entfernen` | `isAuthenticated()` | Watchlist ist user-eigen, kein Org-Bezug |

### Sponsoring-Anfragen + Konversation

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/anfragen` | `isAuthenticated()`, **ergebnis-rollenabhängig** | Eingehende für alle eigenen Orgs. Vereins-Mitglieder (mind. eine `OrgTyp.VEREIN`-Mitgliedschaft mit Edit-Recht) sehen zusätzlich ausgehende + bekommen den "Sponsor anfragen"-Button. Sponsoren-only-User (`OrgTyp.UNTERNEHMEN`) sehen nur eingehende. |
| GET | `/anfragen/neu?paketId=…` | `isAuthenticated()` | Sponsor-Form für Paket-Anfrage (Marktplatz-Detail-Flow); `kannOrgEditieren(anfragenderOrg)` beim POST |
| POST | `/anfragen/erstellen` | `kannOrgEditieren(anfragenderOrgId)` | Anfragender muss Edit-Recht haben; Empfänger wird vom Paket abgeleitet (kein Client-Trust) |
| GET | `/anfragen/neu-kontakt` | Verein-Mitglied mit Edit-Recht | Sponsor-Picker für Verein→Sponsor-Kontaktanfrage. `OrgTyp.VEREIN`-Whitelist im Controller |
| POST | `/anfragen/kontakt-erstellen` | Verein-Mitglied mit Edit-Recht; Empfänger muss `OrgTyp.UNTERNEHMEN` sein | Verbietet Sponsor→Sponsor sowie Self-Anfrage |
| POST | `/anfragen/{id}/annehmen` \| `/ablehnen` | `kannOrgEditieren(empfaengerOrg)` | IDOR-Schutz auf Empfänger-Org der Anfrage |
| GET | `/meine-anfragen` | `isAuthenticated()` | Eigene ausgehende Anfragen (Sponsor-Sicht) |

### Vertrag + Rechnung

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| POST | `/anfragen/{id}/vertrag/erstellen` | `kannOrgEditierenNachSlug(empfaengerSlug)` | Vertrag-Generator nach Anfrage-Annahme |
| GET | `/vertraege/{id}` | `kannOrgEditierenNachSlug(orgSlug)` (Eigentümer der Anfrage) | Detail-View |
| GET | `/vertraege/{id}/pdf` | `kannOrgEditierenNachSlug(orgSlug)` | PDF-Download |
| POST | `/vertraege/{id}/unterzeichnen` | `kannOrgEditierenNachSlug(orgSlug)` | Status-Wechsel |
| POST | `/vertraege/{id}/kuendigen` | `kannOrgVerwaltenNachSlug(orgSlug)` | OWNER-Aktion (Cut) |
| POST | `/vertraege/{vertragId}/rechnung/erstellen` | `kannOrgEditierenNachSlug(orgSlug)` | Rechnungserzeugung mit Swiss-QR-Bill |
| GET | `/rechnungen/{id}/pdf` | `kannOrgEditierenNachSlug(orgSlug)` | PDF-Download |

### Medien-Auslieferung + Upload

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/medien/{id}` | public | Inline-Bild oder Attachment-Download (RFC-5987-encoded Filename); `StorageObjectNotFoundException` → 404, kein 500-Stacktrace |
| POST | `/medien/{id}/loeschen` | typabhängig | ORGANISATION-Asset → `kannOrgEditieren` der Org; PROJEKT-Asset → `kannOrgEditieren(p.org)`; USER-Asset → nur der User selbst |

### Aufgaben

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/aufgaben` | `isAuthenticated()`, **sichtbarkeits-gefiltert** | Liefert nur Aufgaben, deren `assignee_org_id` zu einer Mitgliedschaft des Users gehört, plus (für PLATFORM_ADMIN) Tasks mit `nur_platform_admin=true`. Implementiert in `AufgabenService.meineOffenen` über `AufgabeRepository.findOffeneFuer(orgIds, istAdmin)`. |
| POST | `/aufgaben/{id}/erledigen` | `isAuthenticated()` + Sichtbarkeitsprüfung | IDOR-Schutz: `AufgabenService.darfSehen(a, user)` wirft `AccessDeniedException`, wenn die Aufgabe weder zu den Org-Mitgliedschaften noch zum Admin-Profil des Users passt. |

### Dashboard + Benachrichtigungen + Events

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/dashboard` | `isAuthenticated()` | Aggregierte Sicht; User ohne Mitgliedschaft wird nach `/onboarding` redirected |
| GET, POST | `/benachrichtigungen/**` | `isAuthenticated()` | In-App-Glocke + "als gelesen markieren"; `empfaenger_id` als implizite Filter-Achse (keine Cross-User-Lesung möglich) |
| GET, POST | `/event/**` | `kannOrgEditieren(orgId)` für Edit/Delete | Org-eigene Events; Lesen für alle eingeloggten User |

### Admin (`/admin/**`)

Alle Admin-Pfade sind über `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` auf Klassen-Ebene geschützt.

| HTTP | Pfad | Begründung |
|---|---|---|
| GET | `/admin` | Admin-Dashboard mit System-Snapshot |
| GET | `/admin/verifizierungen` | Pending-Orgs-Queue + Verifizieren-Button (Zefix oder manuell) |
| GET, POST | `/admin/benutzer` + `/admin/benutzer/{id}/{rolle,sperren,entsperren,2fa-reset}` | Benutzer-Verwaltung inkl. IdP-Anzeige + 2FA-Recovery |
| GET, POST | `/admin/audit` + `/admin/audit/datenexport` | Audit-Log-Viewer + DSG-Export |
| GET, POST | `/admin/backups` + `/admin/backups/{erstellen,restore,{name}/download,{name}/loeschen}` | DB-Dump (pg_dump) |
| GET, POST | `/admin/datei-backups` + entsprechende Operationen | Datei-Backup als ZIP (alle MedienAssets) |
| GET, POST | `/admin/aufgaben-definitionen/**` | CRUD auf Workflow-Vorlagen; System-Defs sind nicht löschbar (`IllegalStateException`); Trigger-Felder sind im Form gesperrt |
| GET, POST | `/admin/backlog/**` | Backlog-Items + Phase-Tracking |
| GET, POST | `/admin/style` | Style-Switcher (klassisch / dashboard-modern), persistiert in `plattform_einstellung` |
| GET, POST | `/admin/mail-einstellungen` | SMTP-Config (Live/Sandbox), Test-Empfänger |
| GET | `/admin/system` | Heap, DB-Stats, Bucket-Stats |
| GET | `/admin/recent-errors` | Letzte ERROR-Logbacks (in-memory Buffer) |

### Ops + REST-API

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/ops/**` | `hasRole('PLATFORM_ADMIN')` | Ops-Dashboard (RecentErrors, Heap, DB-Stats) |
| `*` | `/api/**` | `ApiKeyFilter` (`X-API-Key`-Header) | Off-by-default (503 ohne konfigurierten Key); RateLimitFilter davor |
| GET | `/actuator/health` | public auf Loopback:9090 | Container-Healthcheck; in prod NICHT von außen erreichbar |

**Anti-Pattern vermieden:** Keine `@Secured`-Annotationen mit Hardcoded-Rollen — Org-Rollen sind kontextabhängig (pro Org), das geht nur über die `AccessControl`-Bean (programmatisch). Für globale Plattform-Rollen ist `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` der Standard.

## AccessControl-Slug-Varianten

```java
public boolean kannOrgEditierenNachSlug(String slug, Authentication auth) {
    return organisationRepository.findBySlug(slug)
            .map(org -> kannOrgEditieren(org.getId(), auth))
            .orElse(false);
}

public boolean kannOrgVerwaltenNachSlug(String slug, Authentication auth) {
    return organisationRepository.findBySlug(slug)
            .map(org -> kannOrgVerwalten(org.getId(), auth))
            .orElse(false);
}
```

Unbekannter Slug → `false` (führt zu 403, nicht 404; das ist korrekt — der Auth-Layer leakt keine Existenz-Information). Die anschliessende Controller-Logik wirft den 404 erst, wenn AccessControl bereits durchgewunken hat.

# Rollenkonzept

> Ausführliche Variante: siehe `Sponsoring Plattform/05_Rollenkonzept.md` im Konzept-Workspace.
> Dieses Dokument ist die Spec-Variante — kompakt, implementierungs-nah.

## Modell: Kollaborativ

- Mehrere Organisationen teilen eine offene Datenbasis
- Alle eingeloggten Benutzer sehen alle Daten
- **Edit-Rechte** über `mitgliedschaft` definiert

## Globale Plattform-Rollen

| Rolle | Zweck |
|---|---|
| `PLATFORM_ADMIN` | Plattform-Betrieb, Verifizierung, Lösch-Workflows |
| `PLATFORM_MODERATOR` | Inhalte moderieren, Orgs suspendieren |
| `PLATFORM_SUPPORT` | Read-only zur Anwender-Hilfe |

## Organisations-Rollen (per Mitgliedschaft)

| Rolle | Rechte (in dieser Org) |
|---|---|
| `ORG_OWNER` | Alles + Mitglieder-Verwaltung |
| `ORG_EDITOR` | CRUD auf Inhalte |
| `ORG_VIEWER` | Nur lesen + Sichtbarkeits-Marker |

## Implementation: `AccessControl`-Bean

```java
@Component("accessControl")
public class AccessControl {

    public boolean kannOrgEditieren(UUID orgId, Authentication auth) {
        if (!authenticated(auth)) return false;
        if (istPlattformAdmin(auth)) return true;
        return mitgliedschaftRepo.existsByUserAndOrgAndRolleIn(
            auth.getName(), orgId, Set.of(ORG_OWNER, ORG_EDITOR));
    }

    public boolean kannOrgVerwalten(UUID orgId, Authentication auth) {
        if (!authenticated(auth)) return false;
        if (istPlattformAdmin(auth)) return true;
        return mitgliedschaftRepo.existsByUserAndOrgAndRolle(
            auth.getName(), orgId, ORG_OWNER);
    }
}
```

Verwendung:
```java
@PreAuthorize("@accessControl.kannOrgEditieren(#orgId, authentication)")
public void aktualisiere(UUID orgId, ...) { ... }
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
| `/aufgaben` öffnen (eigene Tasks abarbeiten) | – | ✓ leer | ✓\* | ✓\* | ✓\* | ✓ alle Admin-Tasks |
| Aufgaben-Definitionen pflegen (`/admin/aufgaben-definitionen`) | – | – | – | – | – | ✓ |

**Aufgaben-Sichtbarkeit (Phase 12)**: Eine Aufgabe ist für einen User sichtbar, wenn
entweder (a) `aufgabe.assignee_org_id` zu einer seiner Mitgliedschaften gehört —
jede Org-Rolle reicht, weil ein VIEWER (oft der Vorstand) ebenfalls Reporting auf
offene Aufgaben braucht — oder (b) `aufgabe.nur_platform_admin = true` und der
User PLATFORM_ADMIN ist. Die Aufgabe kann **immer** manuell als erledigt
markiert werden, sobald sie sichtbar ist; Auto-Erledigung läuft separat über
`AufgabenEngine` beim Status-Wechsel der zugrundeliegenden Entity.

Vollständige Konzept-Matrix in `Sponsoring Plattform/05_Rollenkonzept.md`.

## Endpoint-Schutz: Welcher Endpunkt ruft welche AccessControl-Methode?

Alle mutierenden Endpunkte rufen am Anfang der Controller-Methode `accessControl.kannOrgEditierenNachSlug(slug, auth)` bzw. `kannOrgVerwaltenNachSlug(slug, auth)` auf und werfen `AccessDeniedException` bei `false`. Der `GlobalExceptionHandler` mappt diese auf 403.

> Hinweis: Wir haben bewusst auf `@PreAuthorize` mit SpEL-Bean-Referenz verzichtet. Die SpEL-Auswertung von `@accessControl.method(#slug, authentication)` zeigte unter Spring Boot 3.4.1 + Java 25 + Mockito-Subclass-Mocks eine nicht reproduzierbare „Failed to evaluate expression"-Symptomatik. Programmatische Checks sind genauso testbar (AccessControl ist als `@MockBean` verfügbar) und vermeiden Compile-Flag-Abhängigkeiten (`-parameters`).

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/organisationen` | public, **ergebnis-gefiltert** | Anonyme: alle Orgs; Eingeloggte (nicht-Admin): nur Orgs mit eigener Mitgliedschaft (jede Rolle); Plattform-Admins: alle Orgs. Implementierung in `OrganisationController.liste(Authentication)`. |
| GET | `/organisationen/{slug}` | public | Profil lesbar |
| GET | `/organisationen/neu` | `isAuthenticated()` | nur eingeloggt anlegen |
| POST | `/organisationen` (Create) | `isAuthenticated()` | Anlegender wird automatisch ORG_OWNER (Owner-on-Create-Pfad in `OrganisationService.erstelleMitEigentuemer`) |
| POST | `/organisationen/{slug}` (Update) | `kannOrgEditierenNachSlug(#slug)` | Edit-Recht; Slug aus URL, kein `id` im Body (K3-Fix) |
| GET | `/organisationen/{slug}/bearbeiten` | `kannOrgEditierenNachSlug(#slug)` | Edit-Form sichtbar nur für Editor+ |
| POST | `/organisationen/{slug}/loeschen` | `kannOrgVerwaltenNachSlug(#slug)` | nur Owner / Admin |
| GET | `/organisationen/{slug}/mitglieder` | `kannOrgEditierenNachSlug(#slug)` | Mitgliederliste = sensibel |
| POST | `/organisationen/{slug}/mitglieder/hinzufuegen` | `kannOrgVerwaltenNachSlug(#slug)` | Owner-Aktion |
| POST | `/organisationen/{slug}/mitglieder/{id}/entfernen` | `kannOrgVerwaltenNachSlug(#slug)` | Owner-Aktion |
| GET | `/organisationen/{orgSlug}/projekte` | `kannOrgEditierenNachSlug(#orgSlug)` | interner View (Entwürfe etc.) |
| GET | `/organisationen/{orgSlug}/projekte/neu` | `kannOrgEditierenNachSlug(#orgSlug)` | Editor+ |
| POST | `/organisationen/{orgSlug}/projekte/speichern` | `kannOrgEditierenNachSlug(#orgSlug)` | Editor+ |
| GET | `/organisationen/{orgSlug}/projekte/{projektSlug}` | `kannOrgEditierenNachSlug(#orgSlug)` | interner Detail-View |
| POST | `/organisationen/{orgSlug}/projekte/{projektSlug}/veroeffentlichen` | `kannOrgEditierenNachSlug(#orgSlug)` | Editor+ |
| POST | `/organisationen/{orgSlug}/projekte/{projektSlug}/pakete/speichern` | `kannOrgEditierenNachSlug(#orgSlug)` | Editor+ |
| GET | `/anfragen` | `isAuthenticated()`, **ergebnis-rollenabhängig** | Eingehende für alle eigenen Orgs. Vereins-Mitglieder (mind. eine `OrgTyp.VEREIN`-Mitgliedschaft mit Edit-Recht) sehen zusätzlich ausgehende + bekommen den "Sponsor anfragen"-Button. Sponsoren-only-User (`OrgTyp.UNTERNEHMEN`) sehen nur eingehende. |
| POST | `/anfragen/{id}/annehmen|ablehnen` | `kannOrgEditieren(empfaengerOrg)` | IDOR-Schutz auf Empfänger-Org der Anfrage |
| GET | `/anfragen/neu?paketId=…` | `isAuthenticated()` | Sponsor-Form für Paket-Anfrage (Marktplatz-Detail-Flow); `kannOrgEditieren(anfragenderOrg)` beim POST |
| POST | `/anfragen/erstellen` | `kannOrgEditieren(anfragenderOrgId)` | Anfragender muss Edit-Recht haben; Empfänger wird vom Paket abgeleitet (kein Client-Trust) |
| GET | `/anfragen/neu-kontakt` | Verein-Mitglied mit Edit-Recht | Sponsor-Picker für Verein→Sponsor-Kontaktanfrage. `OrgTyp.VEREIN`-Whitelist im Controller |
| POST | `/anfragen/kontakt-erstellen` | Verein-Mitglied mit Edit-Recht; Empfänger muss `OrgTyp.UNTERNEHMEN` sein | Verbietet Sponsor→Sponsor sowie Self-Anfrage |
| GET, POST | `/medien/{id}` (Auslieferung) | public | Inline-Bild oder Attachment-Download (RFC-5987-encoded Filename) |
| POST | `/medien/{id}/loeschen` | typabhängig | ORGANISATION-Asset → `kannOrgEditieren` der Org; PROJEKT-Asset → `kannOrgEditieren(p.org)`; USER-Asset → nur der User selbst |
| GET, POST | `/onboarding/**` | `isAuthenticated()`, redirected wenn keine Mitgliedschaft | Verein-Quick-Create + Einladungs-Token-Eingabe; DashboardController leitet User ohne Mitgliedschaft hierhin |
| GET, POST | `/support` | `isAuthenticated()` | Support-Mail-Form an `sponsorplatz.support.empfaenger` |
| GET, POST | `/sponsor/registrieren` | public | Self-Reg für Sponsor-Org + ORG_OWNER-Mitgliedschaft |
| GET | `/marktplatz/**` | public | Public-Marktplatz |
| GET, POST | `/login`, `/registrieren`, `/verifizieren` | public | Auth-Flows |
| GET, POST | `/passwort-vergessen`, `/passwort-reset` | public | Passwort-Reset-Flow (Token, 1h gültig) |
| GET, POST | `/einladung/annehmen` | public (GET = Vorschau, POST = Annahme — K3-Fix) | Token in URL akzeptiert, weil Mail-Link auch GET ist |
| GET | `/admin/**` | `hasRole('PLATFORM_ADMIN')` | Plattform-Admin-Tools (Verifizierung, Audit, Backups, Backlog, System, Aufgaben-Definitionen) |
| GET | `/aufgaben` | `isAuthenticated()`, **sichtbarkeits-gefiltert** | Liefert nur Aufgaben, deren `assignee_org_id` zu einer Mitgliedschaft des Users gehört, plus (für PLATFORM_ADMIN) Tasks mit `nur_platform_admin=true`. Implementiert in `AufgabenService.meineOffenen` über `AufgabeRepository.findOffeneFuer(orgIds, istAdmin)`. |
| POST | `/aufgaben/{id}/erledigen` | `isAuthenticated()` + Sichtbarkeitsprüfung | IDOR-Schutz: `AufgabenService.darfSehen(a, user)` wirft `AccessDeniedException`, wenn die Aufgabe weder zu den Org-Mitgliedschaften noch zum Admin-Profil des Users passt. |
| GET, POST | `/admin/aufgaben-definitionen/**` | `hasRole('PLATFORM_ADMIN')` | CRUD auf Workflow-Vorlagen; System-Defs sind nicht löschbar (Service wirft `IllegalStateException`) und Trigger-Felder sind im Form gesperrt, damit die im Code verdrahteten Service-Trigger nicht ins Leere zeigen. |

**Anti-Pattern vermieden:** Keine `@Secured`-Annotationen mit Hardcoded-Rollen — Org-Rollen sind kontextabhängig (pro Org), das geht nur über die `AccessControl`-Bean (programmatisch oder SpEL).

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

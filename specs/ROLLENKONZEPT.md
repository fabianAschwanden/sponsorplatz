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

| Aktion | anonym | eingeloggt | ORG_VIEWER | ORG_EDITOR | ORG_OWNER | PLATFORM_ADMIN |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Public-Projekt ansehen | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| CRM-Daten ansehen | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| Sponsor anlegen | – | – | – | ✓ | ✓ | ✓ |
| Sponsor bearbeiten (eigener) | – | – | – | ✓ | ✓ | ✓ |
| Sponsor bearbeiten (fremder) | – | – | – | – | – | ✓ |
| Mitglieder verwalten | – | – | – | – | ✓ | ✓ |
| Org verifizieren | – | – | – | – | – | ✓ |

Vollständige Matrix in `Sponsoring Plattform/05_Rollenkonzept.md`.

## Endpoint-Schutz: Welcher Endpunkt ruft welche AccessControl-Methode?

Alle mutierenden Endpunkte rufen am Anfang der Controller-Methode `accessControl.kannOrgEditierenNachSlug(slug, auth)` bzw. `kannOrgVerwaltenNachSlug(slug, auth)` auf und werfen `AccessDeniedException` bei `false`. Der `GlobalExceptionHandler` mappt diese auf 403.

> Hinweis: Wir haben bewusst auf `@PreAuthorize` mit SpEL-Bean-Referenz verzichtet. Die SpEL-Auswertung von `@accessControl.method(#slug, authentication)` zeigte unter Spring Boot 3.4.1 + Java 25 + Mockito-Subclass-Mocks eine nicht reproduzierbare „Failed to evaluate expression"-Symptomatik. Programmatische Checks sind genauso testbar (AccessControl ist als `@MockBean` verfügbar) und vermeiden Compile-Flag-Abhängigkeiten (`-parameters`).

| HTTP | Pfad | Schutz | Begründung |
|---|---|---|---|
| GET | `/organisationen` | public | Liste lesbar |
| GET | `/organisationen/{slug}` | public | Profil lesbar |
| GET | `/organisationen/neu` | `isAuthenticated()` | nur eingeloggt anlegen |
| POST | `/organisationen` (Create) | `isAuthenticated()` | Anlegender wird automatisch ORG_OWNER |
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
| GET | `/marktplatz/**` | public | Public-Marktplatz |
| GET, POST | `/login`, `/registrieren`, `/verifizieren` | public | Auth-Flows |

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

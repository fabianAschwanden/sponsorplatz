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

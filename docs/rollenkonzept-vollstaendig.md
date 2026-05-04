# Rollen- & Berechtigungskonzept

**Version:** 1.0
**Bezug:** `00_Konzept_v3_Kollaborative-Plattform.md`
**Modell:** Kollaborative Plattform (geteilte Datenbasis, Edit-Rechte über Mitgliedschaft)

---

## Inhaltsverzeichnis

1. [Akteure](#1-akteure)
2. [Rollen-Übersicht](#2-rollen-übersicht)
3. [Globale Plattform-Rollen](#3-globale-plattform-rollen)
4. [Organisations-Rollen (Verein / Firma / Stiftung)](#4-organisations-rollen-verein--firma--stiftung)
5. [Permission-Matrix: Verein-Daten](#5-permission-matrix-verein-daten)
6. [Permission-Matrix: Sponsor-/Firmen-Daten](#6-permission-matrix-sponsor--firmen-daten)
7. [Permission-Matrix: Plattform-weite Aktionen](#7-permission-matrix-plattform-weite-aktionen)
8. [Konkrete Workflows](#8-konkrete-workflows)
9. [Sicherheits-Implementierung (Spring)](#9-sicherheits-implementierung-spring)
10. [DSG, Audit & Datenexport](#10-dsg-audit--datenexport)
11. [Edge-Cases & Konflikte](#11-edge-cases--konflikte)

---

## 1. Akteure

Sechs Hauptakteure interagieren mit der Plattform:

| # | Akteur | Beschreibung | Beispiel |
|---|---|---|---|
| 1 | **Plattform-Betreiber** | Eigentümer & Admin der Plattform | Fabian |
| 2 | **Vereins-Mitglied** | Person mit Mitgliedschaft in Verein-Org | Lea, Vorstand SCA |
| 3 | **Firmen-Mitarbeiter** | Person mit Mitgliedschaft in Sponsor-Org | Jan, Marketing-Manager Bäckerei AG |
| 4 | **Stiftungs-Vertreter** | wie Firmen-Mitarbeiter, aber Org-Typ `STIFTUNG` | Sofia, Stiftungsrat XYZ-Stiftung |
| 5 | **Eingeloggter Benutzer** | Authentifiziert, aber (noch) kein Org-Mitglied | Frischer Self-Reg-Account |
| 6 | **Anonymer Besucher** | Nicht eingeloggt | Sponsor browsed Marktplatz |

> **Hinweis zum Modell:** Im kollaborativen Plattform-Modell (v3) sehen *alle eingeloggten User* alle Daten. Rollen unterscheiden sich daher fast ausschließlich in **Edit-** und **Verwaltungs-Rechten**, nicht in Lese-Rechten.

---

## 2. Rollen-Übersicht

```
┌──────────────────────────────────────────────────────────────────┐
│                      GLOBALE ROLLEN (User-Ebene)                 │
│                                                                  │
│  PLATFORM_ADMIN   →  alles, plattform-weit                       │
│  PLATFORM_MODERATOR → Inhalt moderieren, keine User-Verwaltung   │
│  PLATFORM_SUPPORT →  Read-only zur Hilfe                         │
│                                                                  │
│  (kein Plattform-Recht = normaler Benutzer)                      │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│              ORGANISATIONS-ROLLEN (Mitgliedschaft-Ebene)         │
│                                                                  │
│  ORG_OWNER     →  vollständige Verwaltung der Org                │
│  ORG_EDITOR    →  Inhalte CRUD                                   │
│  ORG_VIEWER    →  nur lesen + Org-Zugehörigkeit anzeigen         │
│                                                                  │
│  (keine Mitgliedschaft = darf nichts in dieser Org editieren)    │
└──────────────────────────────────────────────────────────────────┘
```

**Wichtig:** Ein User kann mehrere Mitgliedschaften haben (z.B. `ORG_OWNER` bei „SCA", `SPONSOR_VIEWER` bei „Bäckerei AG"). Plattform-Rollen und Org-Rollen sind orthogonal.

**Vereinheitlichung:** Auf Code-Ebene gibt es nur **drei** Org-Rollen-Werte (`ORG_OWNER`, `ORG_EDITOR`, `ORG_VIEWER`) — unabhängig vom Org-Typ. Die UI kann sie semantisch übersetzen ("Vorstand", "Sponsoring-Verantwortlicher", "Mitglied" usw.). Das hält das Berechtigungssystem einheitlich und testbar.

---

## 3. Globale Plattform-Rollen

Werden über **OIDC-Gruppen** im Identity Provider (OCI IAM Domains) gepflegt — analog zur heutigen `ROLE_ADMIN/EDITOR/VIEWER`-Logik. Mapping in `SecurityConfig`.

### 3.1 `PLATFORM_ADMIN`

- **Zweck:** Plattform-Betrieb, technische Verwaltung
- **Rechte:**
  - Alles, was eine `ORG_OWNER`-Mitgliedschaft in jeder Org gibt — *implizit für alle Orgs*
  - Vereine verifizieren / suspendieren
  - Plattform-weite Kategorien und Tags pflegen
  - Audit-Trail einsehen
  - Konten löschen, Daten exportieren (DSG)
  - Globale System-Einstellungen
- **Anzahl typisch:** 1–3 Personen
- **Auth-Mechanismus:** OIDC-Gruppe `platform-admin`

### 3.2 `PLATFORM_MODERATOR`

- **Zweck:** Inhalts-Moderation
- **Rechte:**
  - Gemeldete Inhalte einsehen und entfernen
  - Projekte auf `ARCHIVIERT` setzen bei AGB-Verstoß
  - Org-Status auf `SUSPENDED` setzen (nicht löschen)
- **Kann nicht:** Konten löschen, Plattform-Einstellungen ändern, Verifizierung durchführen
- **Auth-Mechanismus:** OIDC-Gruppe `platform-moderator`

### 3.3 `PLATFORM_SUPPORT`

- **Zweck:** Anwender-Support
- **Rechte:**
  - Read-Only auf alle Daten (sieht alles, was eingeloggte User sehen + Audit-Logs)
  - Kann Aktionen *im Namen eines Users* nicht durchführen (kein Impersonation)
- **Auth-Mechanismus:** OIDC-Gruppe `platform-support`

---

## 4. Organisations-Rollen (Verein / Firma / Stiftung)

Werden in der Tabelle `mitgliedschaft` gepflegt. Ein User kann pro Org **eine** Rolle haben (höchste gewinnt bei mehreren).

### 4.1 `ORG_OWNER` (Inhaber)

- **Typische Personen:** Vereinspräsident, Vorstand, Geschäftsführer, Sponsoring-Leiter
- **Rechte (in dieser Org):**
  - Alle CRUD-Operationen auf Sponsoren, Projekte, Pakete, Saisons, Anfragen, Medien, E-Mail-Vorlagen
  - Mitglieder einladen / entfernen / Rolle ändern
  - Org-Profil bearbeiten (Logo, Beschreibung, Website, Branche)
  - Bankverbindung & Rechnungs-Adresse pflegen (Phase 5)
  - Org auflösen / archivieren (Selbst-Lösch-Antrag → Plattform-Admin)
- **Anzahl typisch pro Org:** 1–3

### 4.2 `ORG_EDITOR` (Bearbeiter)

- **Typische Personen:** Eventmanager, Kommunikations-Verantwortliche, Sponsoring-Mitarbeiter
- **Rechte (in dieser Org):**
  - Sponsoren CRUD (sofern angelegt von dieser Org)
  - Projekte CRUD
  - Pakete CRUD
  - Anfragen bearbeiten (annehmen / ablehnen / kommentieren)
  - Medien hochladen
  - E-Mail-Vorlagen verwenden, Serien-E-Mails versenden
  - **Kann nicht:** Mitglieder verwalten, Org-Profil ändern

### 4.3 `ORG_VIEWER` (Beobachter)

- **Typische Personen:** Vereinsmitglied ohne Funktion, Praktikant, Revisionsstelle
- **Rechte (in dieser Org):**
  - Lesen aller Org-Daten (was eingeloggte User ohnehin können)
  - **Anzeige der Org-Zugehörigkeit** im UI ("Lea — Mitglied SCA")
  - Eigene Daten in Org-Kontext sehen (z.B. „meine Aufgaben")
- **Kann nicht:** editieren

> **Anmerkung im offenen Modell:** `ORG_VIEWER` bringt funktional kaum mehr Rechte als „nicht-Mitglied + eingeloggt". Die Rolle ist primär ein **Sichtbarkeits-Marker** im UI — wer ist offiziell Teil des Vereins?

### 4.4 Org-spezifische Bezeichnungen (UI-Layer)

Auf Code-Ebene bleiben die drei Werte. In der UI werden sie je nach Org-Typ benannt:

| Code | Verein-UI | Firma-UI | Stiftung-UI |
|---|---|---|---|
| `ORG_OWNER` | Vorstand | Sponsoring-Leitung | Stiftungsrat |
| `ORG_EDITOR` | Mitarbeiter | Sponsoring-Manager | Sachbearbeiter |
| `ORG_VIEWER` | Mitglied | Mitarbeiter | Beobachter |

Konfiguriert in `messages_de_CH.properties`.

---

## 5. Permission-Matrix: Verein-Daten

Daten, die eine Verein-Organisation besitzt: Projekte, eigene Sponsoring-Pakete, eigene CRM-Sponsoren (von diesem Verein angelegt), Beteiligungen, Saisons, E-Mail-Vorlagen, Medien.

Legende: ✅ darf · ❌ nicht erlaubt · 👁️ nur lesen · ⚙️ wenn Plattform-Admin

| Aktion | Anonym | Eingeloggt (kein Mitglied) | ORG_VIEWER | ORG_EDITOR | ORG_OWNER | PLATFORM_ADMIN |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Profil & Mitglieder** | | | | | | |
| Org-Profil ansehen | 👁️ public | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Org-Profil editieren | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| Mitglieder ansehen | ❌ | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Mitglieder einladen | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| Mitglied entfernen | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| Rolle eines Mitglieds ändern | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| **Projekte** | | | | | | |
| Public-Projekt ansehen | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Draft-Projekt ansehen | ❌ | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Projekt anlegen | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Projekt bearbeiten | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Projekt veröffentlichen | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Projekt archivieren / löschen | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| **Sponsoring-Pakete** | | | | | | |
| Pakete ansehen | 👁️ public | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Paket erstellen / bearbeiten | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Paket löschen | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| **CRM (Sponsoren-Stammdaten)** | | | | | | |
| Sponsor ansehen (geteilter Pool) | ❌ | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Sponsor anlegen | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Sponsor bearbeiten (eigener) | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Sponsor bearbeiten (fremder) | ❌ | ❌ | ❌ | ❌ | ❌ | ⚙️ |
| „Update vorschlagen" zu fremdem Sponsor | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Sponsor löschen | ❌ | ❌ | ❌ | ❌ | ✅ (eigener) | ⚙️ |
| **Beteiligungen** | | | | | | |
| Beteiligung ansehen | ❌ | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Beteiligung anlegen | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Beteiligung bearbeiten | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Beteiligung löschen | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| **Anfragen (eingehend)** | | | | | | |
| Anfrage in Inbox ansehen | ❌ | ❌ | 👁️ | 👁️ | 👁️ | 👁️ |
| Anfrage annehmen / ablehnen | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Nachricht an Sponsor schreiben | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| **E-Mail-Versand** | | | | | | |
| E-Mail-Vorlage anlegen / ändern | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| Serien-E-Mail versenden | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Kommunikations-Historie ansehen | ❌ | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| **Excel & Word** | | | | | | |
| Excel exportieren | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Excel importieren | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Word-Serienbrief generieren | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| **Datenbereinigung** | | | | | | |
| Zefix-/Nominatim-Abgleich starten | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| Findings-Excel herunterladen | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| **Medien** | | | | | | |
| Medien ansehen (in public Projekten) | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Medien hochladen | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Medien löschen | ❌ | ❌ | ❌ | ✅ (eigene) | ✅ | ⚙️ |

---

## 6. Permission-Matrix: Sponsor-/Firmen-Daten

Daten, die eine Sponsor-Organisation (Typ `UNTERNEHMEN` oder `STIFTUNG`) besitzt: eigenes Org-Profil, eigene Anfragen, eigene Mitarbeiter.

| Aktion | Anonym | Eingeloggt (Nicht-Mitglied) | SPONSOR_VIEWER | SPONSOR_EDITOR | SPONSOR_OWNER | PLATFORM_ADMIN |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Sponsor-Org-Profil** | | | | | | |
| Profil im Marktplatz ansehen | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| Profil bearbeiten (Logo, Beschreibung, Branche) | ❌ | ❌ | ❌ | ✅ | ✅ | ⚙️ |
| Mitarbeiter einladen | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| **Marktplatz-Browsing** | | | | | | |
| Projekte browsen + filtern | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Pitch-Decks herunterladen | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Anfragen (ausgehend)** | | | | | | |
| Anfrage zu einem Paket erstellen (Entwurf) | ❌ | ✅* | ❌ | ✅ | ✅ | — |
| Anfrage einreichen | ❌ | ✅* | ❌ | ✅ | ✅ | — |
| Eigene Anfragen ansehen | ❌ | 👁️ eigene | 👁️ alle Org | 👁️ alle Org | 👁️ alle Org | 👁️ |
| Eigene Anfrage zurückziehen | ❌ | ✅ eigene | ❌ | ✅ eigene | ✅ alle Org | ⚙️ |
| Auf Verein-Nachricht antworten | ❌ | ✅ eigene | ❌ | ✅ | ✅ | — |
| **Sponsoring-Historie** | | | | | | |
| Eigene angenommene Sponsorings sehen | ❌ | 👁️ eigene | 👁️ alle Org | 👁️ alle Org | 👁️ alle Org | 👁️ |
| Vertrag herunterladen (Phase 5) | ❌ | ✅ eigene | ✅ alle Org | ✅ alle Org | ✅ alle Org | ⚙️ |
| Rechnung markieren als bezahlt (Phase 5) | ❌ | ❌ | ❌ | ❌ | ✅ | ⚙️ |
| **Watchlist (Phase 2+)** | | | | | | |
| Verein folgen / Watchlist | ❌ | ✅ persönlich | ✅ persönlich | ✅ persönlich | ✅ persönlich | — |

\* Nur eingeschränkt — bevor jemand eine Anfrage einreichen kann, muss er Mitglied einer Sponsor-Org sein. Ein „nackter" eingeloggter User wird beim Anfrage-Klick zum Org-Onboarding geführt.

---

## 7. Permission-Matrix: Plattform-weite Aktionen

| Aktion | Eingeloggt | PLATFORM_SUPPORT | PLATFORM_MODERATOR | PLATFORM_ADMIN |
|---|:---:|:---:|:---:|:---:|
| Self-Reg neue Verein-Org | ✅ | — | — | — |
| Self-Reg neue Sponsor-Org | ✅ | — | — | — |
| Audit-Log lesen | ❌ | 👁️ | 👁️ | 👁️ |
| Verein verifizieren (Status `VERIFIED`) | ❌ | ❌ | ❌ | ✅ |
| Org suspendieren (`SUSPENDED`) | ❌ | ❌ | ✅ | ✅ |
| Org dauerhaft löschen | ❌ | ❌ | ❌ | ✅ |
| Inhalt entfernen / `ARCHIVIERT` setzen | ❌ | ❌ | ✅ | ✅ |
| User-Account dauerhaft löschen (DSG-Antrag) | ❌ | ❌ | ❌ | ✅ |
| Datenexport für User (DSG-Antrag) | ❌ | ❌ | ❌ | ✅ |
| Globale Tags / Kategorien pflegen | ❌ | ❌ | ❌ | ✅ |
| Plattform-Einstellungen | ❌ | ❌ | ❌ | ✅ |
| Datenbereinigung initiieren (alle Orgs) | ❌ | ❌ | ❌ | ✅ |

---

## 8. Konkrete Workflows

### 8.1 Verein registriert sich

```
1. Anonymer User klickt „Verein registrieren"
2. Self-Reg-Form: Name, E-Mail, Passwort, Vereins-Daten
3. System legt an:
   - app_user (Local-Identity oder OIDC)
   - organisation (typ=VEREIN, status=PENDING)
   - mitgliedschaft (rolle=ORG_OWNER)
4. E-Mail-Verifizierung
5. (Auto-Versuch) Zefix-Lookup: bei UID-Match → status=VERIFIED
6. Sonst: PLATFORM_ADMIN-Queue, manuelle Verifizierung
7. Verein kann sofort Daten anlegen — wird aber im Marktplatz erst angezeigt, wenn status=VERIFIED
```

### 8.2 Verein lädt zweites Mitglied ein

```
1. ORG_OWNER Lea: „Mitglied einladen" → E-Mail + Rolle wählen
2. System sendet E-Mail mit Einladungs-Token (24 h gültig)
3. Empfänger klickt Link
   3a. Hat Account → Mitgliedschaft wird angelegt, Rolle gesetzt, fertig
   3b. Hat keinen Account → Self-Reg-Light-Flow, danach 3a
4. Audit-Event: MITGLIEDSCHAFT_ANGELEGT
```

### 8.3 Sponsor sucht Projekt und stellt Anfrage

```
1. Anonymer User browst Marktplatz, filtert nach Region/Branche
2. Klickt auf Projekt → Detail mit Paketen
3. Klickt „Anfrage stellen" auf Gold-Paket
   3a. Nicht eingeloggt → Login/Self-Reg-Sponsor-Flow
   3b. Eingeloggt aber kein SPONSOR_EDITOR/OWNER → „Welche Sponsor-Org?" oder
       „Neue Sponsor-Org anlegen"
   3c. Mitglied einer Sponsor-Org mit ausreichender Rolle → direkt zum Formular
4. Anfrage-Formular: Anschreiben, ggf. abweichender Betrag
5. Status ENTWURF → Klick „Einreichen" → Status EINGEREICHT
6. E-Mail an alle ORG_OWNER + ORG_EDITOR der Verein-Org
7. Konversation läuft via threaded Nachrichten
8. Verein klickt „Annehmen" → Status ANGENOMMEN
   - System legt automatisch SponsorBeteiligung an (mit Sponsor-Org-Daten)
   - quantity_taken += 1
   - Audit-Event ANFRAGE_ANGENOMMEN
9. Bei „Ablehnen" → Status ABGELEHNT, Begründung gespeichert
```

### 8.4 Plattform-Admin verifiziert Verein manuell

```
1. PLATFORM_ADMIN öffnet /admin/verifizierung
2. Liste aller status=PENDING Vereine
3. Klick auf Verein → Detail
   - Org-Profil
   - Eingegebene UID, Adresse
   - Zefix-Auto-Findings (falls vorhanden)
   - Statuten-Upload (Phase 2)
4. Aktion: Verifizieren / Ablehnen / Mehr Info anfordern
5. Bei Verifizieren: status=VERIFIED, verifiziert_am=now()
6. E-Mail an alle ORG_OWNER der Org
7. Audit-Event ORG_VERIFIZIERT
```

### 8.5 User wechselt Org-Rolle

```
1. ORG_OWNER Lea entfernt sich selbst (will nur noch ORG_VIEWER sein)
2. System prüft: gibt es noch mindestens einen ORG_OWNER?
   - Ja → Demotion erlaubt
   - Nein → Fehler „Mindestens 1 Owner erforderlich"
3. Audit-Event MITGLIEDSCHAFT_GEAENDERT
```

---

## 9. Sicherheits-Implementierung (Spring)

### 9.1 Rollen-Konstanten

```java
public enum Rolle {
    ORG_OWNER,
    ORG_EDITOR,
    ORG_VIEWER
}

public final class PlattformRolle {
    public static final String PLATFORM_ADMIN     = "PLATFORM_ADMIN";
    public static final String PLATFORM_MODERATOR = "PLATFORM_MODERATOR";
    public static final String PLATFORM_SUPPORT   = "PLATFORM_SUPPORT";
    private PlattformRolle() {}
}
```

### 9.2 `AccessControl`-Bean (zentrale Berechtigungslogik)

```java
@Component("accessControl")
@RequiredArgsConstructor
public class AccessControl {

    private final MitgliedschaftRepository mitgliedschaftRepo;

    public boolean kannOrgEditieren(UUID organisationId, Authentication auth) {
        if (!authenticated(auth)) return false;
        if (istPlattformAdmin(auth)) return true;
        return hatRolleInOrg(auth, organisationId,
            Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR));
    }

    public boolean kannOrgVerwalten(UUID organisationId, Authentication auth) {
        if (!authenticated(auth)) return false;
        if (istPlattformAdmin(auth)) return true;
        return hatRolleInOrg(auth, organisationId, Set.of(Rolle.ORG_OWNER));
    }

    public boolean kannSponsorBearbeiten(UUID besitzerOrgId, Authentication auth) {
        return kannOrgEditieren(besitzerOrgId, auth);
    }

    public boolean kannProjektVeroeffentlichen(UUID organisationId, Authentication auth) {
        return kannOrgEditieren(organisationId, auth);
    }

    public boolean istPlattformAdmin(Authentication auth) {
        return hatGlobaleRolle(auth, PlattformRolle.PLATFORM_ADMIN);
    }

    public boolean istPlattformModerator(Authentication auth) {
        return hatGlobaleRolle(auth, PlattformRolle.PLATFORM_ADMIN,
                                     PlattformRolle.PLATFORM_MODERATOR);
    }

    private boolean hatRolleInOrg(Authentication auth, UUID orgId, Set<Rolle> rollen) {
        return mitgliedschaftRepo.existsByUserSubjectAndOrganisationIdAndRolleIn(
            auth.getName(), orgId, rollen);
    }

    // … hatGlobaleRolle, authenticated() Helpers
}
```

### 9.3 Verwendung in Controllern

```java
@PostMapping("/projekte/{id}/veroeffentlichen")
@PreAuthorize("@accessControl.kannProjektVeroeffentlichen(#orgId, authentication)")
public String veroeffentliche(@PathVariable UUID id,
                              @ModelAttribute("orgId") UUID orgId) {
    projektService.veroeffentliche(id);
    return "redirect:/projekte/" + id;
}
```

Oder method-level auf Service:

```java
@Transactional
@PreAuthorize("@accessControl.kannSponsorBearbeiten(#sponsor.besitzerOrganisationId, authentication)")
public Sponsor speichere(Sponsor sponsor) { ... }
```

### 9.4 Read-Endpoints

Lese-Endpoints brauchen **keinen** spezifischen Filter — alle authentifizierten User sehen alles. Für Public-Endpoints (Marktplatz):

```java
@GetMapping("/marktplatz/projekte")
public String listePublic(Model model) {
    model.addAttribute("projekte",
        projektRepo.findBySichtbarkeitOrderByDatumAsc(Sichtbarkeit.OEFFENTLICH));
    return "marktplatz/liste";
}
```

### 9.5 SecurityConfig-Skizze

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .authorizeHttpRequests(auth -> auth
            // Public Marktplatz
            .requestMatchers("/", "/marktplatz/**", "/sitemap.xml", "/oeffentlich/**",
                             "/css/**", "/js/**", "/images/**").permitAll()
            // Self-Registration
            .requestMatchers("/registrieren/**", "/login", "/passwort-vergessen").permitAll()
            // Plattform-Admin-Routen
            .requestMatchers("/admin/**").hasAuthority(PLATFORM_ADMIN)
            // Plattform-Moderation
            .requestMatchers("/moderation/**").hasAnyAuthority(PLATFORM_ADMIN, PLATFORM_MODERATOR)
            // Alles andere: eingeloggt
            .anyRequest().authenticated()
          )
          .oauth2Login(Customizer.withDefaults())  // OIDC für SCA-Mitarbeiter
          .formLogin(Customizer.withDefaults())    // Local-Login für Self-Reg
          .csrf(...);
        return http.build();
    }
}
```

### 9.6 Empfohlene Test-Klassen (TDD)

| Klasse | Was wird getestet |
|---|---|
| `AccessControlTest` | Unit-Tests aller `kann…`-Methoden mit Mock-Repository |
| `RollenIntegrationTest` | `@SpringBootTest` mit `@WithMockUser` + Mitgliedschafts-Setup, alle Permission-Matrix-Zeilen |
| `PublicEndpointsTest` | Anonymer Zugriff auf Marktplatz funktioniert, Edit-Endpoints geben 401/403 |
| `OwnerCountInvariantTest` | Org-Owner kann sich nicht selbst entfernen, wenn er der einzige ist |
| `MitgliedschaftServiceTest` | Einladungs-Workflow, Token-Verfall, Rollen-Promotion |

---

## 10. DSG, Audit & Datenexport

### 10.1 Audit-Trail

Jede sicherheitsrelevante Aktion erzeugt einen Eintrag in `audit_event`:

| `action` | Auslöser |
|---|---|
| `MITGLIEDSCHAFT_ANGELEGT` | Einladung angenommen |
| `MITGLIEDSCHAFT_GEAENDERT` | Rollen-Promotion / Demotion |
| `MITGLIEDSCHAFT_ENTFERNT` | Mitglied entfernt |
| `ORG_VERIFIZIERT` | Plattform-Admin verifiziert |
| `ORG_SUSPENDIERT` | Suspendierung |
| `PROJEKT_VEROEFFENTLICHT` | Sichtbarkeit geändert auf OEFFENTLICH |
| `ANFRAGE_EINGEREICHT` / `_ANGENOMMEN` / `_ABGELEHNT` | Anfrage-Workflow |
| `USER_GELOESCHT` | DSG-Lösch-Antrag durchgeführt |
| `DATENEXPORT_ERSTELLT` | DSG-Export-Antrag |

Felder pro Event: `actor_user_subject`, `action`, `entity_type`, `entity_id`, `payload_json` (Vorher/Nachher-Diff bei UPDATE), `created_at`.

Lesbarkeit: PLATFORM_SUPPORT/MODERATOR/ADMIN haben Read-Zugriff. Ein User kann seine *eigenen* Audit-Events einsehen (Profil → „Meine Aktivitäten").

### 10.2 DSG: Datenexport

Jeder eingeloggte User hat das Recht, seine personenbezogenen Daten zu exportieren:

- **Self-Service-Endpoint:** `/profil/datenexport` → ZIP mit JSON-Dateien (eigenes Profil, eigene Mitgliedschaften, eigene Audit-Events, Notiz-Erwähnungen, Anfrage-Historie als Sponsor-Mitarbeiter)
- **Verein-Export:** `ORG_OWNER` kann komplette Verein-Daten exportieren (`/verein/{id}/export`)
- **Auf Antrag:** `PLATFORM_ADMIN` exportiert auf Anfrage erweiterte Datensätze (für Behörden o.ä.)

### 10.3 DSG: Lösch-Recht

| Was wird gelöscht | Wer löst aus |
|---|---|
| Eigenes Konto + persönliche Daten | User selbst (`/profil/loeschen`) |
| Beiträge in Verein (Notizen, Audit-Events) | bleiben mit Pseudonym `[gelöschter User]` (referentielle Integrität) |
| Sponsoren-Stammdaten | bleiben — sind nicht User-personenbezogen, sondern Org-Daten |
| Sponsor-Org auflösen | `ORG_OWNER` → Antrag → `PLATFORM_ADMIN` führt aus |
| Verein-Org auflösen | `ORG_OWNER` → Antrag → `PLATFORM_ADMIN` führt aus, Daten anonymisiert oder gelöscht je nach Wunsch |

### 10.4 Datenklassifizierung

Auch im offenen Modell macht es Sinn, sensible Felder zu kennzeichnen:

| Klasse | Beispiele | Behandlung |
|---|---|---|
| **Public** | Org-Name, Logo, Branche, veröffentliche Projekte | Marktplatz-sichtbar |
| **Authenticated** | Sponsor-Telefon, Beteiligungsbeträge, Notizen | Login erforderlich |
| **Sensitive** | Geburtsdaten, IBAN (Phase 5) | Login + ORG_VIEWER der Eigentümer-Org erforderlich (engerer Filter), Audit beim Lesen |

Die "Sensitive"-Klasse wird in v3 nicht aktiv genutzt, ist aber als Hook für Phase 5 vorgesehen — falls Banking/Vertragsdaten dazukommen, wird man hier doch wieder eine Form von Tenant-Filter brauchen.

---

## 11. Edge-Cases & Konflikte

### 11.1 User in mehreren Orgs

**Frage:** Lea ist `ORG_OWNER` bei SCA und `ORG_EDITOR` bei FC Beispiel. Wenn sie ein Projekt anlegt — welche Org ist Eigentümer?

**Lösung:** Beim Anlegen wird die Eigentümer-Org explizit gewählt. UI-Default: aktive Org-Auswahl im Header (Session-Attribut), aber alle Orgs als Dropdown verfügbar.

### 11.2 Gemeinsamer Sponsor mit unterschiedlichen Daten

**Frage:** Verein A legt „Migros" mit Telefon 058-… an. Verein B will „Migros" mit Telefon 044-… anlegen.

**Lösung:**
- Bei Anlage: Plattform schlägt vor: „Es existiert bereits 'Migros Genossenschaft'. Verwenden? Oder neu anlegen?" (Duplikat-Erkennung über Name + Adresse).
- Wird verwendet → Verein B nutzt denselben Datensatz, kann eigene Beteiligung anlegen, kann *nicht* die Stammdaten editieren.
- Wird neu angelegt → zwei separate Datensätze mit unterschiedlichen `besitzer_organisation_id`.
- „Update vorschlagen": Verein B kann eine Änderungs-Anfrage stellen → geht an `besitzer_organisation_id` = Verein A.

### 11.3 Owner verlässt die Org ohne Nachfolger

**Frage:** SCA hat genau einen `ORG_OWNER` Lea. Lea will gehen.

**Lösung:** UI verhindert Self-Demotion/Removal, wenn nur ein Owner. Lea muss erst einen anderen Owner ernennen oder einen Mitglied promovieren. Falls nicht möglich → Antrag an `PLATFORM_ADMIN`.

### 11.4 Sponsor ohne Org-Zugehörigkeit

**Frage:** Privatperson Hans möchte einen Verein ohne Firma sponsern.

**Lösung Phase 0–4:** Privatpersonen-Sponsoring nicht im Scope. Hans muss eine Sponsor-Org anlegen (typ=ANDERE) — auch wenn nur er selbst Mitglied ist.

**Phase 5:** Möglichkeit für „Privat-Sponsor"-Account (Sonderform, kein Org-Profil-Public).

### 11.5 Verein wird gehackt → fremde Person ändert Daten

**Maßnahmen:**
- Audit-Trail jedes Edit-Events
- E-Mail-Notification an alle `ORG_OWNER` bei Änderung von Schlüssel-Daten (Bankverbindung, Mitglieder-Liste)
- 2FA für `ORG_OWNER` empfohlen (Phase 1)
- Recovery-Workflow durch `PLATFORM_ADMIN`

### 11.6 Plattform-Admin verlässt Plattform

**Maßnahmen:**
- Mindestens 2 `PLATFORM_ADMIN`-Accounts immer aktiv (Vier-Augen-Prinzip)
- Beim Onboarding einer neuen Plattform-Admin-Person: Übergabe-Checklist
- Im Code: keine Hard-coded Admin-IDs

### 11.7 Recht auf Vergessenwerden vs. Audit-Trail

**Konflikt:** DSG verlangt Lösch-Recht; Audit-Trail soll dauerhaft sein.

**Lösung:** Bei User-Löschung wird der User-Eintrag entfernt, die Audit-Events behalten aber pseudonymisierte Referenz `[user_x_geloescht_am_Y]`. So bleibt die Nachvollziehbarkeit der Aktion erhalten ohne personenbezogene Identifikation.

---

## Anhang A: Ableitungs-Cheatsheet

Wenn ein neuer Endpoint hinzukommt, gehe diese Fragen durch:

```
1. Ist es ein Lese- oder Schreib-Endpoint?
   → Lesen: meist nur authenticated() oder permitAll()
   → Schreiben: braucht @PreAuthorize

2. Welche Org „besitzt" die Ressource?
   → Eigentümer-Org-ID muss aus Pfad/Body verfügbar sein
   → @PreAuthorize("@accessControl.kannOrgEditieren(#orgId, authentication)")

3. Ist es eine Verwaltungs-Aktion (Mitglieder, Org-Profil)?
   → @PreAuthorize("@accessControl.kannOrgVerwalten(#orgId, authentication)")

4. Ist es eine Plattform-globale Aktion?
   → hasAuthority("PLATFORM_ADMIN") oder "@accessControl.istPlattformAdmin(authentication)"

5. Gibt es Sonderfälle (eigene Daten, Self-Service)?
   → Custom-Methode in AccessControl ergänzen
```

## Anhang B: Beispiel-User-Konstellationen

| Person | Globale Rolle | Mitgliedschaften |
|---|---|---|
| Fabian | `PLATFORM_ADMIN` | (keine Org nötig) |
| Lea | – | SCA: `ORG_OWNER` |
| Marco | – | SCA: `ORG_EDITOR`, FC Beispiel: `ORG_OWNER` |
| Jan | – | Bäckerei AG: `ORG_OWNER` |
| Anna | – | Bäckerei AG: `ORG_EDITOR`, Stiftung XYZ: `ORG_VIEWER` |
| Tom | `PLATFORM_SUPPORT` | – |

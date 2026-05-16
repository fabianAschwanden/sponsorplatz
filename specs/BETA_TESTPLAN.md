# Beta-Testplan — Sponsorplatz

> **Status:** Aktiv (Pilot-Phase Mai 2026)
> **Zielgruppe:** Beta-Tester:innen aus drei Welten — Verein, Sponsor (CSS Sponsoring-Team / Agentur), Plattform-Admin
> **Format:** Manuelle Akzeptanz-Tests, ergänzend zu den automatisierten ARCH/ORG/MKT/RECH-Tests in [`TESTSTRATEGIE.md`](TESTSTRATEGIE.md)

---

## 1. Ziel

Der Beta-Test sichert ab, dass die automatisiert grünen Tests auch in der
realen Nutzung tragen. Wir testen mit echten Menschen auf echten Geräten in
realen Vereins-Use-Cases, was die Build-Suite nicht abdeckt:

- **End-to-End-Erlebnis** — von der Anmeldung bis zur Rechnung
- **UX-Verständlichkeit** — Begriffe, Reihenfolge, Hinweise
- **Mail-Empfang** — Verifikation, Anfrage-Bestätigung, Mahnung
- **Mehrsprachigkeit + Mobile** — DE/FR/IT, Smartphone vs. Desktop
- **Performance unter realen Bedingungen** — Vereins-WLAN, mobile Daten
- **Edge-Cases der echten Welt** — Sonderzeichen in Vereinsnamen, Schweizer Bankkonten, Umlaute

Out of Scope (separate Testpfade):

- Automatisiert via ArchUnit gesicherte Architektur-Regeln → [`TESTSTRATEGIE.md`](TESTSTRATEGIE.md) §Architektur-Verifikation
- Schweizer-QR-Bill-Compliance gegen Six-Group-Validator → manueller Compliance-Test mit Banking-App, siehe BETA-V-09
- Lastperformance > 50 Concurrent-Users → Phase-10-Performance-Tests, nicht Beta

## 2. Beta-Phasen

| Phase | Wer | Dauer | Ziel |
|---|---|---|---|
| **α — intern** | Fabian solo | 1 Woche | Smoke-Test aller Szenarien gegen Demo-Seed |
| **β-closed** | 5 Pilot-Vereine + 2 CSS-Sponsoring-Team-Mitglieder | 4 Wochen | Echte Nutzung, qualitatives Feedback |
| **β-open** | 20+ Vereine + 5 Marken-Kontakte | 8 Wochen | Skalierung, Edge-Cases |

Dieser Testplan deckt **α und β-closed** ab. β-open verlangt erweiterte
Onboarding-Prozesse, separate Spec.

## 3. Personas

| Persona | Rolle | Beispiel | Wofür testet sie/er |
|---|---|---|---|
| **Lea** | Vereinsvorstand | FC Beispiel Zürich, Sport | Verein-Self-Reg, Org-Pflege, Anfrage-Antwort, Vertrag, Rechnung |
| **Marco** | Verein-Helfer mit ORG_EDITOR | Reha-Verein Bern | Eingeschränkte Rechte, Projekt-CRUD, keine Org-Löschung |
| **Sandra** | CSS Sponsoring-Team-Mitglied | CSS zentral | Anfrage stellen, Vertrag-Sicht, Engagement-Überblick |
| **Thomas** | CSS-Agentur-Mitarbeiter | CSS Agentur Luzern | Regional Sponsoring-Anfragen, Workflow-Synchronisation |
| **Anna** | Plattform-Admin | Sponsorplatz-Operator | Verifizierungs-Queue, Audit, Backups, Suspend |
| **Petra** | CSS-Versicherte | Public-Besucherin | Marktplatz, Vereinsprofile, Marken-Landing (nicht eingeloggt) |

Jede Persona bekommt **einen Test-Account** mit klar dokumentierten
Credentials (siehe Onboarding §4).

## 4. Tester-Onboarding

### 4.1 Voraussetzungen pro Tester:in

- [ ] Funktionierende E-Mail-Adresse (Verifikation, Anfrage-Benachrichtigungen)
- [ ] Smartphone mit aktueller Banking-App *(für QR-Bill-Test, optional)*
- [ ] Modernen Browser (Chrome/Edge/Firefox/Safari letzte 2 Versionen)
- [ ] 30 Minuten Zeit pro Szenario, plus Feedback-Erfassung

### 4.2 Test-Konten

Auf der `demo`-Umgebung werden via `DemoSeedRunner` (Phase 8.1) Vorab-Daten
geseedet. Tester bekommen vor Beta-Start per Mail:

```
URL:        https://demo.sponsorplatz.ch
Konto:      <vorname>@beta.sponsorplatz.ch
Passwort:   <wird per separater Mail zugestellt>
Rolle:      <ORG_OWNER der Demo-Org X | PLATFORM_ADMIN | etc.>
```

Bei Verein-Personas (Lea, Marco): vorab geseedeter Demo-Verein zugewiesen,
inkl. zwei Demo-Projekten und einer Demo-Anfrage in Status NEU.

### 4.3 Sicherheits-Hinweise

- **Keine echten Bankdaten** in der Beta — IBANs aus Demo-Bereich `CH00 0000 …` verwenden
- **Keine echten Sponsoring-Beträge** in der Beta — alle Beträge bleiben fiktiv
- **Demo-Disclaimer** ist via `app.demoModus`-Flag auf allen Seiten sichtbar
- **Daten werden täglich um 03:00 zurückgesetzt** — bei Persistenz-Bedarf bitte vorher Bescheid geben

## 5. Test-Szenarien

Jedes Szenario folgt dem Format:

- **Persona** — wer testet
- **Vorbedingung** — was muss vorher stimmen
- **Schritte** — nummerierte Aktionen
- **Erwartetes Resultat** — was muss passieren
- **Beobachten** — was der Tester notiert (UX, Verständlichkeit, Performance)

### 5.1 Verein-Szenarien (BETA-V)

#### BETA-V01 — Verein-Self-Registrierung

- **Persona:** Lea
- **Vorbedingung:** kein Konto vorhanden

**Schritte:**

1. Öffne <https://demo.sponsorplatz.ch>
2. Klicke „Verein registrieren"
3. Gib E-Mail, Anzeigename und Passwort (≥ 8 Zeichen) ein
4. Submit
5. Prüfe E-Mail-Postfach auf Verifikations-Mail (kann bis 2 Min dauern)
6. Klicke auf den Verifikations-Link in der Mail
7. Melde dich mit den Credentials an

**Erwartet:**
- Nach Submit: Hinweis „Bitte E-Mail bestätigen"
- Mail enthält klickbaren Link
- Verifikations-Seite zeigt „E-Mail bestätigt"
- Login funktioniert, Dashboard erscheint
- Kein Spam-Ordner, kein „Sender unbekannt"-Banner

**Beobachten:**
- Sind Passwort-Anforderungen klar (mind. 8 Zeichen)?
- Mail-Absender, -Betreff und -Inhalt vertrauenswürdig?
- Wie lange dauert die Mail-Zustellung?

#### BETA-V02 — Erste Organisation anlegen

- **Persona:** Lea, frisch verifiziert
- **Vorbedingung:** Login, noch keine Org

**Schritte:**

1. Im Dashboard → „Neue Organisation"
2. Wähle Typ `VEREIN`, gib Vereinsnamen ein
3. Wähle Branche aus der Health-Liste (z. B. `SPORT`)
4. IBAN + Postadresse ausfüllen *(optional, aber für Rechnungen Pflicht)*
5. Speichern
6. Org-Detail prüfen: Status `PENDING`

**Erwartet:**
- Branche-Dropdown zeigt alle 11 Health-Werte mit deutscher Anzeige
- Nicht-Health-Branche ist nicht wählbar
- Slug wird automatisch aus dem Namen generiert
- Status startet als `PENDING` mit Hinweis „Verifizierung durch Admin folgt"

**Beobachten:**
- Branche-Beschreibungs-Tooltips hilfreich?
- IBAN-Validierung greift bei Tippfehlern?

#### BETA-V03 — Org-Verifizierung beobachten

- **Persona:** Lea (passiv)
- **Vorbedingung:** Org als `PENDING` angelegt

**Schritte:**

1. Warte auf Admin-Verifizierung (Anna prüft, siehe BETA-A02)
2. Beobachte das Dashboard
3. Prüfe E-Mail-Postfach auf Verifizierungs-Bestätigungs-Mail

**Erwartet:**
- Org-Status wechselt auf `VERIFIED`
- „Verifiziert am"-Datum sichtbar im Org-Detail
- E-Mail-Bestätigung kommt an

**Beobachten:**
- Dauer der Admin-Verifizierung (Ziel: < 24 h)
- Klarheit der Bestätigungs-Mail

#### BETA-V04 — Mitglied via Einladung hinzufügen

- **Persona:** Lea (Owner), Marco (eingeladenes Mitglied)
- **Vorbedingung:** Lea ist ORG_OWNER einer verifizierten Org

**Schritte:**

1. Lea → Org → Mitglieder → „Einladung verschicken"
2. Lea gibt Marcos E-Mail + Rolle `ORG_EDITOR` ein
3. Marco prüft sein Postfach
4. Marco klickt Einladungs-Link
5. Marco registriert sich (falls kein Konto) oder nimmt direkt an
6. Lea prüft Mitgliederliste

**Erwartet:**
- Einladungs-Mail enthält Org-Namen und Rolle
- Link führt zu Vorschau-Seite (kein Auto-Accept durch Mail-Crawler)
- Nach Bestätigung: Marco ist in Lea's Org als ORG_EDITOR sichtbar
- Marco kann Projekte bearbeiten, aber Org nicht löschen

**Beobachten:**
- Was passiert, wenn Marco bereits Sponsorplatz-Konto hat?
- Was passiert mit der Mail bei abgelaufenem Token (>7 Tage)?

#### BETA-V05 — Projekt erstellen + veröffentlichen

- **Persona:** Lea
- **Vorbedingung:** Org ist VERIFIED

**Schritte:**

1. Org-Detail → „Projekte" → „Neues Projekt"
2. Name, Beschreibung (≥ 50 Zeichen), Kategorie, Ort, Zeitraum eingeben
3. Cover-Bild hochladen (JPEG, < 5 MB)
4. „Speichern" → Status `ENTWURF`
5. „Veröffentlichen" klicken
6. Marktplatz öffnen (separat im Inkognito-Fenster)
7. Eigenes Projekt im Marktplatz sichtbar?

**Erwartet:**
- Bild-Upload zeigt Vorschau, akzeptiert nur JPEG/PNG/WebP
- Veröffentlichen-Aktion verlangt Bestätigung
- Projekt erscheint sofort im Marktplatz
- Cover-Bild rendert in der Karten-Ansicht

**Beobachten:**
- Maximale Dateigrösse klar kommuniziert?
- Bild-Verarbeitungs-Dauer akzeptabel?

#### BETA-V06 — Sponsoring-Pakete anlegen

- **Persona:** Lea
- **Vorbedingung:** Veröffentlichtes Projekt vorhanden

**Schritte:**

1. Projekt-Detail → „Pakete" → „Neues Paket"
2. Drei Pakete anlegen: Bronze (CHF 500), Silber (CHF 1'500), Gold (CHF 5'000)
3. Sortierung prüfen (Bronze oben, Gold unten oder umgekehrt — was ist Default?)
4. Marktplatz-Detail des Projekts öffnen
5. Pakete sichtbar mit Preisen?

**Erwartet:**
- Preise in CHF mit Tausender-Trennzeichen
- Sortierung ist deterministisch und sinnvoll
- Inaktiv-Markieren entfernt das Paket aus der Public-Sicht, behält die Anfrage-Historie

#### BETA-V07 — Anfrage beantworten

- **Persona:** Lea (passiv-aktiv), Sandra (aktiv im Sponsor-Pfad)
- **Vorbedingung:** Sandra hat in BETA-S03 eine Anfrage gestellt

**Schritte:**

1. Lea sieht im Dashboard „1 neue Anfrage" Glocke
2. Klick auf Glocke → Anfrage öffnen
3. Anfrage lesen: Sponsor-Name, Nachricht, gewähltes Paket, Betrag
4. „Annehmen" → optionale Antwort-Nachricht
5. Submit
6. Sponsor (Sandra) kontaktieren in Inbox-Thread

**Erwartet:**
- Notification erscheint sofort (Polling 30 s)
- Anfrage-Detail zeigt alle Informationen
- Nach Annahme: Status `ANGENOMMEN`, Vertrag-Button erscheint
- Mail an Sandra mit Antwort
- Inbox-Thread öffnet sich (nur für ANGENOMMEN-Anfragen)

**Beobachten:**
- Wie auffällig ist die Notification?
- Sind die Aktions-Buttons (Annehmen/Ablehnen) klar getrennt?

#### BETA-V08 — Vertrag erstellen + unterzeichnen (Phase-0-Modell)

- **Persona:** Lea (ORG_OWNER)
- **Vorbedingung:** Anfrage ist `ANGENOMMEN`

**Schritte:**

1. Anfrage-Detail → „Vertrag erstellen"
2. Vorausgefüllte Felder prüfen (Org, Sponsor, Paket, Preis, Zeitraum)
3. Leistung-Verein und Leistung-Sponsor ergänzen
4. „Vertrag speichern" → Status `ENTWURF`
5. Vertrag-PDF herunterladen, ausdrucken
6. Physisch unterzeichnen (Lea + Sandra), Original per Post oder Übergabe
7. Im UI „Als unterzeichnet markieren"

**Erwartet:**
- PDF rendert mit Org-Logo, sauberem Layout
- „Als unterzeichnet markieren" verlangt Bestätigung
- Status wechselt zu `UNTERZEICHNET`, Datum + User werden geloggt
- Rechnung-Button erscheint (siehe BETA-V09)

**Beobachten:**
- PDF-Layout: passt es auf eine A4-Seite?
- Vertrag-Sprache: rechtlich solide?

#### BETA-V09 — Rechnung mit QR-Bill versenden

- **Persona:** Lea (ORG_OWNER)
- **Vorbedingung:** Vertrag ist `UNTERZEICHNET`, Org hat IBAN + Adresse

**Schritte:**

1. Vertrag-Detail → „Rechnung erstellen"
2. Rechnungsnummer wird automatisch generiert (`R-2026-NNNNN`)
3. Fälligkeitsdatum prüfen (Default: +30 Tage)
4. „Rechnung versenden" → Status `OFFEN`
5. Rechnung-PDF herunterladen
6. PDF in Swiss-QR-Bill-Validator testen *(z. B. PostFinance-App-Scan)*
7. Sandra erhält Rechnung per Mail

**Erwartet:**
- PDF enthält Swiss-QR-Code am unteren Rand
- Banking-App scannt erfolgreich, Empfänger-IBAN korrekt
- Rechnungsbetrag erscheint im Banking-Vorschau-Bildschirm
- Sponsor-Mail mit PDF-Anhang
- Rechnung in der Vereins-Rechnungen-Liste sichtbar

**Beobachten:**
- QR-Code in mehreren Banking-Apps testen (UBS, ZKB, Raiffeisen, PostFinance, Neon)
- PDF im Print-Preview prüfen: Perforations-Marker korrekt?

#### BETA-V10 — Rechnung als bezahlt markieren

- **Persona:** Lea (ORG_OWNER), Sandra (Sponsor, extern)
- **Vorbedingung:** Rechnung ist `OFFEN`, Sandra hat überwiesen

**Schritte:**

1. Sandra überweist via Banking-App (Demo: virtuelle Überweisung)
2. Lea sieht Eingang im Bankkonto (real, ausserhalb der Plattform)
3. Lea → Rechnungs-Detail → „Als bezahlt markieren"
4. Bestätigen → Status `BEZAHLT`

**Erwartet:**
- Bezahlt-Marker schreibt User + Zeitstempel
- Audit-Log-Eintrag entstanden (sichtbar in BETA-A04)
- Sandra erhält Quittungs-Mail (optional, Phase 12)

#### BETA-V11 — DSG-Datenexport

- **Persona:** Lea
- **Vorbedingung:** Lea hat Konto + Org-Mitgliedschaft + Aktivitäten

**Schritte:**

1. Einstellungen → „Mein Datenexport"
2. „Export starten" → JSON-Download

**Erwartet:**
- JSON enthält: AppUser-Daten, Mitgliedschaften, eigene Anfragen, eigene Notifications
- Keine Daten anderer User
- Datei ist menschen-lesbar (formatted)

### 5.2 Sponsor-Szenarien (BETA-S)

#### BETA-S01 — Marktplatz öffentlich erkunden (ohne Login)

- **Persona:** Petra
- **Vorbedingung:** keine

**Schritte:**

1. Öffne <https://demo.sponsorplatz.ch>
2. Header: „Marktplatz" klicken
3. Branche-Filter testen — Chips zum Aktivieren/Deaktivieren
4. Volltextsuche „Sommer", „Zürich"
5. Projekt-Karte klicken → Detail-Seite
6. Vereins-Profil über „Veranstalter"-Link öffnen
7. Marken-Landing („Für Marken") öffnen

**Erwartet:**
- Marktplatz lädt < 1 s
- Branche-Filter wirken sofort (URL-Parameter ändern sich)
- Aktive Filter als entfernbare Coral-Chips sichtbar
- Vereinsprofil zeigt Health-Hero-Chip prominent
- Marken-Landing zeigt Live-Statistik (X Vereine, Y Projekte)

#### BETA-S02 — Sponsor-Org-Self-Registrierung

- **Persona:** Sandra (CSS Sponsoring-Team), Thomas (CSS-Agentur)
- **Vorbedingung:** kein Konto

**Schritte:**

1. „Als Sponsor registrieren" klicken
2. Kombinierter Flow: User-Daten + Sponsor-Org-Name
3. Verifikation per Mail
4. Login

**Erwartet:**
- Sponsor-Org wird mit Typ `UNTERNEHMEN` angelegt
- User wird automatisch ORG_OWNER der neuen Sponsor-Org
- Dashboard zeigt „Anfragen stellen"-CTA prominent

#### BETA-S03 — Anfrage stellen

- **Persona:** Sandra
- **Vorbedingung:** Sponsor-Org existiert, eingeloggt

**Schritte:**

1. Marktplatz → Projekt-Detail eines Pilot-Vereins
2. Paket auswählen → „Anfrage stellen"
3. Nachricht-Form (10-2000 Zeichen) füllen
4. Submit

**Erwartet:**
- Validierung wirft bei < 10 Zeichen
- Anfrage erscheint in Sponsor-Dashboard unter „Meine Anfragen"
- Mail an den Verein-Owner geht raus (siehe BETA-V07)

#### BETA-S04 — Watchlist

- **Persona:** Sandra
- **Vorbedingung:** eingeloggt

**Schritte:**

1. Marktplatz → Projekt-Detail
2. „Merken" klicken
3. Watchlist-Seite (Sidebar) öffnen
4. Eintrag entfernen

**Erwartet:**
- Star-Icon wechselt Zustand
- Watchlist zeigt Projekt-Karten chronologisch
- Entfernen ist sofort und ohne Rückfrage

#### BETA-S05 — Antwort auf Anfrage empfangen

- **Persona:** Sandra
- **Vorbedingung:** Lea hat in BETA-V07 die Anfrage angenommen

**Schritte:**

1. Mail-Postfach prüfen
2. Login → Glocke → „1 Antwort"
3. Inbox-Thread öffnen
4. Antwort schreiben

**Erwartet:**
- Mail enthält Verein-Name + Antwort-Auszug
- Inbox-Thread ist chronologisch
- Antwort wird in beide Richtungen sichtbar

### 5.3 Admin-Szenarien (BETA-A)

#### BETA-A01 — Login als PLATFORM_ADMIN

- **Persona:** Anna
- **Vorbedingung:** Admin-Konto vorhanden

**Schritte:**

1. Login mit Admin-Credentials
2. Sidebar zeigt „Admin" → „System" und „Backlog"
3. Klick auf „Admin/System" → Admin-Dashboard

**Erwartet:**
- Admin-Dashboard zeigt Statistiken (Anzahl User, Orgs, Anfragen)
- Non-Admin-User sehen das Menü gar nicht

#### BETA-A02 — Verifizierungs-Queue abarbeiten

- **Persona:** Anna
- **Vorbedingung:** Lea hat in BETA-V02 eine Org als PENDING angelegt

**Schritte:**

1. Admin → Verifizierungen
2. Pending-Org anschauen: Branche, Website, Beschreibung
3. **Health-Fokus-Check** durchführen (siehe Rollenkonzept)
4. Bei Passt: „Verifizieren" → Status `VERIFIED`
5. Bei Nicht-Health-Fokus: „Ablehnen" mit Begründung

**Erwartet:**
- Branche-Chip prominent, Hinweis „Health-Fokus prüfen" sichtbar
- Verifizieren setzt Status + `verifiziert_am`
- Lea (Owner) erhält Mail-Benachrichtigung
- Audit-Log-Eintrag entstanden

#### BETA-A03 — Verein suspendieren

- **Persona:** Anna
- **Vorbedingung:** Verifizierte Org existiert, Anlass für Suspend (z. B. Spam)

**Schritte:**

1. Admin → User/Org-Suche
2. Org öffnen → „Suspendieren" mit Grund
3. Org-Status → `SUSPENDED`
4. Verein versucht Login → Hinweis-Page

**Erwartet:**
- Suspendierter Verein kann nicht mehr in Marktplatz publizieren
- Bestehende Projekte/Rechnungen bleiben sichtbar für Audit
- Audit-Log enthält Grund

#### BETA-A04 — Audit-Log inspizieren

- **Persona:** Anna
- **Vorbedingung:** Diverse Aktionen wurden durchgeführt

**Schritte:**

1. Admin → Audit-Log
2. Letzte 100 Einträge chronologisch durchscrollen
3. Filter nach Aktion (`RECHNUNG_BEZAHLT`, `ORG_VERIFIZIERT`)
4. Detail eines Eintrags öffnen

**Erwartet:**
- Jede Status-Änderung (Verein, Anfrage, Vertrag, Rechnung) erzeugt einen Eintrag
- Eintrag enthält: Zeitpunkt, Aktion, User, betroffene Entity-ID
- Filter wirken sofort

#### BETA-A05 — Backup manuell auslösen

- **Persona:** Anna
- **Vorbedingung:** keine

**Schritte:**

1. Admin → Backups
2. „Backup jetzt erstellen"
3. Liste prüfen — neuer Eintrag mit Zeitstempel + Grösse
4. Download testen

**Erwartet:**
- Backup wird innerhalb 60 s erstellt
- Datei ist non-empty, herunterladbar
- Cleanup-Logik entfernt Backups > 30 Tage automatisch

### 5.4 Public-/Übergreifende Szenarien (BETA-O)

#### BETA-O01 — Mobile-Ansicht Marktplatz

- **Persona:** Petra, auf Smartphone
- **Vorbedingung:** keine

**Schritte:**

1. Marktplatz auf iPhone/Android öffnen
2. Branche-Chips per Touch togglen
3. Projekt-Karte tappen
4. Vereinsprofil-Link folgen

**Erwartet:**
- Layout passt sich an < 400 px Breite an
- Chips sind mit dem Finger zielbar (mind. 44 × 44 px)
- Keine horizontale Scroll-Schleppe

#### BETA-O02 — Mehrsprachigkeit (DE/FR/IT)

- **Persona:** Petra
- **Vorbedingung:** Sprachumschalter im Footer

**Schritte:**

1. Marktplatz öffnen, Sprache wechseln
2. Wichtige Begriffe prüfen: „Sport-Sponsoring", „Anfrage", „Rechnung"
3. Branche-Anzeigenamen in jeder Sprache prüfen

**Erwartet:**
- Sprachumschalter persistiert via Cookie
- Übersetzungen sinnvoll und vollständig (kein Mix mit Deutsch)
- Datums-/CHF-Format passt zur Locale

#### BETA-O03 — Marken-Landing-Page

- **Persona:** Sandra (oder Petra anonym)
- **Vorbedingung:** keine

**Schritte:**

1. Footer-Link „Für Marken" klicken
2. Statistik beobachten (Vereine pro Branche, aktive Projekte)
3. CTA „Sponsor-Konto erstellen" testen

**Erwartet:**
- Statistik ist plausibel und nicht hardcoded
- Health-Branche-Verteilung sichtbar
- CTA führt zu `/sponsor/registrieren`

#### BETA-O04 — Performance-Smoke

- **Persona:** Lea oder Petra
- **Vorbedingung:** keine

**Schritte:**

1. Browser-DevTools Network-Tab öffnen
2. Marktplatz laden
3. „Slow 4G" simulieren, neu laden
4. Cover-Bilder-Lade-Verhalten beobachten

**Erwartet:**
- p95-Ladezeit < 3 s über 4G
- Cover-Bilder lazy-loaded (kein Pre-Fetch aller Karten)
- Keine 500er-Errors

## 6. Browser- und Device-Matrix

Jedes Szenario sollte mindestens auf einer Kombination aus jeder Spalte getestet werden:

| Browser | OS | Device |
|---|---|---|
| Chrome (latest) | macOS / Windows | Desktop |
| Safari (latest) | macOS / iOS | Desktop / iPhone |
| Firefox (latest) | Linux / Windows | Desktop |
| Edge (latest) | Windows | Desktop |
| Chrome Mobile | Android | Smartphone |

Mindestens **drei Kombinationen** pro β-closed-Tester. Pilot-Vereine kriegen
die freie Wahl ihres Geräts, sollen aber zumindest eine Mobile-Sicht testen.

## 7. Feedback-Erfassung

Jeder Test wird pro Tester:in dokumentiert mit:

| Feld | Beispiel |
|---|---|
| Szenario-ID | BETA-V03 |
| Tester:in | Lea M. |
| Datum / Uhrzeit | 2026-05-15 14:32 |
| Browser / Device | Chrome 124 / MacBook |
| Status | ✓ passed / ✗ failed / ◐ blocked |
| Dauer | 4 Minuten |
| Screenshot (falls failed) | Anhang |
| UX-Bemerkung | "Verifikations-Mail kam in 12 s — schnell. Aber Absender unklar." |
| Wunsch / Verbesserung | "Mail-Absender 'noreply@…' irritiert — könnte 'verein@…' sein" |

**Erfassungs-Pfade:**

- **Primär:** GitHub Issue im Repo `sponsorplatz` mit Label `beta-feedback` (Tester bekommen Read-/Issue-Rechte)
- **Sekundär:** Wöchentliches Tester-Call (30 Min), mündliches Feedback wird in GitHub Issues überführt
- **Tertiär:** E-Mail an `beta@sponsorplatz.ch`, wird täglich gesammelt und nachgepflegt

## 8. Bekannte Lücken (nicht testen)

Diese Bereiche sind bewusst noch im Backlog und sollen in der β-closed nicht getestet werden:

- **Online-Zahlung (Datatrans)** — Phase 9.2, nur Stub-Provider aktiv
- **Single Sign-On via Entra ID** — Phase 1.4 Backlog, siehe AUTH_SSO_OIDC.md
- **MwSt-Aufschlüsselung auf Rechnungen** — Phase 12
- **Vertrags-Mahnwesen** — Phase 12, manuelles Inkasso heute
- **Buchhaltungs-Export (CSV/camt.054)** — Phase 12+
- **Digitale Vertrags-Signatur (QES)** — Phase 5.G, derzeit physische Unterzeichnung
- **Echtes Production-DNS sponsorplatz.ch** — derzeit nur demo-Umgebung

Findet ein Tester Probleme in diesen Bereichen, freuen wir uns über Hinweise, werden sie aber priorisiert behandeln.

## 9. Erfolgs-Kriterien für die β-closed

Wir betrachten die β-closed als **erfolgreich**, wenn:

- [ ] **80% der Pflicht-Szenarien** (BETA-V01..V11, BETA-S01..S05) sind über alle Pilot-Vereine `passed`
- [ ] **Keine kritischen Bugs** (Datenverlust, Sicherheits-Leak, Production-Ausfall) entdeckt
- [ ] **5+ Verein-Anfragen** real durchlaufen und mindestens 2 in Status `VERTRAG` oder `BEZAHLT`
- [ ] **mind. 1 Swiss-QR-Bill** in einer echten Banking-App erfolgreich gescannt + überwiesen
- [ ] **Performance** Marktplatz-Liste < 3 s p95 auf mobiler Verbindung
- [ ] **DSG-Datenexport** funktioniert für alle Personas
- [ ] **Tester-NPS** ≥ 7 (Wie wahrscheinlich empfiehlst du Sponsorplatz weiter?)

## 10. Anhang — Test-Template für GitHub Issues

```markdown
**Szenario:** BETA-VXX — <Titel>
**Tester:in:** <Name>
**Browser/Device:** <Chrome 124 / iPhone 15 Pro>
**Status:** ☐ passed ☐ failed ☐ blocked
**Dauer:** <X> Min

## Was lief
<Beschreibung der erfolgreichen Schritte>

## Was nicht lief
<Konkreter Fehler, Reproduktions-Schritte>

## UX-Bemerkung
<Verständlichkeit, Begriffe, Hinweise>

## Screenshot
<Anhang>

## Vorschlag
<Optionale Verbesserungs-Idee>
```

---

**Pflege dieses Dokuments:** Bei jedem neuen Feature, das Beta-relevant ist,
ein neues Szenario ergänzen — analog zur Test-ID-Pflege in `TESTSTRATEGIE.md`.

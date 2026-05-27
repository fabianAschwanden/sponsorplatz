# Beta-Test-Prompts für Claude in Chrome

> **Zweck:** Ausführbare Prompts für das Claude-Chrome-Plugin, mit denen die in
> [`BETA_TESTPLAN.md`](BETA_TESTPLAN.md) definierten Akzeptanz-Szenarien
> halb-automatisiert durchgespielt werden können.
> **Status:** Aktiv ab Mai 2026, parallel zur β-closed-Phase.
> **Ziel-URL:** `https://sponsorplatz.for-better.biz/`

---

## So funktioniert die Sammlung

Jeder Block unten ist ein **eigenständiger Prompt**. Kopiere ihn als Ganzes in
das Claude-Chrome-Plugin und lass das Plugin ihn ausführen. Die Prompts sind so
formuliert, dass das Plugin ohne weiteren Kontext eine klare Mission, konkrete
Test-Daten, Ablauf-Schritte, Erwartungen und ein Reporting-Schema vorfindet.

Pro Prompt liefert das Plugin am Ende ein strukturiertes Resultat — das wird
händisch in ein GitHub Issue mit Label `beta-feedback` überführt
(Vorlage siehe BETA_TESTPLAN.md §10).

### Voraussetzungen vor dem ersten Lauf

- Die Demo-Plattform `https://sponsorplatz.for-better.biz/` ist erreichbar.
- Vor-geseedete Demo-Accounts existieren (siehe `DemoSeedRunner`, Phase 8.1).
- Für Szenarien mit E-Mail-Verifikation: **Der Tester (Fabian) bearbeitet die
  Verifikation manuell.** Der Agent hält an, gibt eine klare Aufforderung im
  Format `[USER-INTERAKTION NÖTIG] …` aus und wartet auf eine Bestätigung
  ("erledigt"), bevor er weiterläuft. Siehe §0.1 Konvention.
- Cookies und LocalStorage zwischen Prompts leeren (`Chrome → DevTools →
  Application → Clear Storage`), damit sich Personas nicht überlagern.

### User-Interaktion bei Mail-Bestätigung

Wann immer ein Schritt eine echte Mail-Aktion verlangt (Verifikations-Link,
Einladungs-Link, Antwort-Mail, Rechnung-Mail), bricht der Agent den Auto-Lauf
**nicht** ab — er pausiert und fordert mich (Fabian) auf, manuell zu handeln:

```
[USER-INTERAKTION NÖTIG]
  Aktion:    Öffne dein Mail-Postfach für <konkrete E-Mail-Adresse>,
             klicke den Verifikations-Link in der Mail "Bitte E-Mail bestätigen".
  Warum:     Der Agent hat keinen Zugriff auf dein Mail-Postfach.
  Erwartet:  Verifikations-Seite zeigt "E-Mail bestätigt".
  Antworte mit "erledigt", sobald die Bestätigung durchgelaufen ist —
  oder mit "blockiert: <Grund>", falls die Mail nicht ankommt.
```

Erst auf "erledigt" setzt der Agent den Lauf fort. Auf "blockiert" beendet er
das Szenario mit Status `◐ blocked` und protokolliert den Grund im Reporting.

Das gleiche Muster gilt für jede andere Aktion, die der Agent nicht selbst
ausführen kann (z. B. Banking-App-QR-Scan in BETA-V09).

### Test-Daten-Konventionen

| Platzhalter | Bedeutung | Beispiel |
|---|---|---|
| `{TS}` | Unix-Timestamp zur Eindeutigkeits-Garantie | `1716370800` |
| `{LAUF}` | Frei wählbarer Lauf-Tag | `mai26-runde2` |
| `Test1234!` | Standard-Passwort für alle neu angelegten Tester | — |
| `@beta.sponsorplatz.ch` | Reservierte Mail-Domain für Beta-Konten | `lea@beta.sponsorplatz.ch` |

Der Agent ersetzt Platzhalter zu Beginn jedes Laufs und protokolliert sie im
Report, damit der Lauf reproduzierbar bleibt.

### Reporting-Schema (jeder Prompt endet damit)

```
ERGEBNIS BETA-XXX
  Status:        ✓ passed | ✗ failed | ◐ blocked
  Dauer:         <Minuten>
  Browser/UA:    <wird vom Agent ermittelt>
  Test-Daten:    <konkrete Werte, die der Lauf generiert hat>
  Verstöße:      <jede nicht erfüllte Erwartung als eigene Zeile>
  UX-Notizen:    <Verständlichkeit, Begriffe, Tempo, Layout>
  Screenshot:    <URL/Pfad zu beigefügtem Screenshot>
  Vorschlag:     <optional, konkrete Verbesserungs-Idee>
```

---

## 0. Bonus-Prompt: Vollständiger E2E-Happy-Path

Dieser Prompt fasst dein Gherkin-Szenario in einen Claude-in-Chrome-Auftrag.
Er ist die Lackmus-Probe für jeden Release: läuft er sauber durch, ist die
Plattform für die nächste Beta-Runde fit.

```
Du bist Beta-Tester für die Sponsoring-Plattform Sponsorplatz.

MISSION
  Spiele den vollständigen Happy-Path durch:
  Verein registriert sich → legt Verein an → stellt Kontakt-Anfrage an CSS
  → CSS nimmt an → Verein erstellt Vertrag aus der Anfrage.

PERSONAS & DATEN
  Verein-Owner:
    Anzeigename:  Max Muster
    E-Mail:       max-muster-{TS}@beta.sponsorplatz.ch
    Passwort:     Test1234!
  Verein:
    Name:         FC E2E {TS}
    Branche:      SPORT
  Sponsor (vorab geseedet):
    E-Mail:       css-sponsoring@beta.sponsorplatz.ch
    Passwort:     Test1234!
    Org:          CSS Versicherung
  Anfrage:
    Betreff:      Sommerfest-Sponsoring 2026

ZIEL-PLATTFORM
  https://sponsorplatz.for-better.biz/

ABLAUF
  1.  Öffne die Startseite und klicke "Verein registrieren".
  2.  Registriere Max Muster mit obigen Daten.
  3.  Pause für User-Interaktion:
      [USER-INTERAKTION NÖTIG]
        Aktion:    Öffne dein Mail-Postfach für
                   max-muster-{TS}@beta.sponsorplatz.ch, klicke den
                   Verifikations-Link in der Bestätigungs-Mail.
        Warum:     Der Agent hat keinen Postfach-Zugriff.
        Erwartet:  Verifikations-Seite zeigt "E-Mail bestätigt".
        Antworte mit "erledigt" oder "blockiert: <Grund>".
  4.  Sobald "erledigt": Logge dich als Max Muster ein.
  5.  Im Onboarding "Verein FC E2E {TS}" mit Branche SPORT anlegen.
  6.  Eine Kontakt-Anfrage an CSS Versicherung mit Betreff
      "Sommerfest-Sponsoring 2026" stellen (Nachricht ≥ 10 Zeichen).
  7.  Logge Max Muster aus.
  8.  Logge dich als css-sponsoring@beta.sponsorplatz.ch ein.
  9.  Öffne die eingegangene Kontakt-Anfrage von "FC E2E {TS}" und klicke
      "Annehmen".
  10. Logge dich wieder als Max Muster ein.
  11. Öffne die angenommene Kontakt-Anfrage und klicke "Vertrag erstellen".
  12. Prüfe, dass die Org- und Betreff-Daten als Snapshot vorausgefüllt sind,
      speichere den Vertrag als Entwurf.

ERWARTUNGEN
  • Bestätigungs-Seite nach manueller Mail-Verifikation zeigt
    "E-Mail bestätigt".
  • Branche-Dropdown enthält die 11 Health-Werte; nur SPORT ist aktiv wählbar.
  • Kontakt-Anfrage erscheint im Sponsor-Dashboard sofort nach Submit.
  • Annahme schaltet den Button "Vertrag erstellen" beim Verein frei.
  • Vertrag-Detail referenziert die Quelle "Kontakt-Anfrage Nr. XXX".
  • Vertrag-Status nach Submit ist ENTWURF.

REPORTING — gib am Ende exakt diese Struktur aus:
  ERGEBNIS E2E-HAPPY-PATH
  Status:        …
  Dauer:        …
  Test-Daten:   {TS}=…, Verein-ID=…, Anfrage-ID=…, Vertrag-ID=…
  Verstöße:     …
  UX-Notizen:   …
  Screenshot:   …
  Vorschlag:    …
```

---

## 1. Verein-Pfad — BETA-V01 bis BETA-V11

### BETA-V01 — Verein-Self-Registrierung

```
Du bist Beta-Tester. Spiele die Persona LEA, frische Vereinsvorständin.

MISSION
  Erstelle ein neues Verein-Owner-Konto und verifiziere die E-Mail.

DATEN
  Anzeigename: Lea Muster {TS}
  E-Mail:      lea-{TS}@beta.sponsorplatz.ch
  Passwort:    Test1234!

VORBEDINGUNG
  Kein Konto vorhanden. Browser-Storage ist leer.

ABLAUF
  1. Öffne https://sponsorplatz.for-better.biz/
  2. Klicke "Verein registrieren".
  3. Trage E-Mail, Anzeigename, Passwort (mind. 8 Zeichen) ein. Submit.
  4. Bestätige die "Bitte E-Mail bestätigen"-Hinweis-Seite.
  5. Pause für User-Interaktion:
     [USER-INTERAKTION NÖTIG]
       Aktion:    Öffne dein Mail-Postfach für
                  lea-{TS}@beta.sponsorplatz.ch und klicke den
                  Verifikations-Link in der Bestätigungs-Mail.
       Warum:     Der Agent hat keinen Postfach-Zugriff.
       Erwartet:  Verifikations-Seite zeigt "E-Mail bestätigt" und
                  Dauer bis Mail-Eingang (in Sekunden) notieren.
       Antworte mit "erledigt: <Dauer in s>" oder "blockiert: <Grund>".
  6. Sobald "erledigt": Logge dich mit den Credentials ein.

ERWARTUNGEN
  • Submit-Erfolgs-Seite trägt sichtbar "Bitte E-Mail bestätigen".
  • Mail kommt innerhalb 2 Minuten an, Absender vertrauenswürdig
    (Bestätigung kommt vom Tester via "erledigt"-Antwort).
  • Verifikations-Seite zeigt "E-Mail bestätigt".
  • Login funktioniert; Dashboard erscheint.
  • Passwort-Regeln sind vor dem Submit erkennbar.

REPORTING
  ERGEBNIS BETA-V01 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-V02 — Erste Organisation anlegen

```
Du bist Lea, frisch verifiziert. Lege deinen Verein als Organisation an.

DATEN
  Verein-Name: FC Beta {TS}
  Typ:         VEREIN
  Branche:     SPORT
  IBAN:        CH00 0000 0000 0000 0000 0  (Demo-IBAN)
  Adresse:     Musterstrasse 1, 8000 Zürich

VORBEDINGUNG
  Login als lea-{TS}@beta.sponsorplatz.ch, noch keine Org vorhanden.

ABLAUF
  1. Im Dashboard "Neue Organisation" klicken.
  2. Typ VEREIN wählen, Namen eintragen.
  3. Branche-Dropdown öffnen, prüfen ob 11 Health-Werte sichtbar sind.
     SPORT wählen.
  4. IBAN + Adresse ausfüllen.
  5. Speichern.
  6. Org-Detail aufrufen, Status PENDING prüfen.

ERWARTUNGEN
  • Slug wird automatisch aus dem Namen abgeleitet.
  • Branche-Dropdown enthält genau diese 11 Werte: SPORT, BEWEGUNG, REHA,
    BEHINDERTENSPORT, SENIORENSPORT, PRAEVENTION, MENTAL_HEALTH, ERNAEHRUNG,
    WELLNESS, SELBSTHILFE, PATIENTENORGANISATION.
  • Branche-Tooltips erklären den Health-Bezug.
  • Nach Speichern: Hinweis "Verifizierung durch Admin folgt".
  • Status startet als PENDING.

REPORTING
  ERGEBNIS BETA-V02 (Status, Dauer, Test-Daten inkl. Org-ID,
  Verstöße, UX-Notizen, Screenshot, Vorschlag).
```

### BETA-V03 — Org-Verifizierung beobachten

```
Du bist Lea und wartest passiv auf die Admin-Verifizierung deines Vereins.

VORBEDINGUNG
  Org "FC Beta {TS}" ist PENDING (BETA-V02 vorher gelaufen).
  Admin hat parallel BETA-A02 ausgeführt oder ein anderer Lauf hat den Verein
  bereits VERIFIED gemacht. Wenn nicht: signalisiere "blocked".

ABLAUF
  1. Login als lea-{TS}@beta.sponsorplatz.ch.
  2. Dashboard prüfen — gibt es eine Verifizierungs-Notification?
  3. Org-Detail öffnen — Status sichtbar?
  4. Pause für User-Interaktion:
     [USER-INTERAKTION NÖTIG]
       Aktion:    Prüfe dein Mail-Postfach für lea-{TS}@beta.sponsorplatz.ch
                  auf eine Mail "Verein verifiziert" (Absender Sponsorplatz).
       Warum:     Der Agent kann das Mail-Postfach nicht einsehen.
       Erwartet:  Mail enthält Org-Name und persönliche Anrede.
       Antworte mit "erledigt: <kurze Beschreibung der Mail>" oder
       "blockiert: keine Mail erhalten".

ERWARTUNGEN
  • Org-Status ist VERIFIED.
  • "Verifiziert am"-Datum im Org-Detail sichtbar.
  • Bestätigungs-Mail laut Tester-Rückmeldung vorhanden, mit Org-Name +
    Mail-Anrede.
  • Wechsel von PENDING → VERIFIED erzeugte einen Audit-Log-Eintrag
    (Bestätigung später durch BETA-A04).

REPORTING
  ERGEBNIS BETA-V03 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag — falls noch PENDING: "blocked" + Hinweis).
```

### BETA-V04 — Mitglied via Einladung hinzufügen

```
Du bist Lea (Owner). Lade Marco als ORG_EDITOR in deinen Verein ein.

DATEN
  Marco-Anzeigename: Marco Helfer {TS}
  Marco-E-Mail:      marco-{TS}@beta.sponsorplatz.ch
  Marco-Passwort:    Test1234!
  Marco-Rolle:       ORG_EDITOR

VORBEDINGUNG
  Lea ist eingeloggt, Org ist VERIFIED.

ABLAUF
  1. Org-Detail → Tab "Mitglieder" → "Einladung verschicken".
  2. E-Mail von Marco + Rolle ORG_EDITOR eingeben, abschicken.
  3. Logge Lea aus.
  4. Pause für User-Interaktion:
     [USER-INTERAKTION NÖTIG]
       Aktion:    Öffne dein Mail-Postfach für
                  marco-{TS}@beta.sponsorplatz.ch und klicke den
                  Einladungs-Link in der Mail "Einladung zu <Org-Name>".
                  Beobachte: enthält die Mail den Org-Namen UND die Rolle?
       Warum:     Der Agent hat keinen Postfach-Zugriff.
       Erwartet:  Vorschau-Seite (kein Auto-Accept).
       Antworte mit "erledigt: <Mail-Beobachtung>" oder
       "blockiert: <Grund>".
  5. Sobald "erledigt": Auf der Vorschau-Seite Konto neu registrieren
     (Anzeigename, Passwort).
  6. Einladung annehmen.
  7. Logge Marco ein, prüfe Sichtbarkeit der Org.
  8. Versuche, die Org zu löschen — muss fehlschlagen.
  9. Logge Lea wieder ein, prüfe Mitgliederliste.

ERWARTUNGEN
  • Einladungs-Mail enthält Org-Name UND Rolle (kein Auto-Accept-Risiko).
  • Vorschau-Seite verlangt aktive Bestätigung.
  • Marco taucht in Lea's Mitgliederliste mit Rolle ORG_EDITOR auf.
  • Marco kann Projekte bearbeiten, aber Org-Löschen ist nicht sichtbar
    oder lehnt mit 403 ab.

REPORTING
  ERGEBNIS BETA-V04 (Status, Dauer, Test-Daten inkl. Einladungs-Token,
  Verstöße, UX-Notizen, Screenshot, Vorschlag).
```

### BETA-V05 — Projekt erstellen + veröffentlichen

```
Du bist Lea. Lege ein veröffentlichungsreifes Projekt an.

DATEN
  Projekt-Name:  Sommerfest {TS}
  Kategorie:     Sport-Event
  Ort:           Zürich
  Zeitraum:      01.07.2026 – 03.07.2026
  Beschreibung:  (mind. 50 Zeichen, generiere thematisch passenden Text)
  Cover-Bild:    Eine beliebige öffentliche Beispiel-JPEG < 5 MB

VORBEDINGUNG
  Lea ist eingeloggt, Org "FC Beta {TS}" ist VERIFIED.

ABLAUF
  1. Org-Detail → Tab "Projekte" → "Neues Projekt".
  2. Felder ausfüllen, Cover-Bild hochladen.
  3. Speichern — Status ENTWURF prüfen.
  4. "Veröffentlichen" klicken, Bestätigungs-Dialog bestätigen.
  5. Öffne in einem neuen Inkognito-Fenster /marktplatz.
  6. Suche nach "Sommerfest {TS}", öffne die Karte.

ERWARTUNGEN
  • Bild-Upload akzeptiert nur JPEG/PNG/WebP, lehnt anderes ab.
  • Vorschau erscheint vor dem Submit.
  • Maximale Dateigrösse ist im UI sichtbar.
  • Projekt-Karte erscheint im Marktplatz ohne erkennbare Verzögerung.
  • Cover-Bild rendert in Karten- und Detail-Ansicht.

REPORTING
  ERGEBNIS BETA-V05 (Status, Dauer, Test-Daten inkl. Projekt-ID,
  Verstöße, UX-Notizen, Screenshot, Vorschlag).
```

### BETA-V06 — Sponsoring-Pakete anlegen

```
Du bist Lea. Lege drei Sponsoring-Pakete für das Projekt aus BETA-V05 an.

DATEN
  Paket 1: Bronze, CHF 500
  Paket 2: Silber, CHF 1500
  Paket 3: Gold,   CHF 5000

VORBEDINGUNG
  Veröffentlichtes Projekt "Sommerfest {TS}" vorhanden.

ABLAUF
  1. Projekt-Detail → Tab "Pakete" → "Neues Paket".
  2. Alle drei Pakete der Reihe nach anlegen.
  3. Sortierung in der Liste protokollieren (Default-Reihenfolge).
  4. Marktplatz-Detail des Projekts in Inkognito öffnen.
  5. Inaktiv-Markieren des Bronze-Pakets testen, dann Marktplatz neu laden.

ERWARTUNGEN
  • Preise erscheinen als "CHF 1'500" mit Tausender-Apostroph.
  • Default-Sortierung ist deterministisch (Bronze→Gold oder umgekehrt).
  • Inaktives Paket verschwindet sofort aus dem Public-Marktplatz, bleibt
    aber in der Admin-Sicht erhalten.

REPORTING
  ERGEBNIS BETA-V06 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-V07 — Anfrage beantworten

```
Du bist Lea. Beantworte eine eingegangene Sponsoring-Anfrage.

VORBEDINGUNG
  Sandra (Sponsor) hat in BETA-S03 eine Anfrage auf das Projekt
  "Sommerfest {TS}" gestellt. Wenn keine offene Anfrage da ist:
  signalisiere "blocked".

ABLAUF
  1. Login als lea-{TS}@beta.sponsorplatz.ch.
  2. Glocken-Icon im Header — Anzahl ungelesener Notifications prüfen.
  3. Auf die Anfrage klicken, Detail aufrufen.
  4. Inhalt vollständig durchgehen: Sponsor-Name, Nachricht, Paket, Betrag.
  5. "Annehmen" klicken, eine optionale Antwort-Nachricht ergänzen.
  6. Submit.
  7. Sponsor-Name in Inbox-Thread öffnen.

ERWARTUNGEN
  • Notification erscheint binnen 30 s nach Anfrage-Eingang.
  • Anfrage-Detail zeigt vollständige Daten.
  • Nach Annahme: Status ANGENOMMEN, "Vertrag erstellen"-Button erscheint.
  • Sponsor erhält Bestätigungs-Mail. Optional via User-Interaktion prüfen:
    [USER-INTERAKTION OPTIONAL]
      Aktion:    Öffne das Postfach des Sponsors (z. B.
                 sandra-{TS}@beta.sponsorplatz.ch) und bestätige, dass die
                 Annahme-Mail mit deiner Antwort-Nachricht eingegangen ist.
      Antworte mit "erledigt: <Mail-Status>" oder "übersprungen".
  • Inbox-Thread öffnet sich nur für ANGENOMMEN-Anfragen.

REPORTING
  ERGEBNIS BETA-V07 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-V08 — Vertrag erstellen + unterzeichnen

```
Du bist Lea. Erzeuge aus der angenommenen Anfrage einen Vertrags-Entwurf
und markiere ihn nach simulierter physischer Unterzeichnung als
UNTERZEICHNET.

VORBEDINGUNG
  Anfrage ist ANGENOMMEN (BETA-V07 gelaufen).

ABLAUF
  1. Anfrage-Detail → "Vertrag erstellen".
  2. Vorausgefüllte Felder protokollieren: Org, Sponsor, Paket, Preis,
     Zeitraum. Quelle muss als Snapshot referenziert sein.
  3. Leistung-Verein und Leistung-Sponsor mit thematisch passenden Texten
     ausfüllen.
  4. "Vertrag speichern" — Status ENTWURF.
  5. PDF herunterladen, kurz inspizieren (Layout, A4-Tauglichkeit).
  6. "Als unterzeichnet markieren" → Bestätigungs-Dialog bestätigen.

ERWARTUNGEN
  • PDF rendert mit Org-Logo und sauberem Layout, eine A4-Seite.
  • "Unterzeichnet"-Aktion verlangt Bestätigung.
  • Status wechselt zu UNTERZEICHNET, Datum + User werden geloggt.
  • "Rechnung erstellen"-Button erscheint.

REPORTING
  ERGEBNIS BETA-V08 (Status, Dauer, Test-Daten inkl. Vertrag-ID,
  Verstöße, UX-Notizen, Screenshot, Vorschlag).
```

### BETA-V09 — Rechnung mit QR-Bill versenden

```
Du bist Lea. Erzeuge die Rechnung zum unterzeichneten Vertrag und
verifiziere den Swiss-QR-Bill.

VORBEDINGUNG
  Vertrag ist UNTERZEICHNET (BETA-V08), Org hat IBAN + Postadresse.

ABLAUF
  1. Vertrag-Detail → "Rechnung erstellen".
  2. Rechnungsnummer prüfen (Format `R-2026-NNNNN`).
  3. Fälligkeitsdatum prüfen (+30 Tage Default).
  4. "Rechnung versenden" → Status OFFEN.
  5. PDF herunterladen.
  6. PDF dem Lauf-Berichter ankündigen — manueller Schritt durch Tester
     mit Banking-App. Der Agent muss diesen Schritt klar als manuell
     kennzeichnen, kann ihn nicht selbst ausführen.

ERWARTUNGEN
  • Rechnung-PDF enthält Swiss-QR-Code am Fuss der ersten Seite.
  • Empfänger-IBAN, Betrag, Verein als Empfänger korrekt im QR-Code.
  • Sponsor-Mail mit PDF-Anhang ist beim Sponsor angekommen. Bestätigung
    via User-Interaktion:
    [USER-INTERAKTION NÖTIG]
      Aktion:    Öffne das Postfach des Sponsors und bestätige, dass die
                 Rechnungs-Mail samt PDF-Anhang eingegangen ist.
      Antworte mit "erledigt: <Anhang vorhanden? ja/nein>" oder
      "blockiert: <Grund>".
  • Rechnung erscheint in der Vereins-Rechnungs-Liste mit Status OFFEN.

REPORTING
  ERGEBNIS BETA-V09 (Status, Dauer, Test-Daten inkl. Rechnungs-Nr,
  Verstöße, UX-Notizen, Screenshot, Vorschlag — beachte: QR-Scan ist
  manuell durch einen menschlichen Tester durchzuführen).
```

### BETA-V10 — Rechnung als bezahlt markieren

```
Du bist Lea. Verbuche den simulierten Zahlungseingang.

VORBEDINGUNG
  Rechnung aus BETA-V09 ist OFFEN.

ABLAUF
  1. Rechnungs-Detail aufrufen.
  2. "Als bezahlt markieren" klicken.
  3. Bezahl-Datum bestätigen, optional Notiz ergänzen.
  4. Submit.

ERWARTUNGEN
  • Status wechselt zu BEZAHLT.
  • User + Zeitstempel werden geloggt.
  • Audit-Log-Eintrag entsteht (Bestätigung in BETA-A04).
  • Optionale Quittungs-Mail bleibt in dieser Phase Stub (Phase 12).

REPORTING
  ERGEBNIS BETA-V10 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-V11 — DSG-Datenexport

```
Du bist Lea. Lade deinen persönlichen Datenexport herunter und verifiziere
den Inhalt.

VORBEDINGUNG
  Lea hat Konto, Org-Mitgliedschaft und mindestens eine Anfrage erlebt
  (Szenarien V01–V10 vorher gelaufen).

ABLAUF
  1. Einstellungen → "Mein Datenexport" öffnen.
  2. "Export starten" klicken.
  3. JSON-Download abwarten und öffnen.
  4. Inhalt inspizieren.

ERWARTUNGEN
  • Export enthält: AppUser-Daten, Mitgliedschaften, eigene Anfragen,
    eigene Notifications.
  • KEINE Daten anderer User (z. B. Marco oder Sandra) sichtbar.
  • Datei ist formatiert, menschen-lesbar.

REPORTING
  ERGEBNIS BETA-V11 (Status, Dauer, Test-Daten, Verstöße inklusive
  fremder Daten-Lecks, UX-Notizen, Screenshot, Vorschlag).
```

---

## 2. Sponsor-Pfad — BETA-S01 bis BETA-S05

### BETA-S01 — Marktplatz öffentlich erkunden

```
Du bist Petra, anonyme Besucherin und CSS-Versicherte.

VORBEDINGUNG
  Nicht eingeloggt, frischer Browser-Storage.

ABLAUF
  1. Öffne https://sponsorplatz.for-better.biz/
  2. Header → "Marktplatz" klicken.
  3. Aktiviere Branche-Filter "SPORT", deaktiviere ihn wieder.
  4. Volltextsuche nach "Sommer" und "Zürich".
  5. Eine Projekt-Karte klicken → Detail-Seite.
  6. Vereinsprofil über "Veranstalter"-Link öffnen.
  7. Footer → "Für Marken" öffnen.

ERWARTUNGEN
  • Marktplatz lädt unter 1 s erstmals.
  • Branche-Filter wirken sofort, URL-Parameter ändern sich.
  • Aktive Filter erscheinen als entfernbare Coral-Chips.
  • Vereinsprofil zeigt Health-Hero-Chip prominent über dem Faltbereich.
  • Marken-Landing zeigt Live-Statistik (X Vereine, Y Projekte, nicht
    hardcoded).

REPORTING
  ERGEBNIS BETA-S01 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-S02 — Sponsor-Org-Self-Registrierung

```
Du bist Sandra, CSS-Sponsoring-Team-Mitglied.

DATEN
  Anzeigename:    Sandra Sponsor {TS}
  E-Mail:         sandra-{TS}@beta.sponsorplatz.ch
  Passwort:       Test1234!
  Sponsor-Org:    CSS Demo {TS}

VORBEDINGUNG
  Kein Konto.

ABLAUF
  1. "Als Sponsor registrieren" klicken.
  2. Kombinierten Flow durchlaufen: User-Daten + Sponsor-Org-Name.
  3. Pause für User-Interaktion:
     [USER-INTERAKTION NÖTIG]
       Aktion:    Öffne dein Mail-Postfach für
                  sandra-{TS}@beta.sponsorplatz.ch und klicke den
                  Verifikations-Link in der Bestätigungs-Mail.
       Warum:     Der Agent hat keinen Postfach-Zugriff.
       Erwartet:  Verifikations-Seite zeigt "E-Mail bestätigt".
       Antworte mit "erledigt" oder "blockiert: <Grund>".
  4. Sobald "erledigt": Login.

ERWARTUNGEN
  • Sponsor-Org wird mit Typ UNTERNEHMEN angelegt.
  • User ist automatisch ORG_OWNER der neuen Sponsor-Org.
  • Dashboard zeigt prominent "Anfragen stellen".

REPORTING
  ERGEBNIS BETA-S02 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-S03 — Anfrage stellen

```
Du bist Sandra. Stelle eine Sponsoring-Anfrage auf das Projekt
"Sommerfest {TS}".

VORBEDINGUNG
  Sponsor-Org angelegt (BETA-S02), eingeloggt.

DATEN
  Ziel-Projekt:    Sommerfest {TS}
  Gewähltes Paket: Silber (CHF 1500)
  Nachricht:       "Wir freuen uns über die Möglichkeit eines lokalen
                    Engagements für Ihren Verein…" (frei formulieren,
                    10–2000 Zeichen)

ABLAUF
  1. Marktplatz → Suche → Projekt-Detail des Vereins.
  2. Paket Silber auswählen, "Anfrage stellen".
  3. Nachricht eingeben, Validierungs-Untergrenze (< 10 Zeichen) einmal
     bewusst auslösen — Fehler erwartet.
  4. Korrigieren, abschicken.
  5. Sponsor-Dashboard → "Meine Anfragen" — Eintrag prüfen.

ERWARTUNGEN
  • Validierung greift bei < 10 Zeichen mit verständlicher Meldung.
  • Anfrage erscheint sofort im Dashboard mit Status NEU.
  • Mail an Vereins-Owner geht raus. Bestätigung via User-Interaktion:
    [USER-INTERAKTION NÖTIG]
      Aktion:    Öffne das Postfach des Vereins-Owners
                 (lea-{TS}@beta.sponsorplatz.ch) und bestätige, dass die
                 Anfrage-Mail mit Sponsor-Name und Paket eingegangen ist.
      Antworte mit "erledigt: <Mail-Auszug>" oder "blockiert".

REPORTING
  ERGEBNIS BETA-S03 (Status, Dauer, Test-Daten inkl. Anfrage-ID,
  Verstöße, UX-Notizen, Screenshot, Vorschlag).
```

### BETA-S04 — Watchlist

```
Du bist Sandra. Setze und entferne eine Watchlist-Markierung.

VORBEDINGUNG
  Eingeloggt als sandra-{TS}@beta.sponsorplatz.ch.

ABLAUF
  1. Marktplatz öffnen, ein beliebiges Projekt-Detail aufrufen.
  2. "Merken"-Stern klicken, Zustand beobachten.
  3. Aus der Sidebar die Watchlist-Seite öffnen.
  4. Eintrag prüfen, dann entfernen.

ERWARTUNGEN
  • Star-Icon wechselt sichtbar zwischen aktiv/inaktiv.
  • Watchlist zeigt Projekt-Karten chronologisch (zuletzt gemerkt oben).
  • Entfernen wirkt sofort, kein Bestätigungs-Dialog nötig.

REPORTING
  ERGEBNIS BETA-S04 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-S05 — Antwort auf Anfrage empfangen

```
Du bist Sandra. Empfange Lea's Antwort und führe einen Inbox-Thread.

VORBEDINGUNG
  Anfrage aus BETA-S03 ist von Lea in BETA-V07 angenommen worden.

ABLAUF
  1. Pause für User-Interaktion:
     [USER-INTERAKTION NÖTIG]
       Aktion:    Öffne dein Mail-Postfach für
                  sandra-{TS}@beta.sponsorplatz.ch und prüfe, ob die
                  Antwort-Mail vom Verein eingegangen ist. Notiere
                  Verein-Name + Antwort-Auszug.
       Warum:     Der Agent hat keinen Postfach-Zugriff.
       Antworte mit "erledigt: <Mail-Auszug>" oder "blockiert".
  2. Login als Sandra.
  3. Glocke → "1 Antwort" — Notification öffnen.
  4. Inbox-Thread öffnen.
  5. Eine Folge-Antwort schreiben, abschicken.

ERWARTUNGEN
  • Mail enthält Verein-Name und Antwort-Auszug.
  • Inbox-Thread ist chronologisch, beide Richtungen sichtbar.
  • Antwort erscheint sofort in beiden Postfächern (Sandra + Lea-Dashboard).

REPORTING
  ERGEBNIS BETA-S05 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

---

## 3. Admin-Pfad — BETA-A01 bis BETA-A05

### BETA-A01 — Login als PLATFORM_ADMIN

```
Du bist Anna, Plattform-Admin.

DATEN
  E-Mail:    anna@beta.sponsorplatz.ch  (vorab geseedet)
  Passwort:  Test1234!

VORBEDINGUNG
  Frischer Browser-Storage.

ABLAUF
  1. Login mit Admin-Credentials.
  2. Sidebar prüfen — gibt es "Admin/System" und "Backlog"?
  3. Admin/System öffnen.

ERWARTUNGEN
  • Sidebar zeigt Admin-Menü nur bei Admin-Konten.
  • Admin-Dashboard zeigt Statistiken: Anzahl User, Orgs, Anfragen.
  • Non-Admin-User (z. B. Lea aus früherem Lauf) sieht das Menü nicht.

REPORTING
  ERGEBNIS BETA-A01 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-A02 — Verifizierungs-Queue abarbeiten

```
Du bist Anna. Verifiziere oder lehne pending Vereine im Health-Fokus ab.

VORBEDINGUNG
  Mindestens eine PENDING-Org existiert (z. B. "FC Beta {TS}" aus BETA-V02).

ABLAUF
  1. Admin → Verifizierungen.
  2. Eine pending Org öffnen.
  3. Branche, Website, Beschreibung prüfen.
  4. Health-Fokus-Check protokollieren.
  5. Bei Passt: "Verifizieren". Bei klar Nicht-Health: "Ablehnen" mit
     Begründung — für den FC Beta erwartet Verifizieren.

ERWARTUNGEN
  • Branche-Chip prominent oberhalb des Faltbereichs.
  • Hinweis "Health-Fokus prüfen" sichtbar.
  • Nach Verifizieren: Status VERIFIED, "verifiziert_am" gesetzt.
  • Owner-Mail-Benachrichtigung kommt an. Bestätigung via User-Interaktion:
    [USER-INTERAKTION OPTIONAL]
      Aktion:    Öffne das Postfach des Owners (lea-{TS}@…) und bestätige,
                 dass die "Verein verifiziert"-Mail eingegangen ist.
      Antworte mit "erledigt" oder "übersprungen".
  • Audit-Log-Eintrag entstanden.

REPORTING
  ERGEBNIS BETA-A02 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-A03 — Verein suspendieren

```
Du bist Anna. Suspendiere eine verifizierte Org auf Verdacht.

DATEN
  Ziel-Org:  Eine verifizierte Demo-Org (NICHT FC Beta {TS} — wähle eine
             andere, sonst brichst du den E2E-Lauf ab).
  Grund:     "Beta-Test: Spam-Verdacht"

VORBEDINGUNG
  Mindestens zwei verifizierte Orgs existieren.

ABLAUF
  1. Admin → User/Org-Suche.
  2. Ziel-Org öffnen, "Suspendieren" mit Grund.
  3. Logge dich danach kurz als Owner dieser Org ein, um die Sperr-Page
     zu prüfen.

ERWARTUNGEN
  • Suspendierter Verein kann nicht mehr publizieren.
  • Bestehende Projekte/Rechnungen bleiben für Audit sichtbar.
  • Audit-Log enthält den Begründungs-Text.
  • Login-Versuch zeigt eine klare Hinweis-Page, keine 500.

REPORTING
  ERGEBNIS BETA-A03 (Status, Dauer, Test-Daten inkl. Org-ID, Verstöße,
  UX-Notizen, Screenshot, Vorschlag).
```

### BETA-A04 — Audit-Log inspizieren

```
Du bist Anna. Inspiziere das Audit-Log nach den vorigen Beta-Läufen.

VORBEDINGUNG
  Mindestens die Szenarien V01–V10 + A02 + A03 sind gelaufen.

ABLAUF
  1. Admin → Audit-Log öffnen.
  2. Letzte 100 Einträge chronologisch durchscrollen.
  3. Filter nach Aktion `RECHNUNG_BEZAHLT` setzen.
  4. Filter nach Aktion `ORG_VERIFIZIERT` setzen.
  5. Detail eines Eintrags öffnen.

ERWARTUNGEN
  • Jede in den V/A-Szenarien getätigte Status-Änderung erzeugt einen Eintrag.
  • Eintrag enthält: Zeitpunkt, Aktion, User, Entity-ID, Umgebungs-Marker.
  • Filter wirken sofort, URL-Parameter ändern sich.

REPORTING
  ERGEBNIS BETA-A04 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-A05 — Backup manuell auslösen

```
Du bist Anna. Erzeuge ein Ad-hoc-Backup und prüfe die Liste.

VORBEDINGUNG
  Eingeloggt als Admin.

ABLAUF
  1. Admin → Backups.
  2. "Backup jetzt erstellen" klicken.
  3. Wartezeit < 60 s einhalten.
  4. Backup-Liste neu laden, neuesten Eintrag suchen.
  5. Download testen (Datei-Größe protokollieren, ohne sie zu öffnen).

ERWARTUNGEN
  • Backup wird in < 60 s erzeugt.
  • Datei ist non-empty, Format z. B. .sql.gz oder .dump.
  • Cleanup-Logik älterer Backups (> 30 Tage) zeigt einen Hinweis in der UI.

REPORTING
  ERGEBNIS BETA-A05 (Status, Dauer, Test-Daten inkl. Backup-Größe,
  Verstöße, UX-Notizen, Screenshot, Vorschlag).
```

---

## 4. Public-/Übergreifende Szenarien — BETA-O01 bis BETA-O04

### BETA-O01 — Mobile-Ansicht Marktplatz

```
Du bist Petra auf einem Smartphone. Wenn der Agent kein echtes Mobile-Gerät
hat, emuliere via Chrome DevTools "iPhone 15 Pro" (Toolbar → Device-Modus).

VORBEDINGUNG
  Nicht eingeloggt. Viewport ist 393 × 852 (iPhone 15 Pro).

ABLAUF
  1. Marktplatz öffnen.
  2. Branche-Chips per Touch togglen.
  3. Projekt-Karte tappen → Detail-Seite.
  4. Veranstalter-Link folgen → Vereinsprofil.

ERWARTUNGEN
  • Kein horizontales Scrollen.
  • Chips sind mind. 44 × 44 px (mit Touch zielbar).
  • Cover-Bilder laden lazy, keine Voll-Lade-Welle beim Initial-Render.

REPORTING
  ERGEBNIS BETA-O01 (Status, Dauer, Test-Daten inkl. emuliertem Gerät,
  Verstöße, UX-Notizen, Screenshot, Vorschlag).
```

### BETA-O02 — Mehrsprachigkeit DE/FR/IT

```
Du bist Petra. Schalte zwischen den Sprachen und prüfe die Konsistenz.

VORBEDINGUNG
  Nicht eingeloggt.

ABLAUF
  1. Marktplatz öffnen, Sprache im Footer auf FR umschalten.
  2. Begriffe prüfen: "Sport-Sponsoring", "Anfrage", "Rechnung" — alle
     übersetzt? Notiere unübersetzt gebliebene Strings.
  3. Branche-Anzeigenamen prüfen — auch in FR/IT übersetzt?
  4. CHF-/Datum-Format anschauen (FR: "1 500 CHF", "15/05/2026" o. ä.).
  5. Seite neu laden — bleibt FR aktiv?
  6. Wiederhole für IT.

ERWARTUNGEN
  • Spracheinstellung persistiert via Cookie über Reload hinweg.
  • Keine Deutsch-Reste in FR/IT-Ansichten (Liste alle Verstöße).
  • Datums- und Währungs-Formate folgen der Locale.

REPORTING
  ERGEBNIS BETA-O02 (Status, Dauer, Test-Daten, Verstöße inkl. konkreter
  unübersetzter Strings, UX-Notizen, Screenshot, Vorschlag).
```

### BETA-O03 — Marken-Landing-Page

```
Du bist Petra (oder Sandra, anonym). Prüfe die Marken-Landing-Page.

VORBEDINGUNG
  Nicht eingeloggt.

ABLAUF
  1. Footer-Link "Für Marken" klicken.
  2. Statistik beobachten: Vereine pro Branche, aktive Projekte.
  3. CTA "Sponsor-Konto erstellen" anklicken — Ziel-URL prüfen.

ERWARTUNGEN
  • Statistik ist live aus Datenbank, nicht hardcoded. Werte > 0 plausibel.
  • Health-Branche-Verteilung sichtbar (z. B. als Balken oder Liste).
  • CTA führt zu /sponsor/registrieren.

REPORTING
  ERGEBNIS BETA-O03 (Status, Dauer, Test-Daten, Verstöße, UX-Notizen,
  Screenshot, Vorschlag).
```

### BETA-O04 — Performance-Smoke

```
Du bist Lea oder Petra. Profiliere den Marktplatz-Ladevorgang.

VORBEDINGUNG
  Nicht eingeloggt.

ABLAUF
  1. Chrome DevTools → Network-Tab öffnen, Cache deaktivieren.
  2. Marktplatz laden, Initial-Load-Time protokollieren.
  3. Throttling auf "Slow 4G" setzen, neu laden.
  4. Cover-Bild-Lade-Verhalten beobachten: Lazy-Loading aktiv?
  5. Auf 5XX-Errors achten.

ERWARTUNGEN
  • Erst-Ladezeit < 3 s p95 auf Slow 4G.
  • Cover-Bilder werden lazy geladen, nicht pre-fetched.
  • Kein 500/503.

REPORTING
  ERGEBNIS BETA-O04 (Status, Dauer inkl. konkreter Load-Time-Werte,
  Verstöße, UX-Notizen, Screenshot, Vorschlag).
```

---

## 5. Hinweise zur Plugin-Konfiguration

- **Sicherheits-Hinweis:** Die Demo-Umgebung enthält keine echten Daten. Trotzdem:
  Plugin-Zugriff auf Datatrans-, Banking- oder Mail-Provider-Tabs sicherheitshalber
  ablehnen — Beta-Test bleibt strikt auf `sponsorplatz.for-better.biz`.
- **Sitzungs-Isolation:** Vor jedem Prompt empfiehlt sich `Inkognito-Tab` oder
  ein Profil-Wechsel — sonst überlagern sich Logins.
- **Mail-Verifikation = User-Interaktion:** Der Agent öffnet keinen
  Mail-Catcher. Jeder mail-relevante Schritt pausiert und wartet auf "erledigt"
  vom Tester (Fabian). Siehe §0.1 Konvention oben.
- **Geschwindigkeits-Schraube:** Wenn der Plugin-Lauf zu schnell ist und
  Race-Conditions auftreten (z. B. Notification-Polling), `wait 2s` zwischen
  kritischen Schritten einbauen.
- **Reporting-Workflow:** Plugin-Outputs als Issue im Repo `sponsorplatz` mit
  Label `beta-feedback` ablegen, Vorlage in BETA_TESTPLAN.md §10.

---

**Pflege:** Bei neuen Szenarien in `BETA_TESTPLAN.md` jeweils einen passenden
Prompt-Block hier ergänzen. Die Numerierung muss synchron bleiben.

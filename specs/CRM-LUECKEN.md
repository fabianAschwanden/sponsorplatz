# CRM-Lücken-Analyse — Sponsorplatz aus Unternehmenssicht

> **Stand:** 28. Mai 2026 · **Code-Verifikation:** gegen V46 (28.05.2026)
> **Perspektive:** Sponsoring-Verantwortliche/r einer Krankenkasse vom Schlag
> CSS, die ihr Portfolio gesponsorter Vereine als Account-Portfolio
> professionell managen muss. 50–200 aktive Engagements pro Jahr.
> **Quellen:** `PROJEKT_INFO.md`, `ROADMAP.md`, `DATENMODELL.md` (V1–V46),
> `TECHNISCHE_SPEZIFIKATION.md`, `ROLLENKONZEPT.md`, `KONTAKT_ANFRAGE_VERTRAG.md`,
> `SPONSORING_ZAHLUNGSFLUSS.md`, `TESTSTRATEGIE.md`.

---

## Executive Summary — Top 5 Lücken

| # | Lücke | Warum kritisch |
|---|---|---|
| **1** | Kontakt-/Activity-Management auf Vereinsebene | Sponsoring ist Beziehungsgeschäft. Heute keine `KontaktPerson`- oder `Aktivitaet`-Entität — Excel wird parallel weiterleben |
| **2** | Renewal-Management (Fristen, Reminder, Pipeline) | 50–200 Engagements = 50–200 Renewals/Jahr. `Vertrag` hat Laufzeit-Felder (`laufzeit_von`/`laufzeit_bis`), aber kein Reminder-/Pipeline-Tooling darauf |
| **3** | Approval-Workflow mit Vier-Augen-Prinzip | Compliance-Pflicht in Krankenkassen. Heute kann ein einzelner ORG_OWNER ohne Co-Signoff unterzeichnen |
| **4** | Budget-/Forecast-Datenmodell mit Pipeline-Stages | Sponsoring-Abteilung muss GL Budget-Forecast belegen. Heute nur Rückspiegel (unterzeichnete Verträge), kein Cockpit |
| **5** | Wirkungsmessung / strukturierte Deliverable-Erfüllung | Spec verspricht «messbar», Datenmodell hat keine Wirkungs-Felder, keinen Nachweis-Upload, keinen Soll-Ist-Vergleich |

---

## Detail pro Themenfeld

### 1. Account-Management (Vereins-Stammdaten als Account)

**Was die Spec heute abdeckt:** `Organisation`-Entität mit Stammdaten, Branche-Enum, hierarchische Org-Struktur (3 Stufen, V24), `SponsorBranche` getrennt von Vereins-Branche, Public Vereinsprofil + Engagement-Schaufenster.

**Was fehlt:**
- Keine internen Account-Felder aus Sponsoren-Sicht: kein **Account-Owner** im CSS-Team, kein **Account-Status** (Hot Lead / Active / In Renewal / Lost / Do-Not-Engage), keine **Account-Tier-Einstufung** (Strategic / Core / Long-Tail).
- Keine sponsor-interne Account-Notiz ohne Vereinssicht — PROJEKT_INFO.md verspricht «Notizen pro Verein», DATENMODELL hat keine `notiz`-Tabelle, nur `Nachricht` (Konversations-Thread auf ANGENOMMEN-Anfrage).
- Keine Segment-/Tagging-Funktion auf Verein aus Sponsor-Sicht («Marathon-Verein», «Frauensport», «Schwerpunkt-Zielgruppe 50+»).
- Keine strukturierte Region/Geo-Felder ausser Ort — Kanton ist Filter im Schaufenster, kein Feld auf Org.

**Priorität: hoch.**

### 2. Kontakt-Management (Personen-Ebene am Verein)

**Was die Spec heute abdeckt:** `AppUser` (V3) als Plattform-User, `Mitgliedschaft` als User↔Org-Verknüpfung.

**Was fehlt:**
- **Keine `KontaktPerson`-Entität als CRM-Kontakt** — nur registrierte AppUser. CSS-Sponsoring-Manager kennt aber Präsident, Pressesprecher, Trainer, die *keinen* Sponsorplatz-Account haben und nie haben werden.
- Kein Konzept «Hauptansprechpartner» / «Stellvertreter» / «Buchhaltungs-Kontakt» — `Mitgliedschaft.rolle` ist Edit-Rollen-orientiert, nicht funktional.
- Keine Visitenkarten-Felder für externe Kontakte (Direktnummer, Mobile, persönliche E-Mail, LinkedIn).
- Kein Decision-Maker-Mapping.

**Priorität: hoch.**

### 3. Activity-Tracking (Interaktions-Historie)

**Was die Spec heute abdeckt:** `AuditLog` (V14) für Systemaktionen, `Nachricht` (V13) für post-deal-Konversation, `Benachrichtigung` (V19) als In-App-Glocke.

**Was fehlt:**
- Keine frei erfassbare Aktivität — kein Call-Log, kein Besuchsbericht, keine Meeting-Notiz, keine Mail-Notiz.
- Keine Activity-Timeline pro Verein über alle Touchpoints.
- Keine Activity-Typen (Call / Email / Meeting / Event-Besuch).
- Keine Verknüpfung Aktivität ↔ Kontakt-Person.
- AuditLog ist System-getrieben, nicht Benutzer-lesbar (ROLLENKONZEPT: nur PLATFORM_ADMIN).

**Priorität: hoch.**

### 4. Pipeline-Management (Forecast über Anfragen hinaus)

**Was die Spec heute abdeckt:** `SponsoringAnfrage` mit Status NEU/ANGENOMMEN/ABGELEHNT, zwei Anfrage-Typen (Paket / Kontakt), Pipeline-Statistik mit Conversion-Rate, `Vertrag` ENTWURF/UNTERZEICHNET/GEKUENDIGT.

**Was fehlt:**
- Keine Pipeline-Stages vor der Anfrage: keine «Lead-Phase», keine «Qualifizierung», keine «Interesse signalisiert», keine «Angebot raus, warten».
- Kein Forecast-Betrag auf Pipeline-Stufen — ich kann nicht sagen «CHF 350'000 in der Pipeline für H2, davon CHF 120'000 wahrscheinlich».
- Keine strukturierten Win-/Loss-Gründe — ABGELEHNT ist Boolean.
- Keine Pipeline-Velocity-Kennzahlen (Time-in-Stage, Time-to-Close).

**Priorität: hoch.**

### 5. Approval-Workflows (interne Genehmigung)

**Was die Spec heute abdeckt:** Org-Rollen ORG_OWNER/EDITOR/VIEWER, `AufgabenEngine` (V36) für Status-getriggerte Tasks.

**Was fehlt:**
- Kein Vier-Augen-Prinzip auf Vertrag-Unterzeichnung — ein einzelner ORG_OWNER kann `markiereUnterzeichnet` ohne Co-Signoff setzen.
- Keine betrags-abhängigen Genehmigungs-Stufen (< CHF 5'000 / CHF 5–50'000 / > CHF 50'000).
- Keine Genehmigungs-Historie.
- Aufgaben-Engine ist Erinnerung, kein State-Gate.

**Priorität: hoch.** Compliance-Anforderung in Krankenkassen — ohne Vier-Augen kein Vertragsabschluss in der Realität.

### 6. Renewals-Management (Vertragsverlängerung)

**Was die Spec heute abdeckt:** `Vertrag` mit terminalem Status GEKUENDIGT **und Laufzeit-Feldern `laufzeit_von`/`laufzeit_bis` (LocalDate, im Entity vorhanden)**. «Wiederkehrende Sponsoring-Abos» ist Phase-15.5-Backlog.

> **Korrektur (V46-Verifikation):** Frühere Analyse-Fassung behauptete «keine Laufzeit-Felder». Falsch — die Datenbasis für Renewals ist da. Die Lücke ist nicht das **Datenmodell**, sondern das **Tooling** darauf. Dadurch sinkt der Aufwand für #6 erheblich: kein Schema-Umbau, nur Query + Reminder-Job + View.

**Was fehlt (Tooling auf vorhandener Laufzeit):**
- Kein Renewal-Reminder 90 Tage vor Vertragsablauf.
- Kein Renewal-Pipeline-View («23 Verträge laufen H2 aus»).
- Kein Vertrags-Verlängerungs-Pfad als eigene Status-Transition — heute «neuer Vertrag», Historie nicht verlinkt.
- Keine Multi-Year-Verträge als First-Class-Konzept (CHF 30'000 über 3 Jahre).
- Kein Renewal-Conversion-Tracking.

**Priorität: hoch.**

### 7. Budget-/Forecast-Management

**Was die Spec heute abdeckt:** `SponsorStatistikService` aggregiert Sponsoring-Volumen über UNTERZEICHNETe Verträge (Phase 11.12).

**Was fehlt:**
- Keine Budget-Hierarchie («Jahresbudget 2026 = CHF 1.2 Mio, Region Zürich CHF 300k, davon Sport CHF 200k»).
- Kein Brutto-vs-Netto-Forecast (zugesagt vs. ausbezahlt vs. wahrscheinlich).
- Keine Cost-Center-/Konto-Zuordnung pro Vertrag.
- Kein Soll-Ist-Vergleich auf Account-Ebene.

**Priorität: hoch.**

### 8. Reporting / Management-Reporting

**Was die Spec heute abdeckt:** `/statistiken`-Route (Phase 11.12) — aktive Engagements, Volumen, Pipeline, Conversion, Rechnungs-Status, Branchen-/Vertrags-Verteilung.

**Was fehlt:**
- Keine Export-Funktion (Excel/CSV/PDF) für Statistik-Sicht.
- Keine Stichtag-/Zeitreihen-Ansicht («Wo standen wir am 31.12.2025?»).
- Keine kundenseitigen Filter / Save-Reports.
- Keine scheduled Reports («jeden 1. Monatsmail mit Pipeline-Snapshot»).
- Kein CSV-Export pro Sponsor-Org der eigenen Verträge.

**Priorität: hoch.**

### 9. Wirkungsmessung / Sponsoring-KPIs

**Was die Spec heute abdeckt:** Positionierung «Lokal. Messbar. Ehrlich.» (PROJEKT_INFO.md), Sponsorplatz als «CRM + Marktplatz», `Event`-Entity (V26) nicht mit KPIs verknüpft.

**Was fehlt:**
- Keine Wirkungs-Felder auf Vertrag/Engagement: Trikot-Logos × Spielanzahl × Zuschauer, Banner-Tage, Social-Reach, Newsletter-Nennungen.
- Kein Leistungs-Nachweis-Upload (Trikot-Foto, Banner-Foto, Event-Bericht) als Vertragserfüllung.
- Keine Soll-Ist-Leistungen aus Sponsor-Sicht — `Vertrag.leistungVerein` ist Freitext, kein strukturierter Deliverable-Katalog.
- Keine ROI-Berechnung (CPM-Äquivalent).
- Engagement-Schaufenster zeigt eingelöste Sponsorings ohne Wirkungs-Layer.

**Priorität: hoch.** Existenz-Berechtigung der Sponsoring-Abteilung hängt am ROI-Nachweis.

### 10. Dokumenten-Management

**Was die Spec heute abdeckt:** `MedienAsset` (V11) als polymorpher Datei-Upload auf PROJEKT/ORGANISATION/USER, Vertrag-PDF + Rechnung-PDF generiert.

**Was fehlt:**
- Keine Dokumente an Verträgen — NDA, Side-Letter, signiertes Original (Scan), Kündigungsschreiben. `MedienAsset.entity_typ` listet kein VERTRAG.
- Keine Versionierung (V1/V2/Original/Geprüft).
- Keine Vertragsmuster-Bibliothek pro Sponsor-Org.
- Kein Volltext-Index über Vertragsanhänge.
- Keine DSG-Aufbewahrungsfrist-Markierung.

**Priorität: mittel.** Sharepoint/Drive als Workaround möglich.

### 11. Integration zu Finanz-/Buchhaltungs-Systemen

**Was die Spec heute abdeckt:** Swiss-QR-Bill outgoing (Zahlungsfluss §4), Stripe/PostFinance/Datatrans im Backlog (§15.1), CSV-Export + camt.054 + Bexio/Sage-API als Phase-12+-Backlog.

**Was fehlt:**
- Keine SAP/Abacus/Bexio-Integration für Sponsor-seitige Verbindlichkeits-Buchung.
- Keine Kostenstellen-/Innenauftrags-Übermittlung.
- Keine bidirektionale Reconciliation (ist Zahlung im SAP rausgegangen?).
- Keine MwSt-Behandlung aus Sponsor-Sicht (Phase-12-Backlog).
- Keine Treasury-/Cashflow-Sicht.

**Priorität: mittel-hoch.** Pilot via Excel-Brücke, für Skalierung Pflicht.

### 12. Datenschutz / Audit aus Sponsoren-Compliance-Sicht

**Was die Spec heute abdeckt:** `AuditLog` (V14) + DSG-Datenexport pro User, Sentry-IP-Filter (Phase 10.2), Cross-Cloud-Audit-Marker (V41).

**Was fehlt:**
- Keine DSG-Aufbewahrungsfrist-Steuerung auf Account-Ebene.
- Keine Field-Level-Permissions (z. B. Bankverbindungs-Feld eines fremden Sponsors).
- **Keine Mandantentrennung für Sponsor-Privatsphäre** — ROLLENKONZEPT sagt explizit «Alle eingeloggten Benutzer sehen alle Daten» und «Sponsor-Stammdaten geteilt (Wikipedia-Modell)». **Aus CSS-Compliance-Sicht ist das kein erlaubtes Modell** für interne Notizen/Pipeline.
- Keine Data-Loss-Prevention auf Export.

**Priorität: hoch.** Wikipedia-Modell ist die fundamentale Architektur-Frage, die im Enterprise-Sponsoring nicht trägt.

### 13. Multi-User-/Multi-Standort (Sponsor-Team)

**Was die Spec heute abdeckt:** Hierarchische Org-Struktur (3 Stufen, V24), Mitgliedschaft mit Owner/Editor/Viewer, OIDC-Login (Phase 13.3).

**Was fehlt:**
- 3-Stufen-Hierarchie zu flach für CSS-Realität (Konzern → Region → Marketing/Sponsoring → Team-Lead → Account-Owner = 5 Stufen).
- Keine Account-Owner-Zuweisung pro Verein.
- Keine Stellvertreter-Logik bei Ferien/Krankheit.
- Kein Team-Performance-Tracking pro Account-Owner.

**Priorität: mittel-hoch.**

### 14. Sponsoring-Catalog / Recherche-Funktionen für Sponsoren

**Was die Spec heute abdeckt:** Marktplatz-Filter, Watchlist (V7, projekt-bezogen), Matching via `MatchingService` (Branchen-Match), Engagement-Schaufenster.

**Was fehlt:**
- Watchlist ist projekt-bezogen, nicht **verein-bezogen** — ich kann keinen Verein als «Pipeline-Lead» markieren.
- Keine Save-Search / Smart-List («alle neu verifizierten Vereine in Kanton BE der letzten 30 Tage, Branche Mental Health → Mail-Alert»).
- Keine Konkurrenz-Sichtbarkeit-Alerts («Verein, den ich beobachte, hat Engagement bei Helsana eingegangen»).
- Keine Vereins-Profil-Vergleichsansicht.
- Matching ist Branchen-basiert, nicht Strategie-basiert.

**Priorität: mittel.**

### 15. Marketing-Aktivierung / Outbound an Sponsoren-Bestand

**Was die Spec heute abdeckt:** E-Mail via `BenachrichtigungsService` für Anfrage-Lifecycle, In-App-Glocke, Engagement-Schaufenster, OG-Card-Generator.

**Was fehlt:**
- Kein Bulk-Mail / Newsletter an alle gesponsorten Vereine — geht heute zu Mailchimp.
- Keine Mail-Vorlagen-Bibliothek (Renewal-Anschreiben, Glückwunsch, Sponsoring-Bestätigung).
- Kein Mail-Open-/Click-Tracking auf Sponsor-Outbound.
- Keine Kampagnen-Verknüpfung («CSS Bewegt 2026» mit 30 zugeordneten Vereinen).
- Keine Veranstaltungs-Einladungs-Funktion.

**Priorität: mittel.**

### 16. Datenmigration / Import aus Excel-Bestand

**Was die Spec heute abdeckt:** DSG-Datenexport, DB-Backup, `DemoSeedRunner` für Demo-Daten.

**Was fehlt:**
- **Kein CSV/Excel-Import** für Bestands-Verträge, -Vereine, -Kontakte beim Onboarding der Sponsor-Org. 200 bestehende Engagements werden *nicht* händisch eingetippt.
- Keine Bestands-Migrations-Spec («so kommen Sie aus Excel rein»).
- Kein API-Endpunkt zum Anlegen von Verträgen/Anfragen ausser REST-API-Skelett mit `X-API-Key` (off-by-default).

**Priorität: hoch.** Adoption-Killer Nr. 1 für jeden bestehenden Sponsor.

---

## Architektur-Implikation — das Wikipedia-Modell

Eine fundamentale Beobachtung: Die aktuelle Spec ist konsequent auf das
«Wikipedia-Modell» ausgerichtet (Konzept v3, kollaborative Plattform, geteilte
Sponsor-Stammdaten, «alle eingeloggten Benutzer sehen alle Daten»).

Aus Vereins- und Marktplatz-Sicht ist das ein bewusst guter Ansatz. **Aus
Unternehmens-CRM-Sicht ist es eine Sackgasse**:

- Pipeline-Daten, Account-Notizen, interne Forecast-Beträge dürfen nicht
  öffentlich oder zwischen Sponsoren geteilt sein.
- Compliance-Pflicht zur Mandantentrennung lässt sich nicht über
  Edit-Rollen erschlagen.
- Ein Sponsor mit 200 Engagements pflegt einen Teil seines Wissens
  konkurrenzkritisch — das passt nicht zum geteilten Stammdaten-Modell.

**Empfehlung:** Vor dem GreenBox-Pitch eine explizite Architektur-Entscheidung
treffen: bleibt das Wikipedia-Modell, dann ist Sponsorplatz primär eine
Marketplace-Discovery-Plattform mit CRM-light. Soll es vollwertiges CRM
werden, braucht es eine **private Layer pro Sponsor** mit klarer Datenisolation
(siehe ADR-0003 «Kollaborative Plattform statt Multi-Tenant» — der müsste
gegebenenfalls superseded werden durch ADR-00XX «Hybrid: kollaborative
Stammdaten + private Sponsor-Daten»).

---

## Vorgehens-Vorschlag

Die 16 Lücken zerfallen in vier Cluster in **Abhängigkeits-Reihenfolge** — die
Reihenfolge ist nicht verhandelbar, weil Cluster 1 die Voraussetzung für alle
anderen ist. CRM ist eine **eigene Phase nach dem Pilot** (Arbeitstitel
«Phase 16 — Sponsor-CRM»), nicht Teil der Produktivschaltung (Phase 14).

### Schritt 0 — Validieren bevor gebaut wird

Kein CRM-Code vor den Discovery-Interviews (Fragen unten). Erst wenn ≥3 von 5
Antworten die Analyse stützen, ist der CRM-Pfad bestätigt. Pilot-Phase 14 läuft
unabhängig weiter — der Pilot mit 5 Vereinen + 3 Marken liefert das
Realdaten-Feedback, das die CRM-Priorisierung schärft.

### Cluster 1 — Architektur-Weiche: Hybrid-Modell (das Fundament)

**Das ist eine Entscheidung, kein Feature.** Empfehlung: ADR-0003 *nicht* kippen,
sondern **erweitern** — additiv statt Umbau:

- **Öffentlich/kollaborativ bleibt:** `Organisation`, `Projekt`, `SponsoringPaket`,
  `MedienAsset`, Marktplatz, Engagement-Schaufenster. Das Wikipedia-Modell ist
  für die Discovery-Seite ein Feature, kein Bug.
- **Neu privat pro Sponsor:** alle CRM-Entitäten (Cluster 2–3) bekommen eine
  `besitzer_sponsor_org_id` und sind **ausschliesslich** für Mitglieder dieser
  Sponsor-Org sichtbar. Neue `AccessControl`-Methode `kannSponsorDatenSehen(...)`
  + eine ArchUnit-Regel, die sicherstellt dass CRM-Repositories nie ohne
  Sponsor-Filter abgefragt werden.

Vorteil: keine Migration der Bestandsdaten, kein Bruch am Marktplatz. Die private
Layer ist eine zusätzliche, isolierte Schicht — die Plattform wird hybrid, nicht
neu gebaut.

> **TDD-Pflicht:** Die Isolations-Garantie braucht zuerst einen ArchUnit-/
> Integrationstest «Sponsor A sieht Sponsor-B-CRM-Daten NICHT», der rot ist,
> bevor die erste CRM-Entität entsteht. Diese Regel ist das Sicherheitsnetz für
> alle folgenden Slices.

### Cluster 2 — CRM-Kern-Entitäten (in der privaten Layer)

Drei neue Aggregate, jeweils mit eigenem View-DTO + Mapping-Test (View-Pflicht):

1. **`SponsorAccount`** — die Beziehung Sponsor-Org ↔ gesponsertem Verein als
   First-Class-Entität: `account_owner` (AppUser im Sponsor-Team), `status`
   (LEAD/AKTIV/IN_RENEWAL/VERLOREN/DO_NOT_ENGAGE), `tier` (STRATEGIC/CORE/LONG_TAIL),
   `tags`, private `notiz`. Hängt an `besitzer_sponsor_org_id`.
2. **`KontaktPerson`** — externe Ansprechpartner ohne Plattform-Account
   (Präsident, Trainer, Pressesprecher): Name, Funktion, Direktnummer, Mobile,
   Mail, optional verknüpft mit Verein-Org. Sponsor-privat.
3. **`Aktivitaet`** — Activity-Log: Typ (CALL/EMAIL/MEETING/EVENT_BESUCH),
   Datum, Notiz, optional verknüpft mit `KontaktPerson`. Liefert die
   Activity-Timeline pro Account.

Damit sind Lücken #1, #2, #3 geschlossen.

### Cluster 3 — Portfolio-Steuerung (Tooling auf Cluster 2 + bestehenden Daten)

- **Renewals (#6)** — **Quick Win**, weil `laufzeit_bis` schon existiert:
  Reminder-Job (analog `OpsAlertJob`) + Renewal-Pipeline-View
  («23 Verträge laufen H2 aus»). Kein Schema-Umbau.
- **Pipeline-Stages (#4)** — Stages *vor* der Anfrage auf `SponsorAccount`
  (LEAD → QUALIFIZIERT → ANGEBOT → GEWONNEN/VERLOREN) + Forecast-Betrag +
  Win/Loss-Grund-Enum.
- **Budget-Cockpit (#7) + Reporting-Export (#8)** — Soll-Ist auf Account-Ebene,
  CSV/Excel-Export der bestehenden `/statistiken`.
- **Wirkungsmessung (#9)** — strukturierter Deliverable-Katalog statt
  `Vertrag.leistungVerein`-Freitext, Nachweis-Upload (braucht Cluster-2-Doku-
  Anbindung: `EntityTyp.VERTRAG` ergänzen, #10).

### Cluster 4 — Adoption-Blocker (parallel zu Cluster 2/3 machbar)

- **CSV/Excel-Import (#16)** — Adoption-Killer Nr. 1. Ohne Import kein Bestands-
  Sponsor. Mapping-UI + Validierungs-Report + Dry-Run.
- **Approval-Workflow (#5)** — Vier-Augen-Prinzip auf Vertrag-Unterzeichnung,
  betragsabhängige Stufen. Compliance-Pflicht — ohne das kein realer
  Vertragsabschluss in einer Krankenkasse.

### Aufwand-Ampel (grobe Schätzung, Solo-Dev Halbtags)

| Cluster | Inhalt | Aufwand | Risiko |
|---|---|:---:|:---:|
| 1 | Hybrid-Architektur + Isolations-Test | mittel | **hoch** (Fundament, security-kritisch) |
| 2 | 3 CRM-Aggregate + Views | groß | mittel |
| 3 | Portfolio-Tooling (Renewal = Quick Win) | groß | niedrig |
| 4 | Import + Approval | mittel | mittel (Import-Datenqualität) |

**Kleinster sinnvoller erster Schritt nach Validierung:** Cluster 1 (Hybrid-
Architektur + Isolations-Test) + der Renewal-Quick-Win aus Cluster 3 — letzterer
zeigt sofort Wert auf bereits vorhandenen Daten, ersterer ist die nicht-
verhandelbare Basis. Damit ist ein vorzeigbarer Vertical Slice da, ohne das
volle CRM zu bauen.

---

## Diskussions-Punkte für die Discovery-Interviews

Aus dieser Lücken-Analyse fünf konkrete Interview-Fragen für Sandra (CSS-Sponsoring-Team) und Marco (CSS-Agentur):

1. **Wie pflegt ihr heute Kontakte und Aktivitäten** pro gesponsortem Verein? Wo werden Telefonate, Besuche, persönliche Beziehungen festgehalten?
2. **Wer entscheidet bei euch ab welchem Betrag** über ein Sponsoring? Wie sieht der Genehmigungsweg aus?
3. **Wie steuert ihr Renewals** über das Portfolio? Welche Vorlauf-Frist nutzt ihr? Wie viele Renewals geht ihr verloren?
4. **Welche Wirkungs-KPIs** verlangt eure Geschäftsleitung pro Engagement? Wie messt ihr das heute?
5. **Wie würdet ihr den Bestand** in eine neue Plattform überführen — habt ihr historische Daten, die mit müssen?

Wenn 3 von 5 Antworten in dieselbe Richtung gehen wie diese Analyse, dann ist
der CRM-Pfad in der GreenBox-Phase unvermeidbar.

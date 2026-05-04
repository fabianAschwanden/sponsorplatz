# Roadmap

> Vollständige Variante mit Iterations-Details: siehe `Sponsoring Plattform/06_Umsetzungsplan.md`.
> Dieses Dokument ist der Spec-Auszug für aktive Entwicklung.

## Aktueller Stand: **Phase 0 — Skelett** ✓

- Spring Boot App lauffähig
- Layout + Index-Seite
- Docker + CI/CD bereit
- Spec-Dokumente im Repo

## Phase 0.1 — Organisation & Mitgliedschaft (2 Wochen)

- [ ] V2: `organisation`-Tabelle, ENUM-Typen
- [ ] `Organisation`-Entity + Repository + Service
- [ ] V3: `app_user`-Tabelle (BCrypt)
- [ ] V4: `mitgliedschaft`-Tabelle
- [ ] `AccessControl`-Bean
- [ ] Tests: ORG-01..03, MG-01..02, AC-01..05

## Phase 1 — Self-Reg & Verifizierung (2 Wochen)

- [ ] Vereins-Self-Registrierung
- [ ] E-Mail-Verifikation mit Token
- [ ] Plattform-Admin-Verifizierungs-Queue
- [ ] Auto-Verifizierung via Zefix
- [ ] Mitglieder-Einladungs-Flow

## Phase 2 — Pakete, Sichtbarkeit & Medien (3 Wochen)

- [ ] `Projekt` mit `sichtbarkeit`, `slug`
- [ ] `SponsoringPaket`-Entity
- [ ] `MedienAsset` mit Storage-Abstraktion
- [ ] Cover/Galerie/Pitch-Deck im Wizard
- [ ] Veröffentlichungs-Flow

## Phase 3 — Marktplatz Public (3-4 Wochen)

- [ ] Public-Routes `/marktplatz/**`
- [ ] Filter (Branche, Region, Datum, Budget)
- [ ] Volltextsuche mit Postgres `tsvector`
- [ ] SEO: Sitemap, Schema.org, Open Graph
- [ ] Public-Vereinsprofile

## Phase 4 — Anfragen & Konversation (3 Wochen)

- [ ] `SponsoringAnfrage`-Entity + Status-Workflow
- [ ] Sponsor-Org-Self-Reg
- [ ] Anfrage-Form auf Paket-Seite
- [ ] Verein-Inbox + Threaded Messages
- [ ] E-Mail-Notifications
- [ ] Bei Annahme: SponsorBeteiligung erzeugen

## Phase 5+ — Wachstum

- Watchlist
- Matching-Empfehlungen
- Mehrsprachigkeit FR/IT
- Vertrags-Generator
- Zahlungs-Integration

## Definition: MVP fertig

- 5+ verifizierte Vereine
- 10+ veröffentlichte Projekte
- 5+ Sponsor-Orgs registriert
- 10+ Anfragen, davon 3+ angenommen
- Test-Suite ≥ 60 Test-Klassen
- DSG-Datenexport funktioniert
- Marktplatz < 300 ms p95

# ADR-0005: Föderierte Identitäten in eigener Tabelle

## Status
Akzeptiert

## Datum
2026-05-08

## Kontext

Mit der Kickbox-Initiative „CSS Sponsoring-Hub" wird OIDC-SSO via Microsoft
Entra ID zum Backlog-Feature (Phase 1.4, siehe [`AUTH_SSO_OIDC.md`](../../specs/AUTH_SSO_OIDC.md)).
Die Speicherung der Provider-Identität-Verknüpfung kann auf zwei Wegen erfolgen:

- **A) Spalten am `app_user`:** `oidc_provider`, `oidc_subject`, `oidc_verbunden_am`.
  Einfach, ein Provider pro User.
- **B) Eigene Tabelle `federierte_identitaet`:** Beziehungs-Entity zwischen `app_user`
  und `(provider, subject)`. Mehrere Provider pro User möglich.

Aktuelle Realität: nur Entra ID ist geplant. Aber:

- Mittelfristig könnten sich Vereinsmitglieder via Google/Apple einloggen wollen (B2C-Federation).
- Verbands-Partner mit eigenem OIDC-Provider könnten dazukommen.
- Account-Verknüpfung (User hat Form-Login UND Entra-SSO mit gleicher E-Mail) ist
  ein häufiger Fall.

## Entscheidung

Wir verwenden **Variante B** — eigene Tabelle `federierte_identitaet` (Migration V25).

Schema:

```sql
CREATE TABLE federierte_identitaet (
    id                UUID         PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider          VARCHAR(50)  NOT NULL,
    subject           VARCHAR(255) NOT NULL,
    email_at_provider VARCHAR(255),
    verbunden_am      TIMESTAMP    NOT NULL,
    letzter_login_am  TIMESTAMP,

    CONSTRAINT chk_provider CHECK (provider IN ('ENTRA_ID')),
    CONSTRAINT uq_provider_subject UNIQUE (provider, subject)
);
```

Bei Provider-Erweiterung wird die `chk_provider`-Constraint per Migration
ergänzt (Werte sind White-Listed, kein freier Provider-String).

Identity-Mapping-Logik im `SponsorplatzOidcUserService`:

1. Lookup via `(provider, subject)` — der stabile Pfad
2. Falls Miss: Email-Match auf `app_user.email` → Auto-Verknüpfung
3. Falls auch Miss: Just-in-Time-Provisionierung neuer `AppUser`

## Konsequenzen

**Positiv:**

- **Mehrere Provider pro User** möglich, ohne Schema-Change.
- **Provider-Whitelisting via Constraint** — neue Provider verlangen bewusste Migration.
- **`letzter_login_am`** pro Provider trackbar (Analytics, Audit).
- **Lifecycle-Trennung:** Provider-Verknüpfung ist eine separate Entity mit eigenem Audit-Trail, nicht ein paar User-Spalten.
- **DELETE CASCADE auf `app_user`** sichert konsistentes Aufräumen.

**Negativ:**

- Ein zusätzlicher Repository-/Service-Aufwand gegenüber Spalten-Lösung.
- Ein zusätzlicher JOIN beim Login-Lookup. Vernachlässigbar bei Pilot-Volumen.

## Alternativen

- **A) Spalten am `app_user`** verworfen — siehe Kontext. Schmerz beim ersten zusätzlichen Provider wäre hoch (Schema-Migration plus alle Lookup-Pfade umbauen).
- **C) Frei-String-Provider ohne Constraint** verworfen — verletzt Defense-in-depth. Wenn ein Bug einen falschen Provider-Wert schreibt, soll die DB es ablehnen.
- **D) Subject als alleiniger Schlüssel** (ohne `provider`-Spalte) verworfen — Subjects können bei einigen Providern wiederverwendet werden (unwahrscheinlich, aber theoretisch). Composite-UNIQUE `(provider, subject)` ist die robuste Wahl.

## Referenzen

- [`specs/AUTH_SSO_OIDC.md`](../../specs/AUTH_SSO_OIDC.md) §5 Datenmodell
- [`specs/AUTH_SSO_OIDC.md`](../../specs/AUTH_SSO_OIDC.md) §6 Identity-Mapping
- Migration V25 (TBD, Implementation Phase 1.4)
- Test-IDs SSO-11/12 — Repository-Tests
- Spring Security 6 OAuth2 Client — kompatibel mit eigener UserService-Lookup-Logik

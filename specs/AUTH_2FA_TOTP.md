# 2FA — Time-based One-Time Password (TOTP)

> **Phase 13.2** — Backlog-Item V39 (Priorität HOCH).
> Voraussetzung für Produktivschaltung (Phase 14), weil PLATFORM_ADMIN-Konten
> dann reale Daten editieren.

## Ziel

Zweiter Faktor beim Form-Login für **PLATFORM_ADMIN-Konten verpflichtend**,
für alle anderen Konten **optional**. OIDC-Logins ([Phase 13.3](AUTH_SSO_OIDC.md))
bringen den 2. Faktor vom IdP mit — kein eigener TOTP-Schritt nötig.

**Bewusst kein SMS-Code:** SIM-Swap-Angriffe sind realistisches Risiko, TOTP
ist resistenter und kostet nichts laufend.

## Standards + Library

- **Algorithmus:** TOTP nach RFC 6238 (HMAC-SHA1, 30s-Window, 6 Ziffern)
- **Library:** [`dev.samstevens.totp`](https://github.com/samdjstevens/java-totp) — schmaler Wrapper um die RFC 6238-Logik, plus QR-Code-Generator (ZXing-basiert)
- **Authenticator-Apps:** alle marktüblichen (Google Authenticator, 1Password, Authy, Microsoft Authenticator) — keine App-spezifische Anpassung
- **Issuer-Label im OTPAuth-URL:** `Sponsorplatz` — taucht im Authenticator als Header über dem 6-stelligen Code auf

## Datenmodell

### Migration V43 — `app_user`-Erweiterung

```sql
ALTER TABLE app_user
    ADD COLUMN totp_secret VARCHAR(64),                       -- Base32-encoded Secret (nullable = 2FA nicht aktiv)
    ADD COLUMN totp_aktiviert_am TIMESTAMP WITH TIME ZONE,    -- nullable; gesetzt bei erfolgreicher Aktivierung
    ADD COLUMN totp_backup_codes_hashed TEXT;                 -- JSON-Array mit BCrypt-Hashes der noch nicht verbrauchten Codes
```

**Speicher-Strategie:**
- `totp_secret` ist **Klartext** in der DB (Base32-encoded). Bei Vollkompromittierung der DB wäre auch das Passwort-Hash kompromittiert — TOTP-Secret-Verschlüsselung wäre zusätzliche Komplexität ohne realen Mehrwert. Vault/KMS später Phase 14+.
- `totp_backup_codes_hashed` ist ein **JSON-Array von BCrypt-Hashes** (≤ 10 Codes). Pro Verbrauch wird der Hash aus dem Array entfernt — kein "verbraucht"-Flag, sondern hard delete.

### Migration V44 (Folge) — *optional*

Wenn später Hardware-Token (FIDO2/Passkey) als alternativer 2.-Faktor dazukommen, wird eine eigene Tabelle `app_user_zweitfaktor` aufgespannt. Für 13.2 reicht TOTP auf der User-Zeile.

## Setup-Flow (`/einstellungen/2fa`)

```
                          eingeloggt + nicht-aktiv
GET /einstellungen/2fa  ────────────────────────►  Setup-Seite
                                                    - QR-Code (otpauth://totp/Sponsorplatz:user@email?secret=BASE32&issuer=Sponsorplatz)
                                                    - Manueller Code (Base32)
                                                    - Form: 6-stelliger Verify-Code
                                                          │
                                                          ▼
POST /einstellungen/2fa/aktivieren                   Backup-Codes-Seite
                                                    - 10 Codes (einmalig, NICHT mehr abrufbar)
                                                    - "Ausgedruckt / gespeichert"-Bestätigung pflicht


                          eingeloggt + aktiv
GET /einstellungen/2fa  ────────────────────────►  Status-Seite
                                                    - "Aktiv seit: dd.mm.yyyy"
                                                    - "Verbleibende Backup-Codes: N"
                                                    - Button "Backup-Codes neu generieren" (verlangt TOTP-Eingabe)
                                                    - Button "2FA deaktivieren" (verlangt Passwort + TOTP)


POST /einstellungen/2fa/deaktivieren                 Setup-Seite (Re-Setup nötig)
   (verlangt currentPasswort + totpCode)
```

## Login-Flow (Slice B)

```
POST /login (Username + Passwort)
    │
    ├── Passwort falsch ─────────► /login?error
    │
    └── Passwort korrekt:
        │
        ├── User hat 2FA aktiv ──► Pre-Auth-Token in Session, Redirect /login/2fa
        │                                │
        │                                ▼
        │                          GET /login/2fa  ──►  Form (6-stellig + "Backup-Code verwenden"-Link)
        │                                │
        │                                ▼
        │                          POST /login/2fa
        │                                │
        │                                ├── Code korrekt (TOTP oder Backup) ──► volle Authentication, Redirect /dashboard
        │                                ├── Code falsch (n-tes Mal) ────────────► /login/2fa?error
        │                                └── 5 Fehlversuche in 15 min ────────────► Session terminate, Pre-Auth-Token weg, Audit LOGIN_2FA_LOCKOUT
        │
        └── User hat 2FA NICHT aktiv:
            │
            ├── User ist PLATFORM_ADMIN ─► Auth, aber Redirect /einstellungen/2fa/zwang (Skip nicht möglich, Slice C)
            └── User ist normaler User  ─► volle Authentication, Redirect /dashboard (nudge auf /einstellungen erst nach 7 Tagen)
```

## Audit-Aktionen (neu)

| Konstante | Wann |
|---|---|
| `TOTP_AKTIVIERT` | User aktiviert 2FA erfolgreich |
| `TOTP_DEAKTIVIERT` | User deaktiviert 2FA selbst |
| `TOTP_LOGIN_OK` | 2FA-Login bestanden |
| `TOTP_LOGIN_FAIL` | TOTP-Eingabe falsch (Detail: Versuch n/5) |
| `TOTP_BACKUP_CODE_GENUTZT` | Login via Backup-Code |
| `TOTP_BACKUP_CODES_NEU` | User regeneriert Backup-Codes |
| `TOTP_RECOVERY_DURCH_ADMIN` | PLATFORM_ADMIN setzt fremdes 2FA zurück |
| `LOGIN_2FA_LOCKOUT` | 5 Fehlversuche → Pre-Auth-Token invalidiert |

## Tests (AUTH-2FA-01..N)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **AUTH-2FA-01** | `TotpServiceTest` | `generateSecret` produziert Base32-konformes 32-Zeichen-Secret |
| **AUTH-2FA-02** | `TotpServiceTest` | `verifyCode` akzeptiert aktuellen Code, lehnt falschen ab |
| **AUTH-2FA-03** | `TotpServiceTest` | Replay-Window 1 Step (±30s) wird akzeptiert, 2 Steps abgelehnt |
| **AUTH-2FA-04** | `TotpServiceTest` | `generateBackupCodes` produziert 10 unique Codes, BCrypt-gehasht |
| **AUTH-2FA-05** | `TotpServiceTest` | `verifyBackupCode` matches einen Code, entfernt ihn aus der Liste |
| **AUTH-2FA-06** | `TwoFaSetupControllerTest` | GET `/einstellungen/2fa` zeigt QR-Code für nicht-aktive User |
| **AUTH-2FA-07** | `TwoFaSetupControllerTest` | POST `/aktivieren` mit korrektem Code → `totp_aktiviert_am` gesetzt, Backup-Codes generiert + angezeigt |
| **AUTH-2FA-08** | `TwoFaSetupControllerTest` | POST `/aktivieren` mit falschem Code → 400, kein DB-Update |
| **AUTH-2FA-09** | `TwoFaSetupControllerTest` | POST `/deaktivieren` ohne Passwort → 400, mit korrektem Passwort + Code → Secret null |
| **AUTH-2FA-10** | `TwoFaLoginIT` (Slice B) | Voller Login-Flow mit 2FA — Username/Pw → /login/2fa → Dashboard |
| **AUTH-2FA-11** | `TwoFaLoginIT` (Slice B) | 5 Fehlversuche → Session-Invalidate, Audit-Eintrag |
| **AUTH-2FA-12** | `AdminBenutzerControllerTest` (Slice C) | PLATFORM_ADMIN-Reset löscht fremdes Secret + Codes, Audit-Eintrag |
| **AUTH-2FA-13** | `AdminPflichtIT` (Slice C) | PLATFORM_ADMIN ohne 2FA → nach Login Redirect `/einstellungen/2fa/zwang`, keine andere Route erreichbar |

## Sicherheits-Annahmen + Trade-offs

| Punkt | Entscheidung | Warum |
|---|---|---|
| Algorithmus | TOTP HMAC-SHA1, 30s-Window | RFC 6238-Standard, alle Authenticator-Apps |
| Backup-Code-Anzahl | 10 | Industry-Standard (GitHub: 16, Microsoft: 10, Google: 10) |
| Backup-Code-Format | 8-stellig alphanumerisch, ohne mehrdeutige Zeichen (kein 0/O, 1/l) | Verkürzt Tipp-Fehler |
| Replay-Window | ±1 Step (30s zurück/vor) | Industry-Standard, Toleranz für Clock-Drift |
| Failed-Login-Lockout | 5 Versuche pro 15 min pro Pre-Auth-Token | Schützt vor Brute-Force, kompatibel mit `LoginSperreFilter`-Pattern |
| Secret-Verschlüsselung | nein, Klartext | DB-Vollkompromittierung wäre Game-Over für Passwörter sowieso; Vault später wenn relevant |
| OIDC-Inter-Op | OIDC-Login skippt TOTP | IdP bringt 2. Faktor (Phase 13.3) |
| Admin-Recovery | `/admin/benutzer/{id}/2fa-reset` POST | Falls User Authenticator + alle Backup-Codes verloren — sonst Account dauerhaft locked |

## Status

- [x] Slice A — Schema + TotpService + Setup-Flow + DIP-Audit-Events (AUTH-2FA-01..09, AUTH-2FA-S-01..06)
- [x] Slice B — Login-Integration: SuccessHandler-Stash + `/login/2fa` + Lockout (AUTH-2FA-S-07..11, AUTH-2FA-10..11)
- [x] Slice C *(Teil 1)* — **Admin-Reset-Pfad** unter `/admin/benutzer/{id}/2fa-reset` + 2FA-Status-Badge in der Admin-Benutzerliste (AUTH-2FA-S-12..14, AUSER-07..08)
- [ ] Slice C *(Teil 2)* — **PLATFORM_ADMIN-Pflicht** (kein Skip nach Login ohne 2FA) — bewusst nach hinten verschoben, kommt später

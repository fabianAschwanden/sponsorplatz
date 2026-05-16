-- V39: Backlog-Item — Zwei-Faktor-Authentifizierung (2FA)
--
-- Idempotent via NOT EXISTS auf den Titel — analog zu V27 für OIDC. Falls
-- jemand das Item manuell vorab anlegt, bleibt es erhalten statt ein
-- Duplikat zu erzeugen.
INSERT INTO backlog_item (titel, beschreibung, status, prioritaet, erstellt_von)
SELECT
    'Zwei-Faktor-Authentifizierung (2FA)',
    'Zweiten Faktor beim Login einführen, damit Plattform-Admins und '
        || 'Vereins-Owner nicht nur per Passwort geschützt sind. '
        || 'Bevorzugt TOTP (RFC 6238) via Authenticator-App (Google '
        || 'Authenticator, 1Password, Authy) — keine SMS-Codes wegen '
        || 'SIM-Swap-Risiko. Umsetzung: '
        || '(1) DB-Spalten auf app_user (totp_secret, totp_enabled, '
        || 'backup_codes_hashed), '
        || '(2) Setup-Flow unter /einstellungen/2fa mit QR-Code-Anzeige '
        || '(Library: dev.samstevens.totp), '
        || '(3) Login-Flow mit zweitem Schritt (POST /login/2fa) und '
        || 'eigener SecurityFilter-Stage, '
        || '(4) Backup-Codes (10 Stück, einmalig verwendbar, BCrypt-gehasht), '
        || '(5) Recovery-Reset durch Plattform-Admin im /admin/benutzer-UI, '
        || '(6) Pflicht für PLATFORM_ADMIN-Rolle (Policy), optional für '
        || 'Vereins-/Sponsor-Owner mit nudge nach erstem Login, '
        || '(7) Audit-Log-Einträge für Enable/Disable/Recovery, '
        || '(8) Tests AUTH-2FA-01..10 (Setup, Verify, Reuse-Protection, '
        || 'Replay-Window, Lockout nach 5 Fehlversuchen, Recovery-Codes, '
        || 'Admin-Reset-Path). '
        || 'Abhängigkeit zu OIDC (V27): bei OIDC-Login kommt 2FA vom IdP — '
        || 'kein eigener Schritt nötig; Policy nur auf Form-Login-User '
        || 'anwenden.',
    'OFFEN',
    'HOCH',
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM backlog_item WHERE titel = 'Zwei-Faktor-Authentifizierung (2FA)'
);

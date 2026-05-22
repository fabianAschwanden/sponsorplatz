-- V43: 2-Faktor-Authentifizierung (TOTP) — Phase 13.2.
--
-- Backlog-V39 Punkt 1. Drei nullable Spalten — Defaults bewusst NULL:
--
--   totp_secret              Base32-encoded TOTP-Secret (RFC 6238).
--                            NULL = 2FA nicht aktiv (Default für alle
--                            bestehenden User; Aktivierung optional).
--   totp_aktiviert_am        Timestamp der erfolgreichen Aktivierung.
--                            Wird gleichzeitig mit totp_secret gesetzt.
--   totp_backup_codes_hashed JSON-Array mit BCrypt-Hashes der noch
--                            nicht verbrauchten Backup-Codes (≤ 10).
--                            Verbrauchter Code wird aus dem Array
--                            entfernt — kein "verbraucht"-Flag.
--
-- Drei separate ALTER-Statements (statt eines mit kommaseparierten
-- ADD COLUMNs), damit H2 + Postgres beide schlucken.
--
-- Spec: specs/AUTH_2FA_TOTP.md
-- Tests: AUTH-2FA-01..09 in specs/TESTSTRATEGIE.md

ALTER TABLE app_user ADD COLUMN totp_secret VARCHAR(64);
ALTER TABLE app_user ADD COLUMN totp_aktiviert_am TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_user ADD COLUMN totp_backup_codes_hashed TEXT;

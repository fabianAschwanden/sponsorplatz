-- V27: Backlog-Item — OIDC-Identity-Provider-Anbindung
--
-- Idempotent via NOT EXISTS auf den Titel — die Migration läuft pro Env genau
-- einmal (Flyway tracking), aber wenn jemand das Item manuell vorab anlegt,
-- bleibt es erhalten statt ein Duplikat zu erzeugen.
INSERT INTO backlog_item (titel, beschreibung, status, prioritaet, erstellt_von)
SELECT
    'OIDC-Identity-Provider-Anbindung',
    'Externe Identity-Provider (Microsoft Entra ID, Google Workspace, '
        || 'SwissID, Switch edu-ID) via OpenID Connect anbinden, sodass '
        || 'Vereine ihre bestehenden Konten nutzen können statt einer '
        || 'separaten Sponsorplatz-Registrierung. spring-boot-starter-'
        || 'oauth2-client ist im POM bereits vorhanden — fehlende Schritte: '
        || 'Provider-Registrierung pro Tenant, Mapping vom OIDC-sub auf '
        || 'AppUser (Auto-Anlage bei Erstanmeldung), Domain-Whitelist für '
        || 'die Auto-Verifizierung, Logout-Flow, Tests SEC-OIDC-01..05.',
    'OFFEN',
    'MITTEL',
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM backlog_item WHERE titel = 'OIDC-Identity-Provider-Anbindung'
);

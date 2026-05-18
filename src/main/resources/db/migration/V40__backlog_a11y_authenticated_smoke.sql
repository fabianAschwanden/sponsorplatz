-- V40: Backlog-Item — A11y-Smoke-Suite für authentifizierte Seiten
--
-- Aktuell deckt A11ySmokeIT nur die 6 Public-Pages ab (/, /login, /kontakt,
-- /impressum, /datenschutz, /agb). Skip-Links und Landmarks wurden aber auch
-- auf /dashboard, /aufgaben, /meine-anfragen, /onboarding, /einstellungen
-- ergänzt — diese Seiten werden vom axe-core-Scan derzeit nicht geprüft.
--
-- Umsetzung (Skizze):
-- (1) Login-Helper in der Test-Suite, der mit @WithUserDetails oder einer
--     dedizierten Demo-User-Seed-Methode einen Session-Cookie holt.
-- (2) A11ySmokeIT um eine zweite Schleife erweitert, die nach Login die
--     auth-pflichtigen Hauptseiten besucht und denselben axe-run ausführt.
-- (3) Test-IDs A11Y-07..12 in TESTSTRATEGIE.md eintragen.
-- (4) Falls @WithMockUser nicht reicht (Playwright spricht eigene HTTP-
--     Session): kleinen Form-Login-Helper in E2EFixtures aufrufen.
--
-- Idempotent via NOT EXISTS auf den Titel — analog V27, V39.
INSERT INTO backlog_item (titel, beschreibung, status, prioritaet, erstellt_von)
SELECT
    'A11y-Smoke-Suite für authentifizierte Seiten',
    'A11ySmokeIT auf die auth-pflichtigen Hauptseiten ausweiten '
        || '(/dashboard, /aufgaben, /meine-anfragen, /onboarding, '
        || '/einstellungen). Aktuell prüft axe-core nur die 6 '
        || 'Public-Pages; Skip-Links und Landmarks auf den '
        || 'eingeloggten Seiten sind durch ARCH-15-Lint und '
        || 'manuelle Reviews abgedeckt, aber nicht automatisiert. '
        || 'Vor Pilot-Launch nice-to-have, blockiert ihn aber nicht.',
    'OFFEN',
    'MITTEL',
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM backlog_item
     WHERE titel = 'A11y-Smoke-Suite für authentifizierte Seiten'
);

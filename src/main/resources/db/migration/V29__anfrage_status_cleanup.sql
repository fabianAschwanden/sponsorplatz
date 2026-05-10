-- V29: AnfrageStatus aufräumen — IN_PRUEFUNG und ZURUECKGEZOGEN sind nie aktiv
-- benutzt worden (kein Service-Pfad setzt sie, keine UI bietet sie an). Wir
-- enthärten sie auf NEU, damit die CHECK-Constraint wieder zur Java-Enum passt.
--
-- Der Workflow ist jetzt strikt: NEU → ANGENOMMEN | ABGELEHNT (terminal).

-- 1) Vorhandene Daten konsolidieren — für dev/prod gleichermassen sicher.
UPDATE sponsoring_anfrage SET status = 'NEU' WHERE status = 'IN_PRUEFUNG';
UPDATE sponsoring_anfrage SET status = 'ABGELEHNT' WHERE status = 'ZURUECKGEZOGEN';

-- 2) CHECK-Constraint neu setzen.
ALTER TABLE sponsoring_anfrage DROP CONSTRAINT chk_anfrage_status;
ALTER TABLE sponsoring_anfrage
    ADD CONSTRAINT chk_anfrage_status CHECK (status IN ('NEU', 'ANGENOMMEN', 'ABGELEHNT'));

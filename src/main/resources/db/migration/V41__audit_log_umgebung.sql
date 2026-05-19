-- V41: Umgebungs-Marker im Audit-Log.
--
-- Hintergrund: durch Multi-Cloud-DB-Sync (OCI ↔ Azure, Phase 15.3) lassen
-- sich nach einem Restore Audit-Einträge nicht mehr unterscheiden — ist
-- ein Eintrag auf der eigenen Cloud entstanden oder aus dem Source-Backup
-- mitgebracht? Diese Spalte trägt die Quell-Umgebung des Eintrags.
--
-- Werte (Konvention, kein DB-Constraint damit künftige Umgebungen
-- erweitert werden können):
--   lokal              — Dev-Maschine
--   oci-staging-free   — OCI Always-Free-VM (sponsorplatz.for-better.biz)
--   azure-staging      — Azure-Stack (sponsorplatz.for-the.biz)
--   <weitere>          — frei wählbar via SPONSORPLATZ_UMGEBUNG-ENV

ALTER TABLE audit_log
    ADD COLUMN umgebung VARCHAR(50) NOT NULL DEFAULT 'unknown';

-- Index für Filter nach Umgebung (z.B. "alles was auf OCI passierte").
CREATE INDEX idx_audit_umgebung ON audit_log(umgebung, zeitpunkt DESC);

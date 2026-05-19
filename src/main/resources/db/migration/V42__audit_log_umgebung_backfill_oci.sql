-- V42: Backfill umgebung='unknown' → 'oci-staging-free' für historische
-- Audit-Einträge.
--
-- Vor V41 hatte audit_log keinen Umgebungs-Marker. Sämtliche bestehenden
-- Einträge stammen aus dem OCI-Always-Free-Stack (sponsorplatz.for-better.biz),
-- dem einzigen Deployment vor dem Multi-Cloud-Setup (Phase 15.3). Nach V41
-- wurden sie via DEFAULT-Clause auf 'unknown' gesetzt — diese Migration
-- ordnet sie ihrer tatsächlichen Quell-Umgebung zu.
--
-- Sicherheit: WHERE umgebung='unknown' lässt neue Einträge (nach V41
-- entstanden) unangetastet, weil die bereits 'oci-staging-free' bzw.
-- 'azure-staging' tragen. Idempotent — beim Re-Run kein Effekt.

UPDATE audit_log
   SET umgebung = 'oci-staging-free'
 WHERE umgebung = 'unknown';

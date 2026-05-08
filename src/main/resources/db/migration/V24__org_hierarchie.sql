-- V24: Hierarchische Organisationsstruktur (Eltern-Kind-Beziehung)
--
-- ON DELETE RESTRICT als Defense-in-Depth: Service-Layer blockt schon
-- (OrganisationService.loesche prüft existsByUebergeordneteOrgId), aber
-- bei Service-Bypass (Direkt-SQL, künftiger zweiter Code-Pfad) verhindert
-- die DB still verwaiste Kinder. SET NULL wäre inkonsistent zur Spec.
ALTER TABLE organisation ADD COLUMN uebergeordnete_org_id UUID REFERENCES organisation(id) ON DELETE RESTRICT;

CREATE INDEX idx_organisation_uebergeordnete ON organisation(uebergeordnete_org_id);

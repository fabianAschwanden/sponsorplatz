-- V46: CHK_PROVIDER-Constraint auf federierte_identitaet droppen.
--
-- Gleiches Pattern wie V44 (chk_audit_aktion) und V45 (chk_benachrichtigung_typ):
-- V28 hat eine CHECK-Constraint mit der Allowlist 'ENTRA_ID' angelegt. Seit
-- Phase 13.3 Slice C kommen GOOGLE, SWISSID und EDU_ID dazu — die Constraint
-- wäre brittle (jeder neue Provider braucht eine Migration).
--
-- Lösung: Constraint entfernen. 'IdentityProvider.java' ist die einzige
-- Source of Truth — die App schreibt nichts ausserhalb der Enum-Konstanten.

ALTER TABLE federierte_identitaet DROP CONSTRAINT IF EXISTS chk_provider;

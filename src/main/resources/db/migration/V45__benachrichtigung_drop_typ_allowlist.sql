-- V45: CHK_BENACHRICHTIGUNG_TYP-Constraint droppen.
--
-- Gleiches Pattern wie V44 (chk_audit_aktion): V19 hat eine CHECK-Constraint
-- angelegt mit einer hartcodierten Allowlist gültiger 'typ'-Werte. Seitdem
-- ist 'NEUE_ORG_REGISTRIERT' im Enum 'BenachrichtigungTyp' dazugekommen
-- (verwendet von AdminBenachrichtigungService beim Anlegen einer neuen Org),
-- aber die DB-Constraint wurde nie nachgezogen.
--
-- Symptom: Beim Anlegen einer Org wirft der Benachrichtigungs-Listener
-- 'CHK_BENACHRICHTIGUNG_TYP: ConstraintViolationException'. Die Org wird
-- trotzdem committed, der Fehler erscheint nur als ERROR im Log.
--
-- Lösung: Constraint entfernen. 'BenachrichtigungTyp.java' ist die einzige
-- Source of Truth — doppelte Pflege im SQL ist brittle und bringt keinen
-- Schutz (die App schreibt nichts ausserhalb der Enum-Konstanten).

ALTER TABLE benachrichtigung DROP CONSTRAINT IF EXISTS chk_benachrichtigung_typ;

-- Fix: /meine-anfragen → /anfragen
-- V36 hat das System-Seed-Definition für „Anfrage bearbeiten" mit dem Link
-- /meine-anfragen angelegt — die Route existiert nicht (MeineAnfragenController
-- mappt @GetMapping("/anfragen")). Tasks aus dieser Definition landeten auf
-- einer 404-Seite.
--
-- Migration korrigiert (a) die Definition für künftige Tasks und (b) bereits
-- erzeugte Aufgaben, deren link-Spalte beim Erzeugen aus der Definition kopiert
-- wurde.

UPDATE aufgaben_definition
   SET link_template = '/anfragen'
 WHERE id = 'aaaaaaa1-0000-0000-0000-000000000002'
   AND link_template = '/meine-anfragen';

UPDATE aufgabe
   SET link = '/anfragen'
 WHERE link = '/meine-anfragen';

package ch.sponsorplatz.aufgabe;

/**
 * Domain-Aggregate, deren Status-Wechsel die {@link AufgabenEngine} überwacht.
 * Neue Werte erfordern Anpassung des CHECK-Constraints {@code chk_aufgaben_def_entity_typ}
 * in der DB-Migration sowie eines Trigger-Aufrufs im jeweiligen Service.
 */
public enum TriggerEntityTyp {
    ORG,
    ANFRAGE,
    VERTRAG,
    RECHNUNG,
    PROJEKT
}

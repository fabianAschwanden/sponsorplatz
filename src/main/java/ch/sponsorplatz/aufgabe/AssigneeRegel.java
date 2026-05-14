package ch.sponsorplatz.aufgabe;

/**
 * Regel, die beim Erzeugen einer {@link Aufgabe} aus einer {@link AufgabenDefinition}
 * die Sichtbarkeit auflöst — entweder auf eine konkrete Org (alle Mitglieder sehen
 * die Aufgabe) oder auf PLATFORM_ADMIN.
 *
 * <p>Die Regel ist mit {@link TriggerEntityTyp} gekoppelt: z.B. {@link #VERTRAG_VEREIN_ORG}
 * lässt sich nur sinnvoll auf {@link TriggerEntityTyp#VERTRAG} anwenden. Der
 * {@link AssigneeResolver} prüft das defensiv.
 */
public enum AssigneeRegel {
    /** Aufgabe ist nur für Benutzer mit Plattform-Rolle PLATFORM_ADMIN sichtbar. */
    PLATFORM_ADMIN,

    /** Aufgabe geht an alle Mitglieder der Entity-Org (passt nur für ORG-Entities). */
    ORG_MITGLIEDER,

    /** Empfänger-Org der Anfrage (klassisch der Verein, der angefragt wird). */
    ANFRAGE_EMPFAENGER_ORG,

    /** Anfragende Org der Anfrage (klassisch der Sponsor bzw. bei Kontakt-Anfragen der Verein). */
    ANFRAGE_ANFRAGENDER_ORG,

    /** Verein-Org des Vertrags ({@code vertrag.org}). */
    VERTRAG_VEREIN_ORG,

    /** Sponsor-Org des Vertrags ({@code vertrag.sponsorOrg}). */
    VERTRAG_SPONSOR_ORG,

    /** Rechnungsstellende Org (typischerweise der Verein, {@code rechnung.org}). */
    RECHNUNG_VEREIN_ORG
}

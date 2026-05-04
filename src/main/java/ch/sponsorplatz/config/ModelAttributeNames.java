package ch.sponsorplatz.config;

/**
 * Zentrale Konstanten für Model-Attribut-Keys.
 * Verhindert Tippfehler-Bugs zwischen Controller und Template.
 */
public final class ModelAttributeNames {

    public static final String AKTIVE_SEITE = "aktiveSeite";
    public static final String FEHLERMELDUNG = "fehlermeldung";
    public static final String ERFOLGS_MELDUNG = "erfolgsMeldung";

    private ModelAttributeNames() {
    }
}

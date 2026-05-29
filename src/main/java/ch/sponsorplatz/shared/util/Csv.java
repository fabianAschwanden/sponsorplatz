package ch.sponsorplatz.shared.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimaler CSV-Helfer mit Semikolon-Delimiter (Excel-CH-Standard). Felder, die
 * {@code ;} oder {@code "} enthalten, werden in Anführungszeichen gesetzt
 * (internes {@code "} verdoppelt). Zeilenumbrüche in Feldern werden beim
 * Schreiben durch Leerzeichen ersetzt, damit Import zeilenweise funktioniert.
 */
public final class Csv {

    private static final char DELIMITER = ';';

    private Csv() {
    }

    /** Baut eine CSV-Zeile (ohne Zeilenende) aus den Feldern. */
    public static String zeile(List<String> felder) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < felder.size(); i++) {
            if (i > 0) sb.append(DELIMITER);
            sb.append(escape(felder.get(i)));
        }
        return sb.toString();
    }

    private static String escape(String feld) {
        if (feld == null || feld.isEmpty()) return "";
        String bereinigt = feld.replace('\r', ' ').replace('\n', ' ');
        if (bereinigt.indexOf(DELIMITER) >= 0 || bereinigt.indexOf('"') >= 0) {
            return '"' + bereinigt.replace("\"", "\"\"") + '"';
        }
        return bereinigt;
    }

    /**
     * Zerlegt eine CSV-Zeile in Felder. Beachtet {@code "}-gequotete Felder
     * (mit eingebettetem {@code ;} und verdoppeltem {@code ""}).
     */
    public static List<String> parse(String zeile) {
        List<String> felder = new ArrayList<>();
        StringBuilder feld = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < zeile.length(); i++) {
            char c = zeile.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < zeile.length() && zeile.charAt(i + 1) == '"') {
                        feld.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    feld.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == DELIMITER) {
                felder.add(feld.toString());
                feld.setLength(0);
            } else {
                feld.append(c);
            }
        }
        felder.add(feld.toString());
        return felder;
    }
}

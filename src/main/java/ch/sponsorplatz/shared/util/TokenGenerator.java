package ch.sponsorplatz.shared.util;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Util zur Erzeugung kryptografisch starker Tokens für Verifikation, Einladungen
 * und ähnliche Flows. 32 Bytes random → 64 Hex-Zeichen.
 */
public final class TokenGenerator {

    private static final int LAENGE_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {
        // util — kein Instanziieren
    }

    public static String generiere() {
        byte[] bytes = new byte[LAENGE_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

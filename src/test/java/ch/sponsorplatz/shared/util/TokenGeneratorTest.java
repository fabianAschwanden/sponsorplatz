package ch.sponsorplatz.shared.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGeneratorTest {

    /** TG-01: generiere() erzeugt 64-stelligen Hex-String (32 Bytes random). */
    @Test
    void generiereLiefert64HexZeichen() {
        String token = TokenGenerator.generiere();

        assertThat(token).hasSize(64);
        assertThat(token).matches("[0-9a-f]{64}");
    }

    /** TG-02: zwei Aufrufe liefern unterschiedliche Tokens (Eindeutigkeit). */
    @Test
    void zweiAufrufeLiefernUnterschiedlicheTokens() {
        String t1 = TokenGenerator.generiere();
        String t2 = TokenGenerator.generiere();

        assertThat(t1).isNotEqualTo(t2);
    }
}

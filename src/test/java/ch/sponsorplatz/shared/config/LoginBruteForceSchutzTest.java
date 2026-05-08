package ch.sponsorplatz.shared.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginBruteForceSchutzTest {

    private LoginBruteForceSchutz schutz;

    @BeforeEach
    void setUp() {
        schutz = new LoginBruteForceSchutz();
    }

    @Test
    @DisplayName("BF-01: Nicht gesperrt bei weniger als MAX_VERSUCHE Fehlversuchen")
    void nichtGesperrtUnterLimit() {
        for (int i = 0; i < LoginBruteForceSchutz.MAX_VERSUCHE - 1; i++) {
            schutz.fehlversuchRegistrieren("test@sp.ch");
        }
        assertThat(schutz.istGesperrt("test@sp.ch")).isEqualTo(0);
    }

    @Test
    @DisplayName("BF-02: Gesperrt nach MAX_VERSUCHE Fehlversuchen")
    void gesperrtNachLimit() {
        for (int i = 0; i < LoginBruteForceSchutz.MAX_VERSUCHE; i++) {
            schutz.fehlversuchRegistrieren("test@sp.ch");
        }
        assertThat(schutz.istGesperrt("test@sp.ch")).isGreaterThan(0);
    }

    @Test
    @DisplayName("BF-03: Erfolgreicher Login setzt Zähler zurück")
    void erfolgreichResettet() {
        for (int i = 0; i < LoginBruteForceSchutz.MAX_VERSUCHE - 1; i++) {
            schutz.fehlversuchRegistrieren("test@sp.ch");
        }
        schutz.erfolgreichenLoginRegistrieren("test@sp.ch");

        assertThat(schutz.istGesperrt("test@sp.ch")).isEqualTo(0);
        assertThat(schutz.verbleibendeVersuche("test@sp.ch")).isEqualTo(LoginBruteForceSchutz.MAX_VERSUCHE);
    }

    @Test
    @DisplayName("BF-04: Verschiedene E-Mails haben getrennte Zähler")
    void getrenntProEmail() {
        for (int i = 0; i < LoginBruteForceSchutz.MAX_VERSUCHE; i++) {
            schutz.fehlversuchRegistrieren("a@sp.ch");
        }
        assertThat(schutz.istGesperrt("a@sp.ch")).isGreaterThan(0);
        assertThat(schutz.istGesperrt("b@sp.ch")).isEqualTo(0);
    }

    @Test
    @DisplayName("BF-05: verbleibendeVersuche reduziert sich korrekt")
    void verbleibendeVersuche() {
        assertThat(schutz.verbleibendeVersuche("x@sp.ch")).isEqualTo(LoginBruteForceSchutz.MAX_VERSUCHE);

        schutz.fehlversuchRegistrieren("x@sp.ch");
        schutz.fehlversuchRegistrieren("x@sp.ch");

        assertThat(schutz.verbleibendeVersuche("x@sp.ch")).isEqualTo(LoginBruteForceSchutz.MAX_VERSUCHE - 2);
    }

    @Test
    @DisplayName("BF-06: Case-insensitive E-Mail-Matching")
    void caseInsensitive() {
        for (int i = 0; i < LoginBruteForceSchutz.MAX_VERSUCHE; i++) {
            schutz.fehlversuchRegistrieren("Test@SP.ch");
        }
        assertThat(schutz.istGesperrt("test@sp.ch")).isGreaterThan(0);
    }

    @Test
    @DisplayName("BF-07: bereinigen entfernt Einträge mit abgelaufener Sperre")
    void bereinigenEntferntAbgelaufeneSperre() {
        for (int i = 0; i < LoginBruteForceSchutz.MAX_VERSUCHE; i++) {
            schutz.fehlversuchRegistrieren("alt@sp.ch");
        }
        // jetzt+1h: Sperre (15min) und idle-Grenze (15min) lange überschritten
        Instant zukunft = Instant.now().plus(Duration.ofHours(1));
        assertThat(schutz.bereinigen(zukunft)).isEqualTo(1);
        assertThat(schutz.istGesperrt("alt@sp.ch")).isEqualTo(0);
        assertThat(schutz.verbleibendeVersuche("alt@sp.ch")).isEqualTo(LoginBruteForceSchutz.MAX_VERSUCHE);
    }

    @Test
    @DisplayName("BF-08: bereinigen behält aktive Sperren und entfernt idle Einträge ohne Sperre")
    void bereinigenBehaeltAktiveEntferntIdle() {
        // Account 1: gesperrt, sollte bleiben (Sperre noch aktiv in 5min)
        for (int i = 0; i < LoginBruteForceSchutz.MAX_VERSUCHE; i++) {
            schutz.fehlversuchRegistrieren("aktiv@sp.ch");
        }
        // Account 2: nur 1 Fehlversuch, idle → soll weg
        schutz.fehlversuchRegistrieren("idle@sp.ch");

        Instant in5min = Instant.now().plus(Duration.ofMinutes(5));
        // bei +5min: aktiv@ noch gesperrt (15min Sperre läuft), idle@ noch nicht idle (Grenze=15min)
        assertThat(schutz.bereinigen(in5min)).isEqualTo(0);

        Instant in30min = Instant.now().plus(Duration.ofMinutes(30));
        // bei +30min: aktiv@ Sperre abgelaufen → weg, idle@ über idle-Grenze → weg
        assertThat(schutz.bereinigen(in30min)).isEqualTo(2);
        assertThat(schutz.istGesperrt("aktiv@sp.ch")).isEqualTo(0);
    }
}


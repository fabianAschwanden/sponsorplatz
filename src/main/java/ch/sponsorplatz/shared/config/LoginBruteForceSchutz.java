package ch.sponsorplatz.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Account-basierter Brute-Force-Schutz für Login-Versuche.
 *
 * <p>Track-t fehlgeschlagene Logins pro E-Mail (nicht nur IP).
 * Nach {@link #MAX_VERSUCHE} Fehlversuchen innerhalb von {@link #SPERRZEIT}
 * wird das Konto temporär gesperrt.</p>
 *
 * <p>Erfolgreicher Login setzt den Zähler zurück.</p>
 *
 * <p>In-Memory → bei Restart werden alle Sperren aufgehoben,
 * was akzeptabel ist (kein Dauer-Lockout).</p>
 *
 * <p>Stale Einträge (abgelaufene Sperren oder seit {@link #SPERRZEIT} keine
 * neuen Fehlversuche) werden via {@link #geplanteBereinigung()} alle 30 min
 * weggeräumt — verhindert Memory-Leak bei Bot-Angriffen mit gefälschten
 * E-Mail-Adressen.</p>
 */
public class LoginBruteForceSchutz {

    private static final Logger log = LoggerFactory.getLogger(LoginBruteForceSchutz.class);

    /** Maximale Fehlversuche bevor gesperrt wird. */
    static final int MAX_VERSUCHE = 5;

    /** Dauer der temporären Sperre. */
    static final Duration SPERRZEIT = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, LoginVersuchZaehler> zaehler = new ConcurrentHashMap<>();

    /**
     * Registriert einen fehlgeschlagenen Login-Versuch.
     */
    public void fehlversuchRegistrieren(String email) {
        String key = email.toLowerCase().trim();
        LoginVersuchZaehler z = zaehler.computeIfAbsent(key, k -> new LoginVersuchZaehler());
        int versuche = z.inkrement();

        if (versuche >= MAX_VERSUCHE) {
            z.sperreBis(Instant.now().plus(SPERRZEIT));
            log.warn("Brute-Force: Account {} nach {} Fehlversuchen für {} Minuten gesperrt",
                    key, versuche, SPERRZEIT.toMinutes());
        }
    }

    /**
     * Prüft ob ein Account aktuell gesperrt ist.
     *
     * @return verbleibende Sekunden der Sperre, 0 wenn nicht gesperrt
     */
    public long istGesperrt(String email) {
        String key = email.toLowerCase().trim();
        LoginVersuchZaehler z = zaehler.get(key);
        if (z == null) return 0;

        Instant sperreEnde = z.getSperreBis();
        if (sperreEnde == null) return 0;

        if (Instant.now().isBefore(sperreEnde)) {
            return Duration.between(Instant.now(), sperreEnde).getSeconds();
        }

        // Sperre abgelaufen → Zähler zurücksetzen
        zaehler.remove(key);
        return 0;
    }

    /**
     * Setzt den Zähler nach erfolgreichem Login zurück.
     */
    public void erfolgreichenLoginRegistrieren(String email) {
        zaehler.remove(email.toLowerCase().trim());
    }

    /**
     * Gibt die Anzahl verbleibender Versuche zurück.
     */
    public int verbleibendeVersuche(String email) {
        String key = email.toLowerCase().trim();
        LoginVersuchZaehler z = zaehler.get(key);
        if (z == null) return MAX_VERSUCHE;
        return Math.max(0, MAX_VERSUCHE - z.getAnzahl());
    }

    /**
     * Räumt stale Einträge weg — entweder mit abgelaufener Sperre oder
     * seit {@link #SPERRZEIT} keine neuen Versuche mehr. Aktive Sperren
     * bleiben unverändert.
     *
     * @return Anzahl entfernter Einträge (für Tests/Monitoring)
     */
    public int bereinigen() {
        return bereinigen(Instant.now());
    }

    /** Test-Override: erlaubt es, die "jetzt"-Zeit zu injizieren ohne {@code Thread.sleep}. */
    int bereinigen(Instant jetzt) {
        Instant idleGrenze = jetzt.minus(SPERRZEIT);
        int vorher = zaehler.size();
        zaehler.entrySet().removeIf(e -> {
            LoginVersuchZaehler z = e.getValue();
            // Aktive Sperre? Behalten.
            if (z.getSperreBis() != null && jetzt.isBefore(z.getSperreBis())) {
                return false;
            }
            // Stale: keine aktive Sperre und seit Sperrzeit kein Update mehr.
            return z.getLastUpdate().isBefore(idleGrenze);
        });
        return vorher - zaehler.size();
    }

    /** Cleanup-Tick — alle 30 min, abgelaufene/idle Einträge fallen raus. */
    @Scheduled(fixedDelay = 30, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void geplanteBereinigung() {
        int entfernt = bereinigen();
        if (entfernt > 0) {
            log.debug("Brute-Force-Cleanup: {} stale Einträge entfernt (jetzt {} aktiv)",
                    entfernt, zaehler.size());
        }
    }

    // --- Innere Hilfsklasse ---

    static class LoginVersuchZaehler {
        private final AtomicInteger anzahl = new AtomicInteger(0);
        private volatile Instant sperreBis;
        private volatile Instant lastUpdate = Instant.now();

        int inkrement() {
            this.lastUpdate = Instant.now();
            return anzahl.incrementAndGet();
        }

        int getAnzahl() {
            return anzahl.get();
        }

        void sperreBis(Instant bis) {
            this.sperreBis = bis;
            this.lastUpdate = Instant.now();
        }

        Instant getSperreBis() {
            return sperreBis;
        }

        Instant getLastUpdate() {
            return lastUpdate;
        }
    }
}


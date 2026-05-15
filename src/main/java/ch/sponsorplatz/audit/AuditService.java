package ch.sponsorplatz.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service für Audit-Logging. Schreibt alle relevanten Plattform-Aktionen
 * in die audit_log-Tabelle. Läuft asynchron, um die Business-Logik nicht zu blockieren.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Protokolliert eine Aktion asynchron.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void protokolliere(String aktion, String bereich, UUID zielId, String zielTyp, String details) {
        protokolliereIntern(aktion, bereich, zielId, zielTyp, details);
    }

    /**
     * Protokolliert mit explizitem Benutzer (für Fälle wo SecurityContext nicht verfügbar ist).
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void protokolliereMitBenutzer(String aktion, String bereich, UUID benutzerId,
                                          String benutzerEmail, UUID zielId, String zielTyp, String details) {
        AuditLog eintrag = new AuditLog();
        eintrag.setAktion(aktion);
        eintrag.setBereich(bereich);
        eintrag.setBenutzerId(benutzerId);
        eintrag.setBenutzerEmail(benutzerEmail);
        eintrag.setZielId(zielId);
        eintrag.setZielTyp(zielTyp);
        eintrag.setDetails(details);
        repository.save(eintrag);
        log.debug("Audit: {} {} {} ({})", aktion, bereich, zielId, benutzerEmail);
    }

    /**
     * Gibt die letzten 100 Audit-Einträge zurück (für Admin-UI).
     */
    @Transactional(readOnly = true)
    public List<AuditLog> letzteEintraege() {
        return repository.findTop100ByOrderByZeitpunktDesc();
    }

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<AuditLogView> letzteEintraegeViews() {
        return AuditLogView.von(letzteEintraege());
    }

    /**
     * Gibt Einträge für einen bestimmten Bereich zurück.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findeNachBereich(String bereich) {
        return repository.findByBereichOrderByZeitpunktDesc(bereich);
    }

    /**
     * Gibt Einträge in einem Zeitraum zurück.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findeNachZeitraum(Instant von, Instant bis) {
        return repository.findByZeitpunktBetweenOrderByZeitpunktDesc(von, bis);
    }

    private void protokolliereIntern(String aktion, String bereich, UUID zielId, String zielTyp, String details) {
        AuditLog eintrag = new AuditLog();
        eintrag.setAktion(aktion);
        eintrag.setBereich(bereich);
        eintrag.setZielId(zielId);
        eintrag.setZielTyp(zielTyp);
        eintrag.setDetails(details);

        // Benutzer aus SecurityContext (wenn vorhanden)
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                eintrag.setBenutzerEmail(auth.getName());
            }
        } catch (Exception ignored) {
            // SecurityContext nicht verfügbar — kein Problem
        }

        repository.save(eintrag);
        log.debug("Audit: {} {} {} ({})", aktion, bereich, zielId, eintrag.getBenutzerEmail());
    }
}


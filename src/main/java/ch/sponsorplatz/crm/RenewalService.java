package ch.sponsorplatz.crm;

import ch.sponsorplatz.anfrage.VertragRepository;
import ch.sponsorplatz.anfrage.VertragsStatus;
import ch.sponsorplatz.organisation.AccessControl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Renewal-Pipeline der privaten Sponsor-CRM-Layer (ADR-0011): zeigt einem
 * Sponsor seine unterzeichneten Verträge, deren Laufzeit in den nächsten
 * {@link #VORLAUF_TAGE} Tagen endet (inkl. bereits überfälliger). Der
 * Zugriffs-Check ({@link AccessControl#kannSponsorDatenSehen}) läuft ZUERST —
 * kein Pfad zu Vertrags-Daten eines Sponsors ohne diese Schranke.
 *
 * <p>Quick-Win: nutzt das bereits existierende {@code vertrag.laufzeit_bis}, es
 * braucht weder Migration noch neue Entität.
 */
@Service
@Transactional(readOnly = true)
public class RenewalService {

    /** Vorlauf-Fenster: Verträge, die innerhalb dieser Frist enden, sind „fällig". */
    static final int VORLAUF_TAGE = 90;

    private final VertragRepository vertragRepository;
    private final AccessControl accessControl;

    public RenewalService(VertragRepository vertragRepository, AccessControl accessControl) {
        this.vertragRepository = vertragRepository;
        this.accessControl = accessControl;
    }

    /**
     * Auslaufende Verträge eines Sponsors, dringendste (frühestes Ende) zuerst.
     * Nur {@link VertragsStatus#UNTERZEICHNET} — Entwürfe und gekündigte
     * Verträge sind keine Renewal-Kandidaten.
     */
    public List<RenewalView> findeAuslaufende(UUID sponsorOrgId, Authentication auth) {
        if (!accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)) {
            throw new AccessDeniedException(
                    "Kein Zugriff auf die CRM-Daten dieser Sponsor-Organisation");
        }
        LocalDate heute = LocalDate.now();
        LocalDate stichtag = heute.plusDays(VORLAUF_TAGE);
        return RenewalView.von(
                vertragRepository
                        .findBySponsorOrgIdAndStatusAndLaufzeitBisNotNullAndLaufzeitBisLessThanEqualOrderByLaufzeitBisAsc(
                                sponsorOrgId, VertragsStatus.UNTERZEICHNET, stichtag),
                heute);
    }
}

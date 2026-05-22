package ch.sponsorplatz.audit;

import ch.sponsorplatz.benutzer.TwoFaEvents;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Hängt sich an die 2FA-Domain-Events aus dem {@code benutzer/}-Paket und
 * schreibt sie ins Audit-Log.
 *
 * <p>Dieses Listener-Pattern bricht den Compile-Time-Edge
 * benutzer→audit auf — der publisher in benutzer/ kennt nur Spring
 * ApplicationEventPublisher, kein audit-Symbol (ARCH-06).
 */
@Component
public class TwoFaAuditListener {

    private static final String BEREICH = "AUTH";
    private static final String ZIEL_TYP = "AppUser";

    private final AuditService auditService;

    public TwoFaAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onAktiviert(TwoFaEvents.TwoFaAktiviertEvent ev) {
        auditService.protokolliereMitBenutzer(AuditAktion.TOTP_AKTIVIERT, BEREICH,
                ev.userId(), ev.email(), ev.userId(), ZIEL_TYP, null);
    }

    @EventListener
    public void onDeaktiviert(TwoFaEvents.TwoFaDeaktiviertEvent ev) {
        auditService.protokolliereMitBenutzer(AuditAktion.TOTP_DEAKTIVIERT, BEREICH,
                ev.userId(), ev.email(), ev.userId(), ZIEL_TYP, null);
    }

    @EventListener
    public void onBackupCodesNeu(TwoFaEvents.TwoFaBackupCodesNeuEvent ev) {
        auditService.protokolliereMitBenutzer(AuditAktion.TOTP_BACKUP_CODES_NEU, BEREICH,
                ev.userId(), ev.email(), ev.userId(), ZIEL_TYP, null);
    }

    @EventListener
    public void onLoginOk(TwoFaEvents.TwoFaLoginOkEvent ev) {
        String aktion = ev.backupCodeGenutzt() ? AuditAktion.TOTP_BACKUP_CODE_GENUTZT : AuditAktion.TOTP_LOGIN_OK;
        auditService.protokolliereMitBenutzer(aktion, BEREICH,
                ev.userId(), ev.email(), ev.userId(), ZIEL_TYP, null);
    }

    @EventListener
    public void onLoginFail(TwoFaEvents.TwoFaLoginFailEvent ev) {
        auditService.protokolliereMitBenutzer(AuditAktion.TOTP_LOGIN_FAIL, BEREICH,
                ev.userId(), ev.email(), ev.userId(), ZIEL_TYP,
                "Versuch " + ev.versuchNummer() + "/5");
    }

    @EventListener
    public void onLockout(TwoFaEvents.TwoFaLockoutEvent ev) {
        auditService.protokolliereMitBenutzer(AuditAktion.LOGIN_2FA_LOCKOUT, BEREICH,
                ev.userId(), ev.email(), ev.userId(), ZIEL_TYP,
                "Session terminate nach 5 Fehlversuchen");
    }
}

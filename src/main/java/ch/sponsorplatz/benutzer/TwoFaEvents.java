package ch.sponsorplatz.benutzer;

import java.util.UUID;

/**
 * Domain-Events des 2-Faktor-Authentifizierungs-Flows (Phase 13.2).
 *
 * <p>Werden vom {@link TwoFaService} via {@code ApplicationEventPublisher}
 * publiziert. Audit-Listener in {@code audit/} hängen sich per
 * {@code @EventListener} dran — damit ist kein Compile-Time-Import
 * benutzer→audit nötig (würde sonst die Feature-Folder-DAG verletzen,
 * ARCH-06).
 */
public final class TwoFaEvents {

    public record TwoFaAktiviertEvent(UUID userId, String email) {}

    public record TwoFaDeaktiviertEvent(UUID userId, String email) {}

    public record TwoFaBackupCodesNeuEvent(UUID userId, String email) {}

    // Login-Flow (Slice B)
    public record TwoFaLoginOkEvent(UUID userId, String email, boolean backupCodeGenutzt) {}

    public record TwoFaLoginFailEvent(UUID userId, String email, int versuchNummer) {}

    public record TwoFaLockoutEvent(UUID userId, String email) {}

    private TwoFaEvents() {}
}

package ch.sponsorplatz.benutzer;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO für die Admin-Benutzerliste.
 * Niemals passwortHash oder verifikationsToken!
 */
public record AdminBenutzerView(
        UUID id,
        String email,
        String anzeigename,
        PlatformRolle platformRolle,
        boolean aktiv,
        boolean emailVerifiziert,
        Instant registriertAm,
        String profilbildUrl
) {

    public static AdminBenutzerView von(AppUser user) {
        String bildUrl = user.getProfilbildId() != null
                ? "/medien/" + user.getProfilbildId()
                : null;
        return new AdminBenutzerView(
                user.getId(),
                user.getEmail(),
                user.getAnzeigename(),
                user.getPlatformRolle(),
                user.isAktiv(),
                user.isEmailVerifiziert(),
                user.getRegistriertAm(),
                bildUrl
        );
    }

    public static List<AdminBenutzerView> von(List<AppUser> users) {
        return users.stream().map(AdminBenutzerView::von).toList();
    }
}


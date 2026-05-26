package ch.sponsorplatz.benutzer;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO für die Admin-Benutzerliste.
 * Niemals passwortHash oder verifikationsToken!
 *
 * <p>{@code hatFormLogin} + {@code federierteProvider} (UC-SSO-4): zeigt
 * in der Admin-Liste welche Auth-Quellen ein User hat. Pflegt der Service
 * via {@code von(user, providers)}-Overload — die ältere {@code von(user)}-
 * Signatur bleibt für Single-User-Lookups und Setzer-Methoden bestehen und
 * liefert leere Provider-Liste.
 */
public record AdminBenutzerView(
        UUID id,
        String email,
        String anzeigename,
        PlatformRolle platformRolle,
        boolean aktiv,
        boolean emailVerifiziert,
        Instant registriertAm,
        String profilbildUrl,
        boolean totpAktiv,
        boolean hatFormLogin,
        List<IdentityProvider> federierteProvider
) {

    public static AdminBenutzerView von(AppUser user) {
        return von(user, List.of());
    }

    public static AdminBenutzerView von(AppUser user, List<IdentityProvider> federierteProvider) {
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
                bildUrl,
                user.hatTotpAktiv(),
                hatFormLogin(user),
                federierteProvider
        );
    }

    public static List<AdminBenutzerView> von(List<AppUser> users) {
        return users.stream().map(AdminBenutzerView::von).toList();
    }

    /**
     * Form-Login ist möglich wenn der User einen echten Passwort-Hash hat —
     * weder {@code null}/leer noch der OIDC-Only-Marker (Spec §6.2 setzt den
     * Marker für JIT-provisionierte OIDC-User, damit Form-Login dort
     * blockiert ist).
     */
    private static boolean hatFormLogin(AppUser user) {
        String hash = user.getPasswortHash();
        return hash != null
                && !hash.isBlank()
                && !SponsorplatzOidcUserService.OIDC_ONLY_PASSWORT_MARKER.equals(hash);
    }
}

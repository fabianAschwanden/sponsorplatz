package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.AppUser;

import java.time.Instant;
import java.util.UUID;

/**
 * View-DTO für das Benutzerprofil. Niemals passwortHash oder Token!
 */
public record ProfilView(
        UUID id,
        String email,
        String anzeigename,
        String sprache,
        String telefon,
        String bio,
        String ort,
        String websiteUrl,
        String positionTitel,
        UUID profilbildId,
        String profilbildUrl,
        Instant registriertAm
) {

    public static ProfilView von(AppUser user) {
        String bildUrl = user.getProfilbildId() != null
                ? "/medien/" + user.getProfilbildId()
                : null;
        return new ProfilView(
                user.getId(),
                user.getEmail(),
                user.getAnzeigename(),
                user.getSprache(),
                user.getTelefon(),
                user.getBio(),
                user.getOrt(),
                user.getWebsiteUrl(),
                user.getPositionTitel(),
                user.getProfilbildId(),
                bildUrl,
                user.getRegistriertAm()
        );
    }
}


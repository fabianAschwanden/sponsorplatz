package ch.sponsorplatz.anfrage;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only View-DTO für Sponsoring-Anfragen. Flacht den Paket-Namen und
 * Org-Informationen ein, damit Templates nicht über Lazy-Relationen navigieren müssen.
 */
public record AnfrageView(
        UUID id,
        AnfrageStatus status,
        String nachricht,
        String antwort,
        String kontaktName,
        String kontaktEmail,
        Instant createdAt,
        Instant beantwortetAm,
        String paketName,
        String empfaengerOrgSlug,
        String empfaengerOrgName,
        String anfragenderOrgName
) {

    public static AnfrageView von(SponsoringAnfrage a) {
        return new AnfrageView(
                a.getId(),
                a.getStatus(),
                a.getNachricht(),
                a.getAntwort(),
                a.getKontaktName(),
                a.getKontaktEmail(),
                a.getCreatedAt(),
                a.getBeantwortetAm(),
                a.getPaket() != null ? a.getPaket().getName() : null,
                a.getEmpfaengerOrg() != null ? a.getEmpfaengerOrg().getSlug() : null,
                a.getEmpfaengerOrg() != null ? a.getEmpfaengerOrg().getName() : null,
                a.getAnfragenderOrg() != null ? a.getAnfragenderOrg().getName() : null
        );
    }

    public static List<AnfrageView> von(List<SponsoringAnfrage> anfragen) {
        return anfragen.stream().map(AnfrageView::von).toList();
    }
}

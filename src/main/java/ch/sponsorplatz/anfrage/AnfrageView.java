package ch.sponsorplatz.anfrage;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only View-DTO für Sponsoring-Anfragen. Flacht den Paket-Namen und
 * Org-Informationen ein, damit Templates nicht über Lazy-Relationen navigieren müssen.
 *
 * <p>Zwei Anfrage-Typen werden unterstützt:
 * <ul>
 *   <li><b>PAKET</b>: paket-bezogen (klassischer Sponsor → Verein-Flow). {@code paketName}
 *       gesetzt, {@code betreff} null.</li>
 *   <li><b>KONTAKT</b>: Kontakt-Anfrage (Verein → Sponsor). {@code paketName} null,
 *       {@code betreff} gesetzt.</li>
 * </ul>
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
        String betreff,
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
                a.getBetreff(),
                a.getEmpfaengerOrg() != null ? a.getEmpfaengerOrg().getSlug() : null,
                a.getEmpfaengerOrg() != null ? a.getEmpfaengerOrg().getName() : null,
                a.getAnfragenderOrg() != null ? a.getAnfragenderOrg().getName() : null
        );
    }

    public static List<AnfrageView> von(List<SponsoringAnfrage> anfragen) {
        return anfragen.stream().map(AnfrageView::von).toList();
    }

    /** True wenn die Anfrage paket-bezogen ist (klassisch). */
    public boolean istPaketAnfrage() {
        return paketName != null;
    }

    /** Anzeige-Titel: Paket-Name oder Betreff. */
    public String anzeigeTitel() {
        return paketName != null ? paketName : (betreff != null ? betreff : "—");
    }
}

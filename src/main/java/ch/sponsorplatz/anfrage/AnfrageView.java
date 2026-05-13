package ch.sponsorplatz.anfrage;


import ch.sponsorplatz.organisation.OrgTyp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only View-DTO für Sponsoring-Anfragen. Flacht Paket-Namen und
 * Org-Informationen ein, damit Templates nicht über Lazy-Relationen navigieren müssen.
 *
 * <p>Zwei Anfrage-Typen werden unterstützt:
 * <ul>
 *   <li><b>PAKET</b>: paket-bezogen (klassischer Sponsor → Verein-Flow). {@code paketName}
 *       gesetzt, {@code betreff} null. Anfragender ist der Sponsor.</li>
 *   <li><b>KONTAKT</b>: Kontakt-Anfrage (Verein → Sponsor proaktiv). {@code paketName} null,
 *       {@code betreff} gesetzt. Anfragender ist der Verein.</li>
 * </ul>
 *
 * <p>Beide Anfrage-Typen können nach Annahme einen Vertrag erzeugen.
 * Der Vertrag wird immer vom <em>Verein</em> erstellt (auch bei Kontakt-Anfrage,
 * obwohl der Verein dort der Anfragende ist). {@link #vereinSlug()} liefert
 * den korrekten Slug abhängig vom Anfrage-Typ.
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
        OrgTyp empfaengerOrgTyp,
        String anfragenderOrgSlug,
        String anfragenderOrgName,
        OrgTyp anfragenderOrgTyp
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
                a.getEmpfaengerOrg() != null ? a.getEmpfaengerOrg().getTyp() : null,
                a.getAnfragenderOrg() != null ? a.getAnfragenderOrg().getSlug() : null,
                a.getAnfragenderOrg() != null ? a.getAnfragenderOrg().getName() : null,
                a.getAnfragenderOrg() != null ? a.getAnfragenderOrg().getTyp() : null
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

    /**
     * Slug der Verein-Seite — bei Paket-Anfrage der Empfänger, bei
     * Kontakt-Anfrage der Anfragende. Wird für die Vertrag-Erstellungs-URL
     * gebraucht, weil der Vertrag immer beim Verein angelegt wird.
     * Fallback auf Empfänger-Slug, wenn der Typ unbekannt ist
     * (Defensive für ältere Datensätze ohne Typ).
     */
    public String vereinSlug() {
        if (anfragenderOrgTyp == OrgTyp.VEREIN) return anfragenderOrgSlug;
        if (empfaengerOrgTyp == OrgTyp.VEREIN) return empfaengerOrgSlug;
        return empfaengerOrgSlug;
    }
}

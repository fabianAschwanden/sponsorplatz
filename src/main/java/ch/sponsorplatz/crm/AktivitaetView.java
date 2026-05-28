package ch.sponsorplatz.crm;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO für {@link Aktivitaet} (View-DTO-Pflicht). Flacht den optionalen
 * Kontakt auf Name ein; Mandanten-Schlüssel bleibt aus dem Output.
 */
public record AktivitaetView(
        UUID id,
        UUID accountId,
        AktivitaetTyp typ,
        LocalDate datum,
        String betreff,
        String notiz,
        UUID kontaktPersonId,
        String kontaktName
) {

    public static AktivitaetView von(Aktivitaet a) {
        KontaktPerson k = a.getKontaktPerson();
        return new AktivitaetView(
                a.getId(),
                a.getAccount().getId(),
                a.getTyp(),
                a.getDatum(),
                a.getBetreff(),
                a.getNotiz(),
                k != null ? k.getId() : null,
                k != null ? k.getVorname() + " " + k.getNachname() : null);
    }

    public static List<AktivitaetView> von(List<Aktivitaet> aktivitaeten) {
        return aktivitaeten.stream().map(AktivitaetView::von).toList();
    }
}

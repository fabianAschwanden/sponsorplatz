package ch.sponsorplatz.crm;

import java.util.List;
import java.util.UUID;

/**
 * View-DTO für {@link KontaktPerson} (View-DTO-Pflicht). Mandanten-Schlüssel
 * ist Zugriffs-, kein Anzeige-Belang und bleibt aus dem Output.
 */
public record KontaktPersonView(
        UUID id,
        UUID accountId,
        String vorname,
        String nachname,
        String funktion,
        KontaktRolle kontaktRolle,
        String email,
        String telefon,
        String mobile,
        String notiz
) {

    public static KontaktPersonView von(KontaktPerson k) {
        return new KontaktPersonView(
                k.getId(),
                k.getAccount().getId(),
                k.getVorname(),
                k.getNachname(),
                k.getFunktion(),
                k.getKontaktRolle(),
                k.getEmail(),
                k.getTelefon(),
                k.getMobile(),
                k.getNotiz());
    }

    public static List<KontaktPersonView> von(List<KontaktPerson> kontakte) {
        return kontakte.stream().map(KontaktPersonView::von).toList();
    }

    /** Anzeigename „Vorname Nachname". */
    public String name() {
        return vorname + " " + nachname;
    }
}

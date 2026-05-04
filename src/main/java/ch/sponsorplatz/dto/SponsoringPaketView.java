package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.SponsoringPaket;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Immutable View-DTO für Sponsoring-Pakete.
 */
public record SponsoringPaketView(
        UUID id,
        String name,
        String beschreibung,
        BigDecimal preisChf,
        boolean aktiv
) {

    public static SponsoringPaketView von(SponsoringPaket paket) {
        return new SponsoringPaketView(
                paket.getId(),
                paket.getName(),
                paket.getBeschreibung(),
                paket.getPreisChf(),
                paket.isAktiv()
        );
    }

    public static List<SponsoringPaketView> von(List<SponsoringPaket> pakete) {
        return pakete.stream().map(SponsoringPaketView::von).toList();
    }
}

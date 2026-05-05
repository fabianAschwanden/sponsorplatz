package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.MedienAsset;

import java.util.List;
import java.util.UUID;

/**
 * View-DTO für Medien-Assets — enthält die URL zum Ausliefern.
 */
public record MedienAssetView(
        UUID id,
        String dateiname,
        String contentType,
        String assetTyp,
        String url
) {

    public static MedienAssetView von(MedienAsset asset) {
        return new MedienAssetView(
                asset.getId(),
                asset.getDateiname(),
                asset.getContentType(),
                asset.getAssetTyp().name(),
                "/medien/" + asset.getId()
        );
    }

    public static List<MedienAssetView> von(List<MedienAsset> assets) {
        return assets.stream().map(MedienAssetView::von).toList();
    }
}


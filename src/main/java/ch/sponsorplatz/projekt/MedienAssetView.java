package ch.sponsorplatz.projekt;


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
        String url,
        long groesseBytes
) {

    public static MedienAssetView von(MedienAsset asset) {
        return new MedienAssetView(
                asset.getId(),
                asset.getDateiname(),
                asset.getContentType(),
                asset.getAssetTyp().name(),
                "/medien/" + asset.getId(),
                asset.getGroesseBytes()
        );
    }

    public static List<MedienAssetView> von(List<MedienAsset> assets) {
        return assets.stream().map(MedienAssetView::von).toList();
    }

    /** True wenn das Asset ein Bild ist (für Thumbnail-Anzeige). */
    public boolean istBild() {
        return contentType != null && contentType.startsWith("image/");
    }

    /** Menschenlesbare Dateigrösse (KB/MB). */
    public String groesseFormatiert() {
        if (groesseBytes < 1024) return groesseBytes + " B";
        if (groesseBytes < 1024 * 1024) return String.format("%.0f KB", groesseBytes / 1024.0);
        return String.format("%.1f MB", groesseBytes / (1024.0 * 1024.0));
    }

    /** Datei-Endung extrahieren (für Icon-Zuordnung im Template). */
    public String endung() {
        if (dateiname == null || !dateiname.contains(".")) return "";
        return dateiname.substring(dateiname.lastIndexOf('.') + 1).toLowerCase();
    }
}


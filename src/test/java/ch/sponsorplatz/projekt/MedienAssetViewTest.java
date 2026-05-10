package ch.sponsorplatz.projekt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für {@link MedienAssetView}.
 * Test-IDs: VIEW-10..12.
 */
class MedienAssetViewTest {

    @Test
    @DisplayName("VIEW-10: MedienAssetView.von mappt alle Felder inkl. groesseBytes")
    void vonMapptAlleFelder() {
        MedienAsset asset = testAsset("bericht.pdf", "application/pdf", 2_500_000L, AssetTyp.ANHANG);

        MedienAssetView view = MedienAssetView.von(asset);

        assertThat(view.id()).isEqualTo(asset.getId());
        assertThat(view.dateiname()).isEqualTo("bericht.pdf");
        assertThat(view.contentType()).isEqualTo("application/pdf");
        assertThat(view.assetTyp()).isEqualTo("ANHANG");
        assertThat(view.url()).isEqualTo("/medien/" + asset.getId());
        assertThat(view.groesseBytes()).isEqualTo(2_500_000L);
    }

    @Test
    @DisplayName("VIEW-11: istBild() true für Bilder, false für Dokumente")
    void istBildErkenntContentType() {
        MedienAssetView bild = MedienAssetView.von(
                testAsset("foto.jpg", "image/jpeg", 100_000L, AssetTyp.COVER));
        MedienAssetView pdf = MedienAssetView.von(
                testAsset("dossier.pdf", "application/pdf", 500_000L, AssetTyp.ANHANG));

        assertThat(bild.istBild()).isTrue();
        assertThat(pdf.istBild()).isFalse();
    }

    @Test
    @DisplayName("VIEW-12: groesseFormatiert() zeigt KB und MB korrekt")
    void groesseFormatiert() {
        assertThat(viewMitGroesse(500).groesseFormatiert()).isEqualTo("500 B");
        assertThat(viewMitGroesse(10_240).groesseFormatiert()).isEqualTo("10 KB");
        assertThat(viewMitGroesse(2_621_440).groesseFormatiert()).isEqualTo("2.5 MB");
    }

    @Test
    @DisplayName("VIEW-12b: endung() extrahiert Datei-Endung korrekt")
    void endungExtrahiert() {
        MedienAssetView pptx = MedienAssetView.von(
                testAsset("praesentation.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        1_000_000L, AssetTyp.ANHANG));
        MedienAssetView ohneEndung = MedienAssetView.von(
                testAsset("README", "text/plain", 100L, AssetTyp.ANHANG));

        assertThat(pptx.endung()).isEqualTo("pptx");
        assertThat(ohneEndung.endung()).isEmpty();
    }

    private MedienAsset testAsset(String dateiname, String contentType, long groesse, AssetTyp typ) {
        MedienAsset a = new MedienAsset();
        a.setId(UUID.randomUUID());
        a.setDateiname(dateiname);
        a.setContentType(contentType);
        a.setGroesseBytes(groesse);
        a.setAssetTyp(typ);
        a.setEntityTyp(EntityTyp.PROJEKT);
        a.setEntityId(UUID.randomUUID());
        a.setStoragePfad("test/" + dateiname);
        return a;
    }

    private MedienAssetView viewMitGroesse(long bytes) {
        return MedienAssetView.von(testAsset("test.bin", "application/octet-stream", bytes, AssetTyp.ANHANG));
    }
}


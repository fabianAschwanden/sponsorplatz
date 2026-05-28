package ch.sponsorplatz.projekt;
import ch.sponsorplatz.shared.storage.StorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedienAssetServiceTest {

    @Mock private MedienAssetRepository repository;
    @Mock private StorageService storageService;

    private MedienAssetService service;

    @BeforeEach
    void setUp() {
        service = new MedienAssetService(repository, storageService);
    }

    /** MA-01: Speichern mit gültigem JPEG funktioniert. */
    @Test
    void speichernMitGueltigemJpegErfolgreich() {
        MockMultipartFile datei = new MockMultipartFile(
                "datei", "bild.jpg", "image/jpeg", new byte[1024]);
        UUID entityId = UUID.randomUUID();

        when(repository.countByEntityTypAndEntityId(EntityTyp.PROJEKT, entityId)).thenReturn(0L);
        when(storageService.speichere(any(), anyString())).thenReturn("projekt/" + entityId + "/bild.jpg");
        when(repository.save(any(MedienAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        MedienAsset asset = service.speichere(datei, EntityTyp.PROJEKT, entityId, AssetTyp.COVER);

        assertThat(asset.getDateiname()).isEqualTo("bild.jpg");
        assertThat(asset.getContentType()).isEqualTo("image/jpeg");
        assertThat(asset.getAssetTyp()).isEqualTo(AssetTyp.COVER);
        verify(storageService).speichere(any(), anyString());
    }

    /** MA-02: Ungültiger Content-Type wird abgelehnt. */
    @Test
    void speichernMitUngueltigemContentTypeWirft() {
        // ZIP ist weder Bild- noch Dokument-Whitelist (Bilder: JPEG/PNG/WebP,
        // Dokumente: PDF/PPTX/DOCX/XLSX). PDF wäre seit Anhang-Feature erlaubt.
        MockMultipartFile datei = new MockMultipartFile(
                "datei", "archive.zip", "application/zip", new byte[100]);

        assertThatThrownBy(() -> service.speichere(datei, EntityTyp.PROJEKT, UUID.randomUUID(), AssetTyp.COVER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ungültiger Dateityp");
    }

    /** MA-02b: SVG ist als Bild-Typ erlaubt (Org-Logo-Upload). */
    @Test
    void speichernSvgWirdAkzeptiert() {
        MockMultipartFile datei = new MockMultipartFile(
                "datei", "logo.svg", "image/svg+xml",
                "<svg xmlns='http://www.w3.org/2000/svg'></svg>".getBytes());
        UUID entityId = UUID.randomUUID();
        when(repository.countByEntityTypAndEntityId(EntityTyp.ORGANISATION, entityId)).thenReturn(0L);
        when(storageService.speichere(any(), anyString())).thenReturn("organisation/" + entityId + "/logo.svg");
        when(repository.save(any(MedienAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        MedienAsset asset = service.speichere(datei, EntityTyp.ORGANISATION, entityId, AssetTyp.LOGO);

        assertThat(asset.getContentType()).isEqualTo("image/svg+xml");
        assertThat(asset.getAssetTyp()).isEqualTo(AssetTyp.LOGO);
    }

    /** MA-03: Datei über 5 MB wird abgelehnt. */
    @Test
    void speichernZuGrossWirft() {
        byte[] grosseDatei = new byte[6 * 1024 * 1024];
        MockMultipartFile datei = new MockMultipartFile(
                "datei", "gross.jpg", "image/jpeg", grosseDatei);

        assertThatThrownBy(() -> service.speichere(datei, EntityTyp.PROJEKT, UUID.randomUUID(), AssetTyp.COVER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 MB");
    }

    /** MA-04: Limit von 10 Assets pro Entity. */
    @Test
    void speichernBeiLimitErreichtWirft() {
        MockMultipartFile datei = new MockMultipartFile(
                "datei", "bild.png", "image/png", new byte[100]);
        UUID entityId = UUID.randomUUID();

        when(repository.countByEntityTypAndEntityId(EntityTyp.PROJEKT, entityId)).thenReturn(10L);

        assertThatThrownBy(() -> service.speichere(datei, EntityTyp.PROJEKT, entityId, AssetTyp.GALERIE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum");
    }

    /** MA-05: Löschen entfernt aus DB und Storage. */
    @Test
    void loeschenEntferntAusDbUndStorage() {
        MedienAsset asset = new MedienAsset();
        asset.setId(UUID.randomUUID());
        asset.setStoragePfad("projekt/123/bild.jpg");
        when(repository.findById(asset.getId())).thenReturn(Optional.of(asset));

        service.loesche(asset.getId());

        verify(storageService).loesche("projekt/123/bild.jpg");
        verify(repository).delete(asset);
    }

    /** MA-06: Leere Datei wird abgelehnt. */
    @Test
    void speichernLeereDateiWirft() {
        MockMultipartFile datei = new MockMultipartFile(
                "datei", "leer.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.speichere(datei, EntityTyp.PROJEKT, UUID.randomUUID(), AssetTyp.COVER))
                .isInstanceOf(IllegalArgumentException.class);
    }
}


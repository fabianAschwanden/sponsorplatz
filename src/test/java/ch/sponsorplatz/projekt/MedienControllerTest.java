package ch.sponsorplatz.projekt;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.shared.storage.StorageService;

@WebMvcTest(controllers = MedienController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class MedienControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedienAssetService medienAssetService;
    @MockitoBean
    private StorageService storageService;
    @MockitoBean
    private ProjektService projektService;
    @MockitoBean
    private OrganisationService organisationService;
    @MockitoBean
    private AccessControl accessControl;
    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** MA-07: GET /medien/{id} liefert Bild mit korrektem Content-Type. */
    @Test
    void ausliefernGibtBildMitContentType() throws Exception {
        UUID id = UUID.randomUUID();
        MedienAsset asset = new MedienAsset();
        asset.setId(id);
        asset.setContentType("image/png");
        asset.setStoragePfad("projekt/123/bild.png");

        when(medienAssetService.findeNachId(id)).thenReturn(Optional.of(asset));
        when(storageService.ladeAlsResource("projekt/123/bild.png"))
                .thenReturn(new ByteArrayResource(new byte[] { 1, 2, 3 }));

        mockMvc.perform(get("/medien/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", "public, max-age=86400"));
    }

    /** MA-08: GET /medien/{id} mit unbekannter ID → 404. */
    @Test
    void ausliefernUnbekannteIdGibt404() throws Exception {
        UUID id = UUID.randomUUID();
        when(medienAssetService.findeNachId(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/medien/{id}", id))
                .andExpect(status().isNotFound());
    }
}

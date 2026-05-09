package ch.sponsorplatz.projekt;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;

@WebMvcTest(controllers = SitemapController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class SitemapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockitoBean
    private ProjektService projektService;

    @MockitoBean
    private OrganisationService organisationService;

    /** SEO-01: GET /sitemap.xml ist public und liefert XML. */
    @Test
    void sitemapIstPublicUndLiefertXml() throws Exception {
        when(projektService.findeOeffentliche()).thenReturn(List.of());
        when(organisationService.alle()).thenReturn(List.of());

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<urlset")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/marktplatz")));
    }
}

package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.OrganisationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Generiert eine XML-Sitemap für Suchmaschinen.
 * Enthält: Startseite, Marktplatz, alle öffentlichen Projekte, alle Vereinsprofile.
 */
@Controller
public class SitemapController {

    private final ProjektService projektService;
    private final OrganisationService organisationService;
    private final String basisUrl;

    public SitemapController(ProjektService projektService,
                             OrganisationService organisationService,
                             @Value("${sponsorplatz.basis-url:http://localhost:8080}") String basisUrl) {
        this.projektService = projektService;
        this.organisationService = organisationService;
        this.basisUrl = basisUrl;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Statische Seiten
        fuegeUrlHinzu(xml, "/", "1.0", "daily");
        fuegeUrlHinzu(xml, "/marktplatz", "0.9", "daily");

        // Öffentliche Projekte (Slugs)
        for (String slug : projektService.findeOeffentlicheSlugs()) {
            fuegeUrlHinzu(xml, "/marktplatz/" + slug, "0.8", "weekly");
        }

        // Vereinsprofile (Slugs)
        for (String slug : organisationService.alleSlugs()) {
            fuegeUrlHinzu(xml, "/vereine/" + slug, "0.7", "weekly");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void fuegeUrlHinzu(StringBuilder xml, String pfad, String prioritaet, String aenderungsFrequenz) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(basisUrl).append(pfad).append("</loc>\n");
        xml.append("    <changefreq>").append(aenderungsFrequenz).append("</changefreq>\n");
        xml.append("    <priority>").append(prioritaet).append("</priority>\n");
        xml.append("  </url>\n");
    }
}

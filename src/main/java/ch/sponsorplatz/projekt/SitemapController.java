package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

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

        // Öffentliche Projekte
        List<Projekt> projekte = projektService.findeOeffentliche();
        for (Projekt p : projekte) {
            fuegeUrlHinzu(xml, "/marktplatz/" + p.getSlug(), "0.8", "weekly");
        }

        // Vereinsprofile
        List<Organisation> orgs = organisationService.alle();
        for (Organisation org : orgs) {
            fuegeUrlHinzu(xml, "/vereine/" + org.getSlug(), "0.7", "weekly");
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


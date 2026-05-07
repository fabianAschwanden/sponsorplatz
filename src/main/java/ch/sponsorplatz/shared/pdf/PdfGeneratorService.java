package ch.sponsorplatz.shared.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Wandelt Thymeleaf-HTML-Templates in PDFs um (HTML/CSS → PDF).
 *
 * <p>Verwendet einen <strong>separaten</strong> {@link SpringTemplateEngine}
 * mit deaktiviertem Fragment-Caching, weil PDF-Templates typischerweise
 * print-spezifisches CSS haben und die Spring-Web-Engine bereits in der
 * App registriert ist.
 *
 * <p>Wichtig für PDF-Rendering: Templates müssen <strong>well-formed XHTML</strong>
 * sein (selbst-schliessende Tags, keine ungeschlossenen {@code <br>}/{@code <img>}).
 * openhtmltopdf parst strikt — fehlerhafte Templates werfen
 * {@link RuntimeException}.
 */
@Service
public class PdfGeneratorService {

    private final TemplateEngine templateEngine;

    public PdfGeneratorService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Rendert ein Thymeleaf-Template zu HTML, dann zu PDF-Bytes.
     *
     * @param templateName Name des Templates ohne {@code .html}-Suffix,
     *                     z.B. {@code "vertrag-pdf"}
     * @param variablen    werden ins Thymeleaf-Modell gestellt
     * @param baseUri      Root-URI für relative Links/Bilder im HTML —
     *                     z.B. {@code "https://sponsorplatz.for-better.biz/"}
     */
    public byte[] erzeuge(String templateName, Map<String, Object> variablen, String baseUri) {
        Context ctx = new Context(Locale.GERMAN);
        ctx.setVariables(variablen);
        String html = templateEngine.process(templateName, ctx);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, baseUri);
            builder.toStream(out);
            builder.run();
        } catch (IOException e) {
            throw new RuntimeException("PDF-Generierung fehlgeschlagen für Template " + templateName, e);
        }
        return out.toByteArray();
    }
}

package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.config.CacheConfig;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Rendert Open-Graph-Preview-Bilder (1200×630 PNG) und cached das Ergebnis
 * pro Slug. Eine separate Bean — {@code @Cacheable} braucht den Spring-Proxy,
 * deshalb darf der Aufruf nicht direkt im Controller liegen (Self-Invocation
 * wäre nicht intercepted).
 *
 * <p>Cache-Region: {@link CacheConfig#OG_IMAGES}, Schlüssel-Prefix unterscheidet
 * Verein und Projekt im selben Region-Bucket — ein Slug kann theoretisch in
 * beiden Welten existieren ohne Kollision.
 */
@Service
public class OgImageRenderer {

    private static final int BREITE = 1200;
    private static final int HOEHE = 630;
    private static final Color FARBE_PRIMAER = new Color(0x1E, 0x3A, 0x5F);
    private static final Color FARBE_AKZENT = new Color(0xE0, 0x7A, 0x5F);
    private static final Color FARBE_CREAM = new Color(0xF4, 0xF1, 0xED);

    private final OrganisationRepository orgRepository;
    private final ProjektRepository projektRepository;

    public OgImageRenderer(OrganisationRepository orgRepository, ProjektRepository projektRepository) {
        this.orgRepository = orgRepository;
        this.projektRepository = projektRepository;
    }

    @Cacheable(value = CacheConfig.OG_IMAGES, key = "'verein:' + #slug")
    public byte[] rendereVerein(String slug) {
        Organisation org = orgRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        // Menschen-lesbares Label statt Enum-Name (z.B. „Mentale Gesundheit"
        // statt „MENTAL_HEALTH"). Aktivieren via Branche.anzeige.
        String brancheTag = org.getBranche() != null ? org.getBranche().getAnzeige() : null;
        return erzeugeBild(org.getName(), null, brancheTag, "Verein auf Sponsorplatz");
    }

    @Cacheable(value = CacheConfig.OG_IMAGES, key = "'projekt:' + #slug")
    public byte[] rendereProjekt(String slug) {
        Projekt projekt = projektRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + slug));
        // Branche aus der Org (visueller Tag oben rechts) + projekt-spezifische
        // Kategorie als Untertitel. Das ist der „Branche-Tag im OG-Image", der
        // im Roadmap-Item 7.2 noch offen war — vorher zeigte das Projekt-OG
        // gar keine Branche.
        String brancheTag = projekt.getOrg() != null && projekt.getOrg().getBranche() != null
                ? projekt.getOrg().getBranche().getAnzeige()
                : null;
        String untertitel = projekt.getKategorie();
        return erzeugeBild(projekt.getName(), untertitel, brancheTag, "Projekt auf Sponsorplatz");
    }

    /**
     * Rendert das OG-Bild. {@code untertitel} ist optional (z.B. Projekt-
     * Kategorie), {@code brancheTag} wird als gerundeter Pill in der Akzent-
     * farbe oben rechts gerendert — passt visuell zum {@code health-hero-chip}
     * im Vereins-Profil-Hero.
     */
    private byte[] erzeugeBild(String titel, String untertitel, String brancheTag, String slogan) {
        BufferedImage bild = new BufferedImage(BREITE, HOEHE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bild.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(FARBE_CREAM);
        g.fillRect(0, 0, BREITE, HOEHE);

        g.setColor(FARBE_AKZENT);
        g.fillRect(0, 0, BREITE, 8);

        g.setColor(FARBE_PRIMAER);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        g.drawString("sponsorplatz.ch", 60, 80);

        g.setColor(new Color(0x64, 0x74, 0x8B));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.drawString(slogan, 60, 120);

        if (brancheTag != null && !brancheTag.isBlank()) {
            zeichneBranchePill(g, brancheTag);
        }

        g.setColor(FARBE_PRIMAER);
        g.setFont(new Font(Font.SERIF, Font.BOLD, 56));
        zeichneMehrzeilig(g, titel, 60, 280, BREITE - 120, 70);

        if (untertitel != null && !untertitel.isBlank()) {
            g.setColor(FARBE_AKZENT);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            g.drawString(untertitel, 60, 460);
        }

        g.setColor(FARBE_PRIMAER);
        g.fillRect(0, HOEHE - 40, BREITE, 40);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        g.drawString("Sport & Gesundheit — Wo Vereine und Marken zueinander finden", 60, HOEHE - 12);

        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(bild, "png", out);
        } catch (IOException e) {
            // ByteArrayOutputStream wirft praktisch nie — wenn doch, Server-Fehler
            throw new UncheckedIOException("PNG-Encoding fehlgeschlagen", e);
        }
        return out.toByteArray();
    }

    /**
     * Zeichnet einen abgerundeten Pill in der Akzentfarbe oben rechts mit
     * dem Branchen-Label. Visuell konsistent zum {@code health-hero-chip}
     * im Vereins-Profil-Hero und zum Branche-Filter-Chip auf dem Marktplatz.
     * Höhe 36px, Padding 18/8, weisser Text, abgerundete Ecken (radius=18).
     */
    private void zeichneBranchePill(Graphics2D g, String label) {
        Font alterFont = g.getFont();
        Color alteFarbe = g.getColor();

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        int textBreite = fm.stringWidth(label);
        int pillBreite = textBreite + 36; // Padding 18 links/rechts
        int pillHoehe = 36;
        int x = BREITE - 60 - pillBreite;
        int y = 60;

        g.setColor(FARBE_AKZENT);
        g.fillRoundRect(x, y, pillBreite, pillHoehe, 36, 36);

        g.setColor(Color.WHITE);
        g.drawString(label, x + 18, y + 24);

        g.setFont(alterFont);
        g.setColor(alteFarbe);
    }

    private void zeichneMehrzeilig(Graphics2D g, String text, int x, int y, int maxBreite, int zeilenHoehe) {
        FontMetrics fm = g.getFontMetrics();
        String[] woerter = text.split(" ");
        StringBuilder zeile = new StringBuilder();
        int aktuellesY = y;
        int maxZeilen = 2;
        int zeilenZaehler = 0;

        for (String wort : woerter) {
            String test = zeile.isEmpty() ? wort : zeile + " " + wort;
            if (fm.stringWidth(test) > maxBreite) {
                g.drawString(zeile.toString(), x, aktuellesY);
                aktuellesY += zeilenHoehe;
                zeilenZaehler++;
                if (zeilenZaehler >= maxZeilen) return;
                zeile = new StringBuilder(wort);
            } else {
                zeile = new StringBuilder(test);
            }
        }
        if (!zeile.isEmpty() && zeilenZaehler < maxZeilen) {
            g.drawString(zeile.toString(), x, aktuellesY);
        }
    }
}

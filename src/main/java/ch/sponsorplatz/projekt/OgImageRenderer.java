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
        String brancheText = org.getBranche() != null ? org.getBranche().name() : "";
        return erzeugeBild(org.getName(), brancheText, "Verein auf Sponsorplatz");
    }

    @Cacheable(value = CacheConfig.OG_IMAGES, key = "'projekt:' + #slug")
    public byte[] rendereProjekt(String slug) {
        Projekt projekt = projektRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + slug));
        String subtitle = projekt.getKategorie() != null ? projekt.getKategorie() : "";
        return erzeugeBild(projekt.getName(), subtitle, "Projekt auf Sponsorplatz");
    }

    private byte[] erzeugeBild(String titel, String untertitel, String slogan) {
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

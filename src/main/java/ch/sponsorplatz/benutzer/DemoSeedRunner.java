package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.anfrage.AnfrageStatus;
import ch.sponsorplatz.anfrage.SponsoringAnfrage;
import ch.sponsorplatz.anfrage.SponsoringAnfrageRepository;
import ch.sponsorplatz.organisation.*;
import ch.sponsorplatz.projekt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Erstellt Beispiel-Daten im Profil "demo" für Stakeholder-Präsentationen
 * und Validierungs-Interviews.
 *
 * <p>Idempotent: Wenn der erste Verein-Slug bereits existiert, wird nichts angelegt.
 * Aktivierung: {@code spring.profiles.active=demo} oder {@code --spring.profiles.active=demo}.
 */
@Component
@Profile("demo")
public class DemoSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);
    private static final String IDEMPOTENZ_SLUG = "fc-beispiel-zuerich";

    private final AppUserRepository userRepository;
    private final OrganisationRepository orgRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final ProjektRepository projektRepository;
    private final SponsoringPaketRepository paketRepository;
    private final SponsoringAnfrageRepository anfrageRepository;
    private final PasswordEncoder encoder;

    public DemoSeedRunner(AppUserRepository userRepository,
                          OrganisationRepository orgRepository,
                          MitgliedschaftRepository mitgliedschaftRepository,
                          ProjektRepository projektRepository,
                          SponsoringPaketRepository paketRepository,
                          SponsoringAnfrageRepository anfrageRepository,
                          PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.projektRepository = projektRepository;
        this.paketRepository = paketRepository;
        this.anfrageRepository = anfrageRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (orgRepository.findBySlug(IDEMPOTENZ_SLUG).isPresent()) {
            log.info("DEMO-Seed: Daten existieren bereits (Slug {} vorhanden) — übersprungen.", IDEMPOTENZ_SLUG);
            return;
        }

        log.info("DEMO-Seed: Erstelle Beispiel-Daten für Demo-Betrieb...");

        // Demo-User (Admin für alle Demos)
        AppUser demoAdmin = erstelleUser("demo@sponsorplatz.ch", "Demo Admin", "demo123", PlatformRolle.PLATFORM_ADMIN);

        // 5 Vereine über verschiedene Health-Branchen
        List<Organisation> vereine = new ArrayList<>();
        vereine.add(erstelleVerein("FC Beispiel Zürich", "fc-beispiel-zuerich", Branche.SPORT,
                "Traditioneller Fussballverein mit 200 Mitgliedern im Herzen von Zürich. Engagement für Jugend- und Seniorensport."));
        vereine.add(erstelleVerein("RehaFit Bern", "rehafit-bern", Branche.REHA,
                "Rehabilitationsverein für Menschen nach Unfällen und Operationen. Bewegungstherapie und Wassergymnastik."));
        vereine.add(erstelleVerein("MindBalance Luzern", "mindbalance-luzern", Branche.MENTAL_HEALTH,
                "Verein für mentale Gesundheit. Meditation, Stressbewältigung und Resilienz-Training in Gruppen."));
        vereine.add(erstelleVerein("VitaFresh Basel", "vitafresh-basel", Branche.ERNAEHRUNG,
                "Ernährungsverein mit Kochkursen, Foodliteracy-Programmen und Schulprojekten rund um gesunde Ernährung."));
        vereine.add(erstelleVerein("Selbsthilfe Aargau", "selbsthilfe-aargau", Branche.SELBSTHILFE,
                "Dachverband für Selbsthilfegruppen im Kanton Aargau. Unterstützung bei chronischen Krankheiten und Sucht."));

        // 3 Sponsor-Orgs (Unternehmen)
        List<Organisation> sponsoren = new ArrayList<>();
        sponsoren.add(erstelleSponsor("CSS Versicherung", "css-versicherung", Branche.PRAEVENTION,
                "Schweizer Krankenversicherung mit Fokus auf Prävention und Gesundheitsförderung."));
        sponsoren.add(erstelleSponsor("TopPharm Apotheken", "toppharm-apotheken", Branche.WELLNESS,
                "Schweizer Apothekenkette mit 100+ Standorten. Partner für Gesundheitsberatung und Prävention."));
        sponsoren.add(erstelleSponsor("BioSuisse Foods", "biosuisse-foods", Branche.ERNAEHRUNG,
                "Nachhaltiger Lebensmittelhersteller mit Schwerpunkt auf biologischer Produktion."));

        // Mitgliedschaften (Demo-Admin ist überall Owner)
        for (Organisation verein : vereine) {
            erstelleMitgliedschaft(demoAdmin, verein, Rolle.ORG_OWNER);
        }
        for (Organisation sponsor : sponsoren) {
            erstelleMitgliedschaft(demoAdmin, sponsor, Rolle.ORG_OWNER);
        }

        // 10 Projekte (2 pro Verein)
        List<Projekt> projekte = new ArrayList<>();
        projekte.add(erstelleProjekt(vereine.get(0), "Sommerfest 2026", "sommerfest-2026", "Sport",
                "Zürich", "Grosses Vereinsfest mit Turnieren, Live-Musik und Tombola für 500 Gäste.",
                LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 15)));
        projekte.add(erstelleProjekt(vereine.get(0), "Junioren-Camp 2026", "junioren-camp-2026", "Jugend",
                "Zürich", "Einwöchiges Fussball-Camp für 80 Kinder zwischen 8 und 14 Jahren.",
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 11)));
        projekte.add(erstelleProjekt(vereine.get(1), "Aqua-Reha-Programm", "aqua-reha-programm", "Rehabilitation",
                "Bern", "12-wöchiges Wassergymnastik-Programm für Patientinnen nach Knie- und Hüft-OPs.",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30)));
        projekte.add(erstelleProjekt(vereine.get(1), "Bewegungstag für alle", "bewegungstag-fuer-alle", "Inklusion",
                "Bern", "Offener Aktionstag mit barrierefreien Bewegungsangeboten für Jung und Alt.",
                LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 20)));
        projekte.add(erstelleProjekt(vereine.get(2), "Achtsamkeits-Retreat", "achtsamkeits-retreat", "Mental Health",
                "Luzern", "Wochenend-Retreat mit Meditation, Yoga und Stressmanagement-Workshops.",
                LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 6)));
        projekte.add(erstelleProjekt(vereine.get(2), "Resilienz-Kurs Herbst", "resilienz-kurs-herbst", "Prävention",
                "Luzern", "8-teiliger Abendkurs zur Stärkung der psychischen Widerstandskraft.",
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 11, 5)));
        projekte.add(erstelleProjekt(vereine.get(3), "Kochkurs Herbst/Winter", "kochkurs-herbst-winter", "Ernährung",
                "Basel", "Saisonale Kochkurse mit Fokus auf regionale, gesunde Lebensmittel.",
                LocalDate.of(2026, 10, 1), LocalDate.of(2027, 2, 28)));
        projekte.add(erstelleProjekt(vereine.get(3), "Schulprojekt Gesund Essen", "schulprojekt-gesund-essen", "Bildung",
                "Basel", "Foodliteracy-Programm für 3 Primarschulen mit 500 Kindern.",
                LocalDate.of(2026, 8, 19), LocalDate.of(2027, 6, 30)));
        projekte.add(erstelleProjekt(vereine.get(4), "Peer-Beratungs-Netzwerk", "peer-beratungs-netzwerk", "Selbsthilfe",
                "Aarau", "Aufbau eines Peer-Beratungsnetzwerks mit ausgebildeten Betroffenen.",
                LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30)));
        projekte.add(erstelleProjekt(vereine.get(4), "Jahrestagung Selbsthilfe", "jahrestagung-selbsthilfe", "Vernetzung",
                "Aarau", "Nationale Tagung mit 200 Teilnehmenden, Workshops und Podiumsdiskussion.",
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15)));

        // Sponsoring-Pakete (je 1 pro Projekt)
        List<SponsoringPaket> pakete = new ArrayList<>();
        for (Projekt p : projekte) {
            pakete.add(erstellePaket(p, "Gold-Sponsor", "Logo auf Banner, Website und Drucksachen. VIP-Plätze am Event.",
                    new BigDecimal("5000.00")));
        }

        // Beispiel-Anfragen (Status-Mix)
        erstelleAnfrage(pakete.get(0), sponsoren.get(0), vereine.get(0), AnfrageStatus.ANGENOMMEN,
                "Wir unterstützen gerne Ihr Sommerfest.", "Vielen Dank, wir freuen uns auf die Zusammenarbeit!");
        erstelleAnfrage(pakete.get(2), sponsoren.get(0), vereine.get(1), AnfrageStatus.ANGENOMMEN,
                "Prävention durch Reha passt perfekt zu uns.", "Wunderbar, Details folgen per Mail.");
        erstelleAnfrage(pakete.get(4), sponsoren.get(1), vereine.get(2), AnfrageStatus.NEU,
                "Achtsamkeit ist uns als Apotheke wichtig.", null);
        erstelleAnfrage(pakete.get(6), sponsoren.get(2), vereine.get(3), AnfrageStatus.ANGENOMMEN,
                "Ernährung und Bio — perfekte Synergie!", "Super, wird ein tolles Projekt!");
        erstelleAnfrage(pakete.get(8), sponsoren.get(1), vereine.get(4), AnfrageStatus.NEU,
                "Selbsthilfe liegt uns am Herzen.", null);

        log.info("DEMO-Seed: {} Vereine, {} Sponsor-Orgs, {} Projekte, {} Anfragen erstellt.",
                vereine.size(), sponsoren.size(), projekte.size(), 5);
    }

    private AppUser erstelleUser(String email, String name, String passwort, PlatformRolle rolle) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setAnzeigename(name);
        user.setPasswortHash(encoder.encode(passwort));
        user.setAktiv(true);
        user.setEmailVerifiziert(true);
        user.setPlatformRolle(rolle);
        return userRepository.save(user);
    }

    private Organisation erstelleVerein(String name, String slug, Branche branche, String beschreibung) {
        Organisation org = new Organisation();
        org.setName(name);
        org.setSlug(slug);
        org.setTyp(OrgTyp.VEREIN);
        org.setBranche(branche);
        org.setBeschreibung(beschreibung);
        org.setStatus(OrgStatus.VERIFIED);
        return orgRepository.save(org);
    }

    private Organisation erstelleSponsor(String name, String slug, Branche branche, String beschreibung) {
        Organisation org = new Organisation();
        org.setName(name);
        org.setSlug(slug);
        org.setTyp(OrgTyp.UNTERNEHMEN);
        org.setBranche(branche);
        org.setBeschreibung(beschreibung);
        org.setStatus(OrgStatus.VERIFIED);
        return orgRepository.save(org);
    }

    private void erstelleMitgliedschaft(AppUser user, Organisation org, Rolle rolle) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(rolle);
        mitgliedschaftRepository.save(m);
    }

    private Projekt erstelleProjekt(Organisation org, String name, String slug, String kategorie,
                                    String ort, String beschreibung, LocalDate start, LocalDate ende) {
        Projekt p = new Projekt();
        p.setOrg(org);
        p.setName(name);
        p.setSlug(slug);
        p.setKategorie(kategorie);
        p.setOrt(ort);
        p.setBeschreibung(beschreibung);
        p.setStartDatum(start);
        p.setEndDatum(ende);
        p.setSichtbarkeit(Sichtbarkeit.OEFFENTLICH);
        p.setVeroeffentlichtAm(Instant.now());
        return projektRepository.save(p);
    }

    private SponsoringPaket erstellePaket(Projekt projekt, String name, String beschreibung, BigDecimal preis) {
        SponsoringPaket paket = new SponsoringPaket();
        paket.setProjekt(projekt);
        paket.setName(name);
        paket.setBeschreibung(beschreibung);
        paket.setPreisChf(preis);
        paket.setAktiv(true);
        return paketRepository.save(paket);
    }

    private void erstelleAnfrage(SponsoringPaket paket, Organisation anfragender, Organisation empfaenger,
                                 AnfrageStatus status, String nachricht, String antwort) {
        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setPaket(paket);
        anfrage.setAnfragenderOrg(anfragender);
        anfrage.setEmpfaengerOrg(empfaenger);
        anfrage.setStatus(status);
        anfrage.setNachricht(nachricht);
        anfrage.setAntwort(antwort);
        anfrage.setKontaktName("Demo Kontakt");
        anfrage.setKontaktEmail("demo@sponsorplatz.ch");
        if (status == AnfrageStatus.ANGENOMMEN || status == AnfrageStatus.ABGELEHNT) {
            anfrage.setBeantwortetAm(Instant.now());
        }
        anfrageRepository.save(anfrage);
    }
}


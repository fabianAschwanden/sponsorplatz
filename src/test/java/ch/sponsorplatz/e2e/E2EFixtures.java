package ch.sponsorplatz.e2e;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.organisation.SponsorBranche;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Test-Daten-Helper fuer die E2E-Suite — wird von {@link E2EHooks} pro Scenario
 * aufgerufen, um konsistente Ausgangslage zu schaffen.
 *
 * <p><b>Bereinige:</b> loescht TRUNCATE auf saemtlichen veraenderlichen
 * Tabellen, damit Email-Unique-Constraints zwischen Szenarien nicht kollidieren.
 * Flyway-Migrationen bleiben erhalten — wir loeschen Daten, nicht Schema.
 *
 * <p><b>SeedeCssSponsor:</b> legt die Test-Sponsor-Org „CSS Versicherung" plus
 * einen Sponsor-Owner ({@code sponsor@css.test}, Passwort
 * {@code Sponsor1234!}) an, die E-Mail ist auf {@code emailVerifiziert=true}
 * gesetzt damit der Login direkt funktioniert.
 */
@Component
public class E2EFixtures {

    /** Konstanten, die Steps + Fixtures gemeinsam nutzen. */
    public static final String SPONSOR_NAME = "CSS Versicherung";
    public static final String SPONSOR_EMAIL = "sponsor@css.test";
    public static final String SPONSOR_PASSWORT = "Sponsor1234!";
    public static final String SPONSOR_ANZEIGENAME = "Sponsor-Owner";

    private final OrganisationRepository orgRepository;
    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final MitgliedschaftService mitgliedschaftService;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager em;

    public E2EFixtures(OrganisationRepository orgRepository,
                       AppUserRepository appUserRepository,
                       MitgliedschaftRepository mitgliedschaftRepository,
                       MitgliedschaftService mitgliedschaftService,
                       PasswordEncoder passwordEncoder) {
        this.orgRepository = orgRepository;
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.mitgliedschaftService = mitgliedschaftService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * TRUNCATE auf allen DB-Tabellen, die durch Tests geaendert werden.
     * Reihenfolge respektiert FK-Constraints (Kind-Tabellen zuerst); via
     * {@code RESTART IDENTITY CASCADE} ist die Liste resilient gegen neue
     * Joins, die noch nicht explizit erwaehnt sind.
     */
    @Transactional
    public void bereinige() {
        em.createNativeQuery("""
                TRUNCATE TABLE
                  nachricht, vertrag, rechnung,
                  sponsoring_anfrage, sponsoring_paket, projekt,
                  einladung, mitgliedschaft,
                  organisation, app_user
                RESTART IDENTITY CASCADE
                """).executeUpdate();
    }

    /**
     * Legt den Sponsor-Org + Sponsor-Owner an. Liefert die Org-ID zurueck,
     * damit Steps darauf referenzieren koennen.
     */
    @Transactional
    public UUID seedeCssSponsor() {
        AppUser owner = new AppUser();
        owner.setEmail(SPONSOR_EMAIL);
        owner.setAnzeigename(SPONSOR_ANZEIGENAME);
        owner.setPasswortHash(passwordEncoder.encode(SPONSOR_PASSWORT));
        owner.setEmailVerifiziert(true);
        owner.setAktiv(true);
        owner.setOnboardingGesehen(true);
        appUserRepository.save(owner);

        Organisation org = new Organisation();
        org.setName(SPONSOR_NAME);
        org.setSlug("css-versicherung");
        org.setTyp(OrgTyp.UNTERNEHMEN);
        org.setSponsorBranche(SponsorBranche.VERSICHERUNG);
        org.setStatus(OrgStatus.ACTIVE);
        orgRepository.save(org);

        mitgliedschaftService.fuegeHinzu(org.getId(), owner.getId(), Rolle.ORG_OWNER, null);
        return org.getId();
    }

    /**
     * Markiert einen ueber die Registrierungs-Page erstellten User als
     * E-Mail-verifiziert, sodass der Login durchgeht. Vermeidet im Pilot den
     * Detour ueber Mail-Capture; entspricht dem fachlichen Effekt vom Klick
     * auf den Verifizierungs-Link.
     */
    @Transactional
    public void markiereEmailVerifiziert(String email) {
        appUserRepository.findByEmail(email).ifPresent(u -> {
            u.setEmailVerifiziert(true);
            appUserRepository.save(u);
        });
    }

    public OrganisationRepository orgRepository() { return orgRepository; }
    public AppUserRepository appUserRepository() { return appUserRepository; }
    public MitgliedschaftRepository mitgliedschaftRepository() {
        return mitgliedschaftRepository;
    }
}

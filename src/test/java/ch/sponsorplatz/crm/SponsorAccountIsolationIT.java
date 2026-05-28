package ch.sponsorplatz.crm;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.organisation.Rolle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CRM-ISO-01..03 — beweist die Mandanten-Isolation der privaten Sponsor-Layer
 * (ADR-0011) gegen die ECHTE DB: voller Stack H2 + JPA + AccessControl + echte
 * Mitgliedschaften, kein Mock. Dies ist das Sicherheitsnetz, das jeder weiteren
 * CRM-Entität vorausgeht.
 *
 * <p>Szenario: Sponsor A (CSS) und Sponsor B (Helsana) sind konkurrierende
 * Sponsor-Orgs. uA ist Mitglied von A, uB von B. A pflegt einen CRM-Account.
 * uB darf diesen niemals sehen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
class SponsorAccountIsolationIT {

    @Autowired private SponsorAccountService service;
    @Autowired private KontaktPersonService kontaktService;
    @Autowired private AktivitaetService aktivitaetService;
    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private MitgliedschaftRepository mitgliedschaftRepository;

    private UUID sponsorAId;
    private UUID sponsorBId;
    private UUID accountAId;
    private Authentication authUA;
    private Authentication authUB;

    @BeforeEach
    void setUp() {
        Organisation sponsorA = sponsorOrg("CSS", "css-iso");
        Organisation sponsorB = sponsorOrg("Helsana", "helsana-iso");
        Organisation verein = vereinOrg("FC Iso", "fc-iso");
        sponsorAId = sponsorA.getId();
        sponsorBId = sponsorB.getId();

        AppUser uA = user("ua-iso@css.ch");
        AppUser uB = user("ub-iso@helsana.ch");
        mitglied(uA, sponsorA, Rolle.ORG_OWNER);
        mitglied(uB, sponsorB, Rolle.ORG_OWNER);

        authUA = auth(uA.getEmail());
        authUB = auth(uB.getEmail());

        // A legt einen CRM-Account für den Verein an + einen Kontakt darauf
        accountAId = service.erstelle(sponsorAId, verein.getId(), authUA).id();
        kontaktService.erstelle(accountAId, "Anna", "Muster", "Präsidentin",
                KontaktRolle.HAUPTANSPRECHPARTNER, "anna@v.ch", "044", "079", authUA);
        aktivitaetService.erstelle(accountAId, ch.sponsorplatz.crm.AktivitaetTyp.ANRUF,
                java.time.LocalDate.now(), "Erstkontakt", "Interessiert", null, authUA);
    }

    /** CRM-ISO-01: Eigentümer-Sponsor sieht seinen Account im Portfolio. */
    @Test
    @DisplayName("CRM-ISO-01: Sponsor A sieht eigenen Account")
    void eigentuemerSiehtAccount() {
        List<SponsorAccountView> portfolioA = service.findePortfolio(sponsorAId, authUA);
        assertThat(portfolioA).hasSize(1);
        assertThat(portfolioA.get(0).vereinName()).isEqualTo("FC Iso");
    }

    /** CRM-ISO-02: Konkurrierender Sponsor B bekommt AccessDenied auf A's Portfolio. */
    @Test
    @DisplayName("CRM-ISO-02: Sponsor B darf A's Portfolio NICHT lesen")
    void konkurrentBekommtKeinenZugriff() {
        assertThatThrownBy(() -> service.findePortfolio(sponsorAId, authUB))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** CRM-ISO-03: B's eigenes Portfolio ist leer — A's Account leakt nicht herüber. */
    @Test
    @DisplayName("CRM-ISO-03: B's Portfolio enthält A's Account nicht")
    void keinLeakInsFremdePortfolio() {
        List<SponsorAccountView> portfolioB = service.findePortfolio(sponsorBId, authUB);
        assertThat(portfolioB).isEmpty();
    }

    /** CRM-ISO-04: Eigentümer sieht die Kontakte seines Accounts. */
    @Test
    @DisplayName("CRM-ISO-04: Sponsor A sieht Kontakte des eigenen Accounts")
    void eigentuemerSiehtKontakte() {
        assertThat(kontaktService.findeKontakte(accountAId, authUA))
                .extracting(KontaktPersonView::name)
                .containsExactly("Anna Muster");
    }

    /** CRM-ISO-05: Konkurrent B darf die Kontakte von A's Account NICHT lesen. */
    @Test
    @DisplayName("CRM-ISO-05: Sponsor B darf A's Kontakte NICHT lesen")
    void konkurrentSiehtKeineKontakte() {
        assertThatThrownBy(() -> kontaktService.findeKontakte(accountAId, authUB))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** CRM-ISO-06: Eigentümer sieht die Aktivitäts-Timeline des eigenen Accounts. */
    @Test
    @DisplayName("CRM-ISO-06: Sponsor A sieht die Timeline des eigenen Accounts")
    void eigentuemerSiehtTimeline() {
        assertThat(aktivitaetService.findeTimeline(accountAId, authUA))
                .extracting(AktivitaetView::betreff)
                .containsExactly("Erstkontakt");
    }

    /** CRM-ISO-07: Konkurrent B darf die Timeline von A's Account NICHT lesen. */
    @Test
    @DisplayName("CRM-ISO-07: Sponsor B darf A's Timeline NICHT lesen")
    void konkurrentSiehtKeineTimeline() {
        assertThatThrownBy(() -> aktivitaetService.findeTimeline(accountAId, authUB))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- Fixtures ---

    private Organisation sponsorOrg(String name, String slug) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.UNTERNEHMEN);
        o.setSponsorBranche(ch.sponsorplatz.organisation.SponsorBranche.VERSICHERUNG);
        o.setStatus(OrgStatus.ACTIVE);
        o.setRegistriertAm(Instant.now());
        return organisationRepository.save(o);
    }

    private Organisation vereinOrg(String name, String slug) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.VEREIN);
        o.setBranche(ch.sponsorplatz.organisation.Branche.SPORT);
        o.setStatus(OrgStatus.ACTIVE);
        o.setRegistriertAm(Instant.now());
        return organisationRepository.save(o);
    }

    private AppUser user(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setAnzeigename(email);
        u.setPasswortHash("$2a$test");
        u.setAktiv(true);
        u.setEmailVerifiziert(true);
        return appUserRepository.save(u);
    }

    private void mitglied(AppUser user, Organisation org, Rolle rolle) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(rolle);
        mitgliedschaftRepository.save(m);
    }

    private Authentication auth(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }
}

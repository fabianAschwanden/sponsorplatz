package ch.sponsorplatz.crm;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.organisation.SponsorBranche;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CRM-IO-01..04 — CSV-Import/-Export der CRM-Portfolio-Daten gegen die echte DB,
 * inkl. Round-Trip (Update + Neuanlage), Fehlerzeile und Mandanten-Isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
class CrmImportExportIT {

    @Autowired private CrmImportExportService service;
    @Autowired private SponsorAccountRepository accountRepository;
    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private MitgliedschaftRepository mitgliedschaftRepository;

    private UUID sponsorId;
    private Organisation verein1;
    private Organisation verein2;
    private Authentication authMitglied;
    private Authentication authFremd;

    @BeforeEach
    void setUp() {
        Organisation sponsor = sponsorOrg("CSS", "css-io");
        sponsorId = sponsor.getId();
        verein1 = vereinOrg("FC Eins", "fc-io-1");
        verein2 = vereinOrg("FC Zwei", "fc-io-2");

        AppUser uMitglied = user("io-mitglied@css.ch");
        AppUser uFremd = user("io-fremd@x.ch");
        mitglied(uMitglied, sponsor, Rolle.ORG_OWNER);
        authMitglied = auth(uMitglied.getEmail());
        authFremd = auth(uFremd.getEmail());

        // Bestehender Account für verein1
        SponsorAccount a = new SponsorAccount();
        a.setBesitzerSponsorOrgId(sponsorId);
        a.setVerein(verein1);
        a.setStatus(AccountStatus.AKTIV);
        a.setTier(AccountTier.CORE);
        accountRepository.save(a);
    }

    /** CRM-IO-01: Export liefert Header + Portfolio-Zeile mit Verein-Slug/Name/Status. */
    @Test
    @DisplayName("CRM-IO-01: Export enthält Header + Account-Zeile")
    void export() {
        String csv = new String(service.exportiere(sponsorId, authMitglied), StandardCharsets.UTF_8);
        assertThat(csv).contains(CrmImportExportService.HEADER);
        assertThat(csv).contains("fc-io-1;FC Eins;AKTIV");
    }

    /** CRM-IO-02: Import aktualisiert bestehenden Account, legt neuen an, meldet Fehlerzeile. */
    @Test
    @DisplayName("CRM-IO-02: Import = Upsert + Fehlerreport")
    void importUpsert() {
        String csv = CrmImportExportService.HEADER + "\r\n"
                + "fc-io-1;FC Eins;IN_RENEWAL;STRATEGIC;GEWONNEN;9000;Aktualisiert\r\n"
                + "fc-io-2;FC Zwei;LEAD;;ANGEBOT;;Neu angelegt\r\n"
                + "gibts-nicht;;LEAD;;LEAD;;\r\n";

        var ergebnis = service.importiere(sponsorId, csv.getBytes(StandardCharsets.UTF_8), authMitglied);

        assertThat(ergebnis.erstellt()).isEqualTo(1);
        assertThat(ergebnis.aktualisiert()).isEqualTo(1);
        assertThat(ergebnis.fehler()).hasSize(1);
        assertThat(ergebnis.fehler().get(0)).contains("gibts-nicht");

        SponsorAccount a1 = accountRepository
                .findByBesitzerSponsorOrgIdAndVereinId(sponsorId, verein1.getId()).orElseThrow();
        assertThat(a1.getStatus()).isEqualTo(AccountStatus.IN_RENEWAL);
        assertThat(a1.getTier()).isEqualTo(AccountTier.STRATEGIC);
        assertThat(a1.getPipelineStage()).isEqualTo(PipelineStage.GEWONNEN);
        assertThat(a1.getForecastBetragChf()).isEqualByComparingTo("9000");

        assertThat(accountRepository
                .findByBesitzerSponsorOrgIdAndVereinId(sponsorId, verein2.getId())).isPresent();
    }

    /** CRM-IO-03: Export durch fremden User → AccessDenied. */
    @Test
    @DisplayName("CRM-IO-03: Export durch Fremden → AccessDenied")
    void exportFremd() {
        assertThatThrownBy(() -> service.exportiere(sponsorId, authFremd))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** CRM-IO-04: Import durch fremden User → AccessDenied. */
    @Test
    @DisplayName("CRM-IO-04: Import durch Fremden → AccessDenied")
    void importFremd() {
        byte[] csv = (CrmImportExportService.HEADER + "\r\nfc-io-2;;LEAD;;LEAD;;\r\n")
                .getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> service.importiere(sponsorId, csv, authFremd))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- Fixtures ---

    private Organisation sponsorOrg(String name, String slug) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.UNTERNEHMEN);
        o.setSponsorBranche(SponsorBranche.VERSICHERUNG);
        o.setStatus(OrgStatus.ACTIVE);
        o.setRegistriertAm(Instant.now());
        return organisationRepository.save(o);
    }

    private Organisation vereinOrg(String name, String slug) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.VEREIN);
        o.setBranche(Branche.SPORT);
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

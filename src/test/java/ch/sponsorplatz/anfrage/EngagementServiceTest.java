package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.projekt.Projekt;
import ch.sponsorplatz.projekt.SponsoringPaket;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.shared.medien.OrganisationLogoLookup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {

    @Mock private SponsoringAnfrageRepository anfrageRepository;
    @Mock private OrganisationRepository orgRepository;
    @Mock private OrganisationLogoLookup logoLookup;
    @InjectMocks private EngagementService service;

    @Test
    @DisplayName("ENG-01: findeNachSponsorSlug liefert nur ANGENOMMEN-Anfragen")
    void nurAngenommeneAnfragen() {
        Organisation sponsor = erstelleOrg("css-versicherung", Branche.PRAEVENTION);
        when(orgRepository.findBySlug("css-versicherung")).thenReturn(Optional.of(sponsor));

        SponsoringAnfrage anfrage = erstelleAnfrage(sponsor);
        when(anfrageRepository.findByAnfragenderOrgIdAndStatusOrderByCreatedAtDesc(
                sponsor.getId(), AnfrageStatus.ANGENOMMEN))
                .thenReturn(List.of(anfrage));

        List<SponsoringAnfrage> result = service.findeNachSponsorSlug("css-versicherung");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("ENG-02: findeNachSponsorSlug mit unbekanntem Slug wirft NotFoundException")
    void unbekannterSlugWirft() {
        when(orgRepository.findBySlug("nix")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findeNachSponsorSlug("nix"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("ENG-04: findeSchaufenster baut Marken-Kopf + Logo + Region-Gruppen")
    void schaufensterMitLogoUndGruppen() {
        Organisation sponsor = erstelleOrg("css-versicherung", Branche.PRAEVENTION);
        when(orgRepository.findBySlug("css-versicherung")).thenReturn(Optional.of(sponsor));
        when(anfrageRepository.findByAnfragenderOrgIdAndStatusOrderByCreatedAtDesc(
                sponsor.getId(), AnfrageStatus.ANGENOMMEN))
                .thenReturn(List.of(erstelleAnfrageMitOrt(sponsor, "Zürich"),
                        erstelleAnfrageMitOrt(sponsor, "Bern")));
        // lenient: mitLogos schlägt zusätzlich die Verein-Logos nach (andere IDs) —
        // dieser Stub gilt nur für das Marken-Logo im Hero.
        lenient().when(logoLookup.findeLogoUrl(sponsor.getId())).thenReturn(Optional.of("/medien/logo-1"));

        SchaufensterAnsicht ansicht = service.findeSchaufenster("css-versicherung", null, null);

        assertThat(ansicht.sponsorName()).isEqualTo("Test css-versicherung");
        assertThat(ansicht.sponsorLogoUrl()).isEqualTo("/medien/logo-1");
        assertThat(ansicht.nachRegion().keySet()).containsExactly("Bern", "Zürich");
        assertThat(ansicht.anzahlVereine()).isEqualTo(2);
        assertThat(ansicht.verfuegbareRegionen()).containsExactly("Bern", "Zürich");
    }

    @Test
    @DisplayName("ENG-05: findeNeuesteEngagements mappt ANGENOMMEN-Anfragen quer über alle Marken")
    void neuesteEngagements() {
        Organisation sponsor = erstelleOrg("css-versicherung", Branche.PRAEVENTION);
        when(anfrageRepository.findNeuesteNachStatus(eq(AnfrageStatus.ANGENOMMEN), any()))
                .thenReturn(List.of(erstelleAnfrage(sponsor)));

        List<EngagementView> result = service.findeNeuesteEngagements(6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sponsorSlug()).isEqualTo("css-versicherung");
    }

    /** ENG-06: Kontakt-Anfrage ohne Paket wird inkl. gemappt (null Projekt-Felder), kein NPE (500-Regression). */
    @Test
    @DisplayName("ENG-06: Kontakt-Anfrage ohne Paket wird gemappt statt NPE")
    void kontaktAnfrageOhnePaketWirdGemappt() {
        Organisation sponsor = erstelleOrg("css-versicherung", Branche.PRAEVENTION);
        SponsoringAnfrage kontaktOhnePaket = new SponsoringAnfrage();
        kontaktOhnePaket.setAnfragenderOrg(sponsor);
        kontaktOhnePaket.setEmpfaengerOrg(erstelleOrg("verein-kontakt", Branche.SPORT));
        kontaktOhnePaket.setStatus(AnfrageStatus.ANGENOMMEN);
        // paket bleibt null (Kontakt-Anfrage)
        when(anfrageRepository.findNeuesteNachStatus(eq(AnfrageStatus.ANGENOMMEN), any()))
                .thenReturn(List.of(kontaktOhnePaket, erstelleAnfrage(sponsor)));

        List<EngagementView> result = service.findeNeuesteEngagements(6);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(e -> e.projektName() == null);  // Kontakt-Engagement
        assertThat(result).anyMatch(e -> e.projektName() != null);  // Paket-Engagement
    }

    /** ENG-07: findeNeuesteEngagements reichert die Verein-Logo-URL an. */
    @Test
    @DisplayName("ENG-07: Verein-Logo wird in die Engagements gemappt")
    void neuesteEngagementsMitVereinLogo() {
        Organisation sponsor = erstelleOrg("css-versicherung", Branche.PRAEVENTION);
        SponsoringAnfrage anfrage = erstelleAnfrage(sponsor);
        UUID vereinId = anfrage.getEmpfaengerOrg().getId();
        when(anfrageRepository.findNeuesteNachStatus(eq(AnfrageStatus.ANGENOMMEN), any()))
                .thenReturn(List.of(anfrage));
        when(logoLookup.findeLogoUrl(vereinId)).thenReturn(Optional.of("/medien/v"));

        List<EngagementView> result = service.findeNeuesteEngagements(6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).vereinLogoUrl()).isEqualTo("/medien/v");
    }

    private Organisation erstelleOrg(String slug, Branche branche) {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setSlug(slug);
        org.setName("Test " + slug);
        org.setBranche(branche);
        // Org-Typ steuert die Rollen-Auflösung in EngagementView.von:
        // Vereins-Slugs → VEREIN, Marken (Sponsoren) → UNTERNEHMEN.
        org.setTyp(slug.startsWith("verein") ? OrgTyp.VEREIN : OrgTyp.UNTERNEHMEN);
        return org;
    }

    private SponsoringAnfrage erstelleAnfrage(Organisation sponsor) {
        Organisation verein = erstelleOrg("verein-test", Branche.SPORT);
        Projekt p = new Projekt();
        p.setName("Test-Projekt");
        p.setSlug("test-projekt");
        p.setOrt("Zürich");
        p.setOrg(verein);
        SponsoringPaket paket = new SponsoringPaket();
        paket.setName("Gold");
        paket.setProjekt(p);

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setAnfragenderOrg(sponsor);
        a.setEmpfaengerOrg(verein);
        a.setPaket(paket);
        a.setStatus(AnfrageStatus.ANGENOMMEN);
        a.setBeantwortetAm(Instant.now());
        return a;
    }

    private SponsoringAnfrage erstelleAnfrageMitOrt(Organisation sponsor, String ort) {
        Organisation verein = erstelleOrg("verein-" + ort.toLowerCase(), Branche.SPORT);
        Projekt p = new Projekt();
        p.setName("Projekt " + ort);
        p.setSlug("projekt-" + ort.toLowerCase());
        p.setOrt(ort);
        p.setOrg(verein);
        SponsoringPaket paket = new SponsoringPaket();
        paket.setName("Gold");
        paket.setProjekt(p);

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setAnfragenderOrg(sponsor);
        a.setEmpfaengerOrg(verein);
        a.setPaket(paket);
        a.setStatus(AnfrageStatus.ANGENOMMEN);
        a.setBeantwortetAm(Instant.now());
        return a;
    }
}


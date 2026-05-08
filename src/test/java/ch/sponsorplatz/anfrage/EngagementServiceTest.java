package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.projekt.Projekt;
import ch.sponsorplatz.projekt.SponsoringPaket;
import ch.sponsorplatz.shared.exception.NotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {

    @Mock private SponsoringAnfrageRepository anfrageRepository;
    @Mock private OrganisationRepository orgRepository;
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
    @DisplayName("ENG-03: findeNachSponsorSlugUndRegion filtert nach Region")
    void regionFilter() {
        Organisation sponsor = erstelleOrg("css-versicherung", Branche.PRAEVENTION);
        when(orgRepository.findBySlug("css-versicherung")).thenReturn(Optional.of(sponsor));

        SponsoringAnfrage zurich = erstelleAnfrageMitOrt(sponsor, "Zürich");
        SponsoringAnfrage bern = erstelleAnfrageMitOrt(sponsor, "Bern");
        when(anfrageRepository.findByAnfragenderOrgIdAndStatusOrderByCreatedAtDesc(
                sponsor.getId(), AnfrageStatus.ANGENOMMEN))
                .thenReturn(List.of(zurich, bern));

        List<SponsoringAnfrage> result = service.findeNachSponsorSlugUndRegion("css-versicherung", "Zürich");
        assertThat(result).hasSize(1);
    }

    private Organisation erstelleOrg(String slug, Branche branche) {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setSlug(slug);
        org.setName("Test " + slug);
        org.setBranche(branche);
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


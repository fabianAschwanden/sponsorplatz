package ch.sponsorplatz.organisation;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.AppUser;

import ch.sponsorplatz.benutzer.AppUserFormDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für SponsorRegistrierungService.
 * Test-IDs: SR-01, SR-02, SR-03
 */
@ExtendWith(MockitoExtension.class)
class SponsorRegistrierungServiceTest {

    @Mock
    private AppUserService appUserService;

    @Mock
    private OrganisationService organisationService;

    @Mock
    private MitgliedschaftService mitgliedschaftService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SponsorRegistrierungService service;

    @BeforeEach
    void setUp() {
        service = new SponsorRegistrierungService(appUserService, organisationService,
                mitgliedschaftService, eventPublisher);
    }

    private SponsorRegistrierungFormDto gueltigesFormular() {
        SponsorRegistrierungFormDto dto = new SponsorRegistrierungFormDto();
        dto.setEmail("sponsor@firma.ch");
        dto.setAnzeigename("Max Muster");
        dto.setPasswort("sicheres-passwort");
        dto.setFirmenname("Muster AG");
        dto.setSponsorBranche(SponsorBranche.SPORTARTIKEL);
        dto.setRechtsform("AG");
        dto.setWebsiteUrl("https://muster.ch");
        dto.setBeschreibung("Sportartikel-Hersteller");
        return dto;
    }

    @Test
    @DisplayName("SR-01: registriereSponsor erstellt AppUser, Organisation (UNTERNEHMEN/PENDING), Mitgliedschaft (ORG_OWNER)")
    void registriereSponsor_erstelltAlles() {
        SponsorRegistrierungFormDto dto = gueltigesFormular();

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail(dto.getEmail());
        when(appUserService.registriere(any(AppUserFormDto.class))).thenReturn(user);

        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setTyp(OrgTyp.UNTERNEHMEN);
        org.setStatus(OrgStatus.PENDING);
        when(organisationService.erstelle(any(OrganisationFormDto.class))).thenReturn(org);

        Mitgliedschaft mitgliedschaft = new Mitgliedschaft();
        when(mitgliedschaftService.fuegeHinzu(any(), any(), any(), any())).thenReturn(mitgliedschaft);

        service.registriereSponsor(dto);

        // User erstellt mit korrekten Daten
        ArgumentCaptor<AppUserFormDto> userCaptor = ArgumentCaptor.forClass(AppUserFormDto.class);
        verify(appUserService).registriere(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("sponsor@firma.ch");
        assertThat(userCaptor.getValue().getAnzeigename()).isEqualTo("Max Muster");
        assertThat(userCaptor.getValue().getPasswort()).isEqualTo("sicheres-passwort");

        // Org erstellt als UNTERNEHMEN
        ArgumentCaptor<OrganisationFormDto> orgCaptor = ArgumentCaptor.forClass(OrganisationFormDto.class);
        verify(organisationService).erstelle(orgCaptor.capture());
        assertThat(orgCaptor.getValue().getTyp()).isEqualTo(OrgTyp.UNTERNEHMEN);
        assertThat(orgCaptor.getValue().getName()).isEqualTo("Muster AG");
        assertThat(orgCaptor.getValue().getSponsorBranche()).isEqualTo(SponsorBranche.SPORTARTIKEL);
        assertThat(orgCaptor.getValue().getBranche()).isNull();

        // Mitgliedschaft als ORG_OWNER
        verify(mitgliedschaftService).fuegeHinzu(eq(org.getId()), eq(user.getId()), eq(Rolle.ORG_OWNER), eq(null));

        // Admins werden über die neue Registrierung benachrichtigt — via Spring-Event
        verify(eventPublisher).publishEvent(new NeueOrgRegistrierungEvent(org));
    }

    @Test
    @DisplayName("SR-02: Doppelte E-Mail → IllegalArgumentException (delegiert an AppUserService)")
    void registriereSponsor_doppelteEmail_wirft() {
        SponsorRegistrierungFormDto dto = gueltigesFormular();
        when(appUserService.registriere(any(AppUserFormDto.class)))
                .thenThrow(new IllegalArgumentException("E-Mail ist bereits vergeben"));

        assertThatThrownBy(() -> service.registriereSponsor(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-Mail ist bereits vergeben");

        verify(organisationService, never()).erstelle(any());
    }

    @Test
    @DisplayName("SR-03: Slug-Konflikt bei Firmenname → IllegalArgumentException")
    void registriereSponsor_slugKonflikt_wirft() {
        SponsorRegistrierungFormDto dto = gueltigesFormular();

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        when(appUserService.registriere(any(AppUserFormDto.class))).thenReturn(user);
        when(organisationService.erstelle(any(OrganisationFormDto.class)))
                .thenThrow(new IllegalArgumentException("Slug bereits vergeben: muster-ag"));

        assertThatThrownBy(() -> service.registriereSponsor(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Slug bereits vergeben");
    }
}


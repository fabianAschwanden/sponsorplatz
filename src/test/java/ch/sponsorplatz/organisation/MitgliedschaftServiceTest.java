package ch.sponsorplatz.organisation;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MitgliedschaftServiceTest {

    private MitgliedschaftRepository mitgliedschaftRepository;
    private AppUserRepository appUserRepository;
    private OrganisationRepository organisationRepository;
    private MitgliedschaftService service;

    @BeforeEach
    void setUp() {
        mitgliedschaftRepository = mock(MitgliedschaftRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        organisationRepository = mock(OrganisationRepository.class);
        service = new MitgliedschaftService(mitgliedschaftRepository, appUserRepository, organisationRepository);
    }

    /** MG-03: fuegeHinzu — zweite Mitgliedschaft gleiche org/user → IllegalStateException. */
    @Test
    void fuegeHinzuWirftBeiDoppelterMitgliedschaft() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(organisationRepository.findById(orgId)).thenReturn(Optional.of(new Organisation()));
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(new AppUser()));
        when(mitgliedschaftRepository.existsByUserIdAndOrgId(userId, orgId)).thenReturn(true);

        assertThatThrownBy(() -> service.fuegeHinzu(orgId, userId, Rolle.ORG_EDITOR, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bereits Mitglied");
    }

    /** MG-04: findeNachOrg gibt alle Mitglieder einer Org zurück. */
    @Test
    void findeNachOrgGibtAlleMitgliederZurueck() {
        UUID orgId = UUID.randomUUID();

        Mitgliedschaft m1 = new Mitgliedschaft();
        Mitgliedschaft m2 = new Mitgliedschaft();
        when(mitgliedschaftRepository.findByOrgId(orgId)).thenReturn(List.of(m1, m2));

        List<Mitgliedschaft> ergebnis = service.findeNachOrg(orgId);

        assertThat(ergebnis).hasSize(2);
    }
}


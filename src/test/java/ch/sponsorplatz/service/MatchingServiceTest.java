package ch.sponsorplatz.service;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;

import ch.sponsorplatz.model.*;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.repository.ProjektRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock private MitgliedschaftRepository mitgliedschaftRepository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private ProjektRepository projektRepository;

    private MatchingService service;

    @BeforeEach
    void setUp() {
        service = new MatchingService(mitgliedschaftRepository, organisationRepository, projektRepository);
    }

    @Test
    @DisplayName("MATCH-01: Findet passende Projekte basierend auf Branche")
    void findeEmpfehlungenMitBrancheMatch() {
        UUID userId = UUID.randomUUID();
        UUID eigeneOrgId = UUID.randomUUID();

        Organisation eigeneOrg = new Organisation();
        eigeneOrg.setId(eigeneOrgId);
        eigeneOrg.setBranche(Branche.SPORT);

        when(mitgliedschaftRepository.findOrgIdsByUserId(userId)).thenReturn(List.of(eigeneOrgId));
        when(organisationRepository.findById(eigeneOrgId)).thenReturn(Optional.of(eigeneOrg));

        Projekt p = new Projekt();
        p.setId(UUID.randomUUID());
        p.setName("Lauftreff Zürich");
        when(projektRepository.findePassende(any(), eq(List.of(eigeneOrgId)), eq(Sichtbarkeit.OEFFENTLICH)))
                .thenReturn(List.of(p));

        List<Projekt> result = service.findeEmpfehlungen(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Lauftreff Zürich");
    }

    @Test
    @DisplayName("MATCH-02: Leere Liste wenn User keine Mitgliedschaften hat")
    void leereListeOhneMitgliedschaften() {
        UUID userId = UUID.randomUUID();
        when(mitgliedschaftRepository.findOrgIdsByUserId(userId)).thenReturn(Collections.emptyList());

        List<Projekt> result = service.findeEmpfehlungen(userId);

        assertThat(result).isEmpty();
        verify(projektRepository, never()).findePassende(any(), any(), any());
    }

    @Test
    @DisplayName("MATCH-03: Leere Liste wenn Org keine Branche hat")
    void leereListeOhneBranche() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Organisation org = new Organisation();
        org.setId(orgId);
        org.setBranche(null);

        when(mitgliedschaftRepository.findOrgIdsByUserId(userId)).thenReturn(List.of(orgId));
        when(organisationRepository.findById(orgId)).thenReturn(Optional.of(org));

        List<Projekt> result = service.findeEmpfehlungen(userId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("MATCH-04: Maximal 6 Empfehlungen")
    void maximalSechs() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Organisation org = new Organisation();
        org.setId(orgId);
        org.setBranche(Branche.BEWEGUNG);

        when(mitgliedschaftRepository.findOrgIdsByUserId(userId)).thenReturn(List.of(orgId));
        when(organisationRepository.findById(orgId)).thenReturn(Optional.of(org));

        // 10 Projekte zurückgeben
        List<Projekt> viele = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Projekt p = new Projekt();
            p.setId(UUID.randomUUID());
            p.setName("Projekt " + i);
            viele.add(p);
        }
        when(projektRepository.findePassende(any(), any(), any())).thenReturn(viele);

        List<Projekt> result = service.findeEmpfehlungen(userId);

        assertThat(result).hasSize(6);
    }
}


package ch.sponsorplatz.organisation;

import ch.sponsorplatz.shared.util.SlugGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgHierarchieServiceTest {

    @Mock private OrganisationRepository repository;
    @Mock private SlugGenerator slugGenerator;

    private OrgHierarchieService service;

    @BeforeEach
    void setUp() {
        service = new OrgHierarchieService(repository, slugGenerator);
    }

    @Test
    @DisplayName("HIER-01: Unterorganisation erfolgreich erstellen + Status- und Branche-Vererbung")
    void erstelleUnterorgErfolgreich() {
        Organisation eltern = erstelleOrg(OrgTyp.UNTERNEHMEN, null);
        eltern.setStatus(OrgStatus.VERIFIED);
        eltern.setBranche(Branche.PRAEVENTION);
        when(repository.findById(eltern.getId())).thenReturn(Optional.of(eltern));
        when(slugGenerator.findeFreienSlug(any(), any())).thenReturn("marketing-abteilung");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Organisation sub = service.erstelleUnterorganisation(
                eltern.getId(), "Marketing-Abteilung", Branche.SPORT, "Beschreibung");

        assertThat(sub.getName()).isEqualTo("Marketing-Abteilung");
        assertThat(sub.getUebergeordneteOrg()).isEqualTo(eltern);
        assertThat(sub.getTyp()).isEqualTo(OrgTyp.UNTERNEHMEN);
        // explizit gesetzte Branche schlägt Eltern-Default
        assertThat(sub.getBranche()).isEqualTo(Branche.SPORT);
        // Status-Vererbung
        assertThat(sub.getStatus()).isEqualTo(OrgStatus.VERIFIED);
    }

    @Test
    @DisplayName("HIER-02: Nur UNTERNEHMEN dürfen Sub-Orgs haben")
    void nurUnternehmenDarfSubOrgsHaben() {
        Organisation verein = erstelleOrg(OrgTyp.VEREIN, null);
        when(repository.findById(verein.getId())).thenReturn(Optional.of(verein));

        assertThatThrownBy(() ->
                service.erstelleUnterorganisation(verein.getId(), "Sub", Branche.SPORT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unternehmen");
    }

    @Test
    @DisplayName("HIER-03: Max 3 Stufen Tiefe")
    void maxTiefeWirdGeprüft() {
        Organisation root = erstelleOrg(OrgTyp.UNTERNEHMEN, null);
        Organisation stufe2 = erstelleOrg(OrgTyp.UNTERNEHMEN, root);
        Organisation stufe3 = erstelleOrg(OrgTyp.UNTERNEHMEN, stufe2);

        when(repository.findById(stufe3.getId())).thenReturn(Optional.of(stufe3));

        assertThatThrownBy(() ->
                service.erstelleUnterorganisation(stufe3.getId(), "Zu tief", Branche.SPORT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tiefe");
    }

    @Test
    @DisplayName("HIER-03b: Eltern auf Tiefe 2 → Erstellung erlaubt (ergibt Tiefe 3)")
    void tiefe2ElternErlaubt() {
        Organisation root = erstelleOrg(OrgTyp.UNTERNEHMEN, null);
        Organisation stufe2 = erstelleOrg(OrgTyp.UNTERNEHMEN, root);
        when(repository.findById(stufe2.getId())).thenReturn(Optional.of(stufe2));
        when(slugGenerator.findeFreienSlug(any(), any())).thenReturn("abteilung");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Organisation sub = service.erstelleUnterorganisation(
                stufe2.getId(), "Abteilung", Branche.SPORT, null);

        assertThat(sub.getUebergeordneteOrg()).isEqualTo(stufe2);
    }

    @Test
    @DisplayName("HIER-07: Branche fehlt (null) → erbt von Eltern")
    void brancheVerertWennNull() {
        Organisation eltern = erstelleOrg(OrgTyp.UNTERNEHMEN, null);
        eltern.setBranche(Branche.MENTAL_HEALTH);
        when(repository.findById(eltern.getId())).thenReturn(Optional.of(eltern));
        when(slugGenerator.findeFreienSlug(any(), any())).thenReturn("tochter");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Organisation sub = service.erstelleUnterorganisation(
                eltern.getId(), "Tochter", null, null);

        assertThat(sub.getBranche()).isEqualTo(Branche.MENTAL_HEALTH);
    }

    @Test
    @DisplayName("HIER-08: Root-Org hat 1-elementige Elternkette (sich selbst)")
    void elternketteRootEinElement() {
        Organisation root = erstelleOrgMitName(OrgTyp.UNTERNEHMEN, null, "Konzern", "konzern");

        List<OrgHierarchieService.BrotkrumenEintrag> kette = service.findeElternkette(root);

        assertThat(kette).hasSize(1);
        assertThat(kette.get(0).name()).isEqualTo("Konzern");
        assertThat(kette.get(0).slug()).isEqualTo("konzern");
    }

    @Test
    @DisplayName("HIER-04: Elternkette wird korrekt ermittelt")
    void elternketteKorrekt() {
        Organisation root = erstelleOrgMitName(OrgTyp.UNTERNEHMEN, null, "Konzern AG", "konzern-ag");
        Organisation tochter = erstelleOrgMitName(OrgTyp.UNTERNEHMEN, root, "Tochter GmbH", "tochter-gmbh");
        Organisation abteilung = erstelleOrgMitName(OrgTyp.UNTERNEHMEN, tochter, "Marketing", "marketing");

        List<OrgHierarchieService.BrotkrumenEintrag> kette = service.findeElternkette(abteilung);

        assertThat(kette).hasSize(3);
        assertThat(kette.get(0).name()).isEqualTo("Konzern AG");
        assertThat(kette.get(1).name()).isEqualTo("Tochter GmbH");
        assertThat(kette.get(2).name()).isEqualTo("Marketing");
    }

    @Test
    @DisplayName("HIER-05: Tiefe wird korrekt berechnet")
    void tiefeBerechnung() {
        Organisation root = erstelleOrg(OrgTyp.UNTERNEHMEN, null);
        Organisation stufe2 = erstelleOrg(OrgTyp.UNTERNEHMEN, root);
        Organisation stufe3 = erstelleOrg(OrgTyp.UNTERNEHMEN, stufe2);

        assertThat(service.berechneTiefe(root)).isEqualTo(1);
        assertThat(service.berechneTiefe(stufe2)).isEqualTo(2);
        assertThat(service.berechneTiefe(stufe3)).isEqualTo(3);
    }

    private Organisation erstelleOrg(OrgTyp typ, Organisation eltern) {
        return erstelleOrgMitName(typ, eltern, "Org-" + UUID.randomUUID(), "slug-" + UUID.randomUUID());
    }

    private Organisation erstelleOrgMitName(OrgTyp typ, Organisation eltern, String name, String slug) {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setTyp(typ);
        org.setName(name);
        org.setSlug(slug);
        org.setBranche(Branche.SPORT);
        org.setUebergeordneteOrg(eltern);
        return org;
    }
}



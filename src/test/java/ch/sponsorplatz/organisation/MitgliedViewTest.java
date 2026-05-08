package ch.sponsorplatz.organisation;

import ch.sponsorplatz.benutzer.AppUser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MitgliedViewTest {

    /** VIEW-04: Mapping flacht user.anzeigename / user.email ein. */
    @Test
    void mappingFlachtUserDatenEin() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("max@example.com");
        user.setAnzeigename("Max Muster");
        user.setPasswortHash("super-geheim-hash");

        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());

        UUID mitgliedschaftId = UUID.randomUUID();

        Mitgliedschaft m = new Mitgliedschaft();
        m.setId(mitgliedschaftId);
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(Rolle.ORG_EDITOR);

        MitgliedView view = MitgliedView.von(m);

        assertThat(view.id()).isEqualTo(mitgliedschaftId);
        assertThat(view.rolle()).isEqualTo(Rolle.ORG_EDITOR);
        assertThat(view.beigetretenAm()).isEqualTo(m.getBeigetretenAm());
        assertThat(view.userAnzeigename()).isEqualTo("Max Muster");
        assertThat(view.userEmail()).isEqualTo("max@example.com");
        assertThat(view.userProfilbildUrl()).isNull();
        // Defense: PasswortHash darf gar nicht ankommen — kein Feld dafür
    }

    /** VIEW-08: Mit Profilbild rendert userProfilbildUrl als /medien/{id}. */
    @Test
    void mappingErzeugtProfilbildUrl() {
        UUID bildId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("max@example.com");
        user.setAnzeigename("Max Muster");
        user.setProfilbildId(bildId);

        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());

        Mitgliedschaft m = new Mitgliedschaft();
        m.setId(UUID.randomUUID());
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(Rolle.ORG_EDITOR);

        MitgliedView view = MitgliedView.von(m);

        assertThat(view.userProfilbildUrl()).isEqualTo("/medien/" + bildId);
    }
}

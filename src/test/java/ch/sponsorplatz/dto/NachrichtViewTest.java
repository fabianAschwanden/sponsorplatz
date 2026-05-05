package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Nachricht;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * View-Mapping-Test für NachrichtView (MSG-09).
 */
class NachrichtViewTest {

    @Test
    @DisplayName("MSG-09: View bildet Absender-Name korrekt ab, kein passwortHash")
    void vonMappingKorrekt() {
        AppUser absender = new AppUser();
        absender.setId(UUID.randomUUID());
        absender.setAnzeigename("Max Muster");
        absender.setPasswortHash("$2a$10$geheim");

        Nachricht nachricht = new Nachricht();
        nachricht.setId(UUID.randomUUID());
        nachricht.setAbsender(absender);
        nachricht.setText("Hallo zusammen!");

        NachrichtView view = NachrichtView.von(nachricht);

        assertThat(view.absenderName()).isEqualTo("Max Muster");
        assertThat(view.absenderId()).isEqualTo(absender.getId());
        assertThat(view.text()).isEqualTo("Hallo zusammen!");
        // Kein Feld für passwortHash im Record
        assertThat(view.toString()).doesNotContain("$2a$10$geheim");
    }

    @Test
    @DisplayName("MSG-09b: von(List) mappt korrekt")
    void vonListeKorrekt() {
        AppUser absender = new AppUser();
        absender.setId(UUID.randomUUID());
        absender.setAnzeigename("Tester");

        Nachricht n1 = new Nachricht();
        n1.setId(UUID.randomUUID());
        n1.setAbsender(absender);
        n1.setText("Eins");

        Nachricht n2 = new Nachricht();
        n2.setId(UUID.randomUUID());
        n2.setAbsender(absender);
        n2.setText("Zwei");

        List<NachrichtView> views = NachrichtView.von(List.of(n1, n2));
        assertThat(views).hasSize(2);
        assertThat(views.get(0).text()).isEqualTo("Eins");
        assertThat(views.get(1).text()).isEqualTo("Zwei");
    }
}


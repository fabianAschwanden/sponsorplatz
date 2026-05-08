package ch.sponsorplatz.organisation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * I18N-05/06/07: Branche-Anzeigenamen aus messages_xx_CH.properties und messages_en.properties.
 */
@SpringBootTest
@ActiveProfiles("dev")
class BrancheI18nTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    @DisplayName("I18N-05: Branche.SPORT FR = 'Sport'")
    void brancheSportFr() {
        String anzeige = messageSource.getMessage(
                "branche.SPORT", null, Locale.forLanguageTag("fr-CH"));
        assertThat(anzeige).isEqualTo("Sport");
    }

    @Test
    @DisplayName("I18N-06: Branche.MENTAL_HEALTH IT = 'Salute mentale'")
    void brancheMentalHealthIt() {
        String anzeige = messageSource.getMessage(
                "branche.MENTAL_HEALTH", null, Locale.forLanguageTag("it-CH"));
        assertThat(anzeige).isEqualTo("Salute mentale");
    }

    @Test
    @DisplayName("I18N-05b: Branche.REHA FR = 'Réhabilitation'")
    void brancheRehaFr() {
        String anzeige = messageSource.getMessage(
                "branche.REHA", null, Locale.forLanguageTag("fr-CH"));
        assertThat(anzeige).isEqualTo("Réhabilitation");
    }

    @Test
    @DisplayName("I18N-06b: Branche.ERNAEHRUNG IT = 'Alimentazione'")
    void brancheErnaehrungIt() {
        String anzeige = messageSource.getMessage(
                "branche.ERNAEHRUNG", null, Locale.forLanguageTag("it-CH"));
        assertThat(anzeige).isEqualTo("Alimentazione");
    }

    @Test
    @DisplayName("I18N-07: Branche.SPORT EN = 'Sports'")
    void brancheSportEn() {
        String anzeige = messageSource.getMessage(
                "branche.SPORT", null, Locale.ENGLISH);
        assertThat(anzeige).isEqualTo("Sports");
    }

    @Test
    @DisplayName("I18N-07b: Branche.MENTAL_HEALTH EN = 'Mental Health'")
    void brancheMentalHealthEn() {
        String anzeige = messageSource.getMessage(
                "branche.MENTAL_HEALTH", null, Locale.ENGLISH);
        assertThat(anzeige).isEqualTo("Mental Health");
    }
}


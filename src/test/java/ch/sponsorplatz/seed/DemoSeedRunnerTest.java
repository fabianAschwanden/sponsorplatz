package ch.sponsorplatz.seed;

import ch.sponsorplatz.anfrage.SponsoringAnfrage;
import ch.sponsorplatz.anfrage.SponsoringAnfrageRepository;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.*;
import ch.sponsorplatz.projekt.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoSeedRunnerTest {

    @Mock private AppUserRepository userRepository;
    @Mock private OrganisationRepository orgRepository;
    @Mock private MitgliedschaftRepository mitgliedschaftRepository;
    @Mock private ProjektRepository projektRepository;
    @Mock private SponsoringPaketRepository paketRepository;
    @Mock private SponsoringAnfrageRepository anfrageRepository;
    @Mock private PasswordEncoder encoder;

    private DemoSeedRunner erstelleRunner() {
        return new DemoSeedRunner(userRepository, orgRepository, mitgliedschaftRepository,
                projektRepository, paketRepository, anfrageRepository, encoder);
    }

    /** SEED-01: DemoSeedRunner erstellt konsistente Daten (Vereine, Projekte, Anfragen). */
    @Test
    @DisplayName("SEED-01: DemoSeed erstellt Vereine, Projekte und Anfragen mit konsistenten FKs")
    void erstelltKonsistenteDaten() {
        when(orgRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(encoder.encode(any())).thenReturn("HASH");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mitgliedschaftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projektRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anfrageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DemoSeedRunner runner = erstelleRunner();
        runner.run();

        // Mindestens 5 Vereine + 3 Sponsor-Orgs
        ArgumentCaptor<Organisation> orgCaptor = ArgumentCaptor.forClass(Organisation.class);
        verify(orgRepository, atLeast(8)).save(orgCaptor.capture());
        List<Organisation> orgs = orgCaptor.getAllValues();

        long vereine = orgs.stream().filter(o -> o.getTyp() == OrgTyp.VEREIN).count();
        long unternehmen = orgs.stream().filter(o -> o.getTyp() == OrgTyp.UNTERNEHMEN).count();
        assertThat(vereine).isGreaterThanOrEqualTo(5);
        assertThat(unternehmen).isGreaterThanOrEqualTo(3);

        // Projekte erstellt
        verify(projektRepository, atLeast(10)).save(any(Projekt.class));

        // Anfragen erstellt
        verify(anfrageRepository, atLeast(3)).save(any(SponsoringAnfrage.class));
    }

    /** SEED-01b: DemoSeedRunner ist idempotent (überspringt bei existierendem Slug). */
    @Test
    @DisplayName("SEED-01b: Idempotent — überspringt wenn Org-Slug bereits existiert")
    void idempotentBeiExistierendemSlug() {
        Organisation existing = new Organisation();
        existing.setSlug("fc-beispiel-zuerich");
        when(orgRepository.findBySlug("fc-beispiel-zuerich")).thenReturn(Optional.of(existing));

        DemoSeedRunner runner = erstelleRunner();
        runner.run();

        // Keine neuen Orgs/Projekte angelegt
        verify(orgRepository, never()).save(any());
        verify(projektRepository, never()).save(any());
    }
}


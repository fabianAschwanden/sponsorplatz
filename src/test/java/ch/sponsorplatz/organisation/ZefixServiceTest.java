package ch.sponsorplatz.organisation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZefixServiceTest {

    /** ZFX-01: Stub gibt immer Optional.empty() zurück. */
    @Test
    void stubGibtImmerLeerZurueck() {
        ZefixService service = new ZefixServiceStub();

        assertThat(service.pruefeOrganisation("FC Beispiel")).isEmpty();
        assertThat(service.pruefeOrganisation("")).isEmpty();
        assertThat(service.pruefeOrganisation(null)).isEmpty();
    }
}


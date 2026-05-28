package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrgTyp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-IDs VIEW-CRM-01..02 — Mapping-Vertrag für SponsorAccountView.
 */
class SponsorAccountViewTest {

    /** VIEW-CRM-01: von(account) flacht Verein ein + mappt alle Felder, ohne Mandanten-Key im Output. */
    @Test
    @DisplayName("VIEW-CRM-01: Mapping flacht Verein ein")
    void mappingEinerAccount() {
        Organisation verein = new Organisation();
        verein.setId(UUID.randomUUID());
        verein.setName("FC Beispiel");
        verein.setSlug("fc-beispiel");
        verein.setTyp(OrgTyp.VEREIN);

        UUID ownerId = UUID.randomUUID();
        Instant erstellt = Instant.now();

        SponsorAccount account = new SponsorAccount();
        account.setId(UUID.randomUUID());
        account.setBesitzerSponsorOrgId(UUID.randomUUID());
        account.setVerein(verein);
        account.setAccountOwnerUserId(ownerId);
        account.setStatus(AccountStatus.AKTIV);
        account.setTier(AccountTier.CORE);
        account.setNotiz("Wichtiger Account");
        account.setErstelltAm(erstellt);

        SponsorAccountView view = SponsorAccountView.von(account);

        assertThat(view.id()).isEqualTo(account.getId());
        assertThat(view.vereinOrgId()).isEqualTo(verein.getId());
        assertThat(view.vereinName()).isEqualTo("FC Beispiel");
        assertThat(view.vereinSlug()).isEqualTo("fc-beispiel");
        assertThat(view.accountOwnerUserId()).isEqualTo(ownerId);
        assertThat(view.status()).isEqualTo(AccountStatus.AKTIV);
        assertThat(view.tier()).isEqualTo(AccountTier.CORE);
        assertThat(view.notiz()).isEqualTo("Wichtiger Account");
        assertThat(view.erstelltAm()).isEqualTo(erstellt);
        // Mandanten-Schlüssel ist KEIN Output-Feld — Defense in depth
        assertThat(view.toString()).doesNotContain(account.getBesitzerSponsorOrgId().toString());
    }

    /** VIEW-CRM-02: tier darf null sein (nicht eingestuft). */
    @Test
    @DisplayName("VIEW-CRM-02: tier=null bleibt null")
    void mappingOhneTier() {
        Organisation verein = new Organisation();
        verein.setId(UUID.randomUUID());
        verein.setName("EHC Test");
        verein.setSlug("ehc-test");
        verein.setTyp(OrgTyp.VEREIN);

        SponsorAccount account = new SponsorAccount();
        account.setId(UUID.randomUUID());
        account.setBesitzerSponsorOrgId(UUID.randomUUID());
        account.setVerein(verein);
        account.setStatus(AccountStatus.LEAD);

        SponsorAccountView view = SponsorAccountView.von(account);

        assertThat(view.tier()).isNull();
        assertThat(view.status()).isEqualTo(AccountStatus.LEAD);
    }
}

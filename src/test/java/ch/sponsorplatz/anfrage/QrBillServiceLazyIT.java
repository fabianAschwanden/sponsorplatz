package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.organisation.OrgTyp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integrationstest für den Lazy-Loading-Pfad von {@link QrBillService}.
 *
 * <p>Regression BETA-V09: {@code erzeugeAlsDataUrlFuerId} lädt die Rechnung per
 * Repository und greift dann auf die <strong>LAZY</strong>-Relation
 * {@code Rechnung.org} zu (für Name/Adresse der QR-Bill). Ohne offene Session
 * (prod: {@code spring.jpa.open-in-view=false}) warf das eine
 * {@code LazyInitializationException} → unbehandelter 500 auf Rechnungs-Detail
 * UND Rechnungs-PDF. Lokal lange unentdeckt, weil die Unit-Tests den Service
 * mockten bzw. eine bereits initialisierte Org-Entity reichten.
 *
 * <p>Setup persistiert die volle Kette in eigener Transaktion und committet;
 * der Service-Aufruf läuft danach OHNE Transaktion — die Rechnung wird frisch
 * geladen, {@code org} ist ein detached Lazy-Proxy, exakt wie im Controller.
 *
 * Test-IDs: QRB-LAZY-01.
 */
@SpringBootTest
@ActiveProfiles("dev")
class QrBillServiceLazyIT {

    @Autowired private QrBillService qrBillService;
    @Autowired private RechnungRepository rechnungRepository;
    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private SponsoringAnfrageRepository anfrageRepository;
    @Autowired private VertragRepository vertragRepository;
    @Autowired private TransactionTemplate txTemplate;

    @Test
    @DisplayName("QRB-LAZY-01: erzeugeAlsDataUrlFuerId löst die LAZY-org auf (kein LazyInitializationException)")
    void erzeugtDataUrlMitLazyOrg() {
        UUID rechnungId = txTemplate.execute(status -> persistiereKette());

        // Kein @Transactional hier: die Rechnung wird vom Service frisch geladen,
        // org ist ein detached Lazy-Proxy. Vor dem Fix wirft getOrg() in erzeuge().
        String dataUrl = qrBillService.erzeugeAlsDataUrlFuerId(rechnungId);

        assertThat(dataUrl).startsWith("data:image/png;base64,");
        assertThat(dataUrl.length()).isGreaterThan(2000);
    }

    @Transactional
    UUID persistiereKette() {
        Organisation verein = neueOrg("FC Lazy", OrgTyp.VEREIN);
        verein.setBranche(Branche.SPORT);
        verein.setStrasse("Bahnhofstrasse 1");
        verein.setPostleitzahl("8001");
        verein.setOrt("Zürich");
        verein.setIban("CH4431999123000889012");
        organisationRepository.save(verein);

        // OrgTyp.ANDERE umgeht die XOR-Branche-Pflicht (V25 chk_branche_pro_typ) —
        // der Sponsor erfüllt hier nur die NOT-NULL-FK der Anfrage.
        Organisation sponsor = neueOrg("Lazy Sponsor AG", OrgTyp.ANDERE);
        organisationRepository.save(sponsor);

        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setAnfragenderOrg(sponsor);
        anfrage.setEmpfaengerOrg(verein);
        anfrage.setStatus(AnfrageStatus.ANGENOMMEN);
        anfrage.setBetreff("Lazy-Test");
        anfrage.setNachricht("Test");
        anfrage.setKontaktName("Max Lazy");
        anfrage.setKontaktEmail("max@lazy.ch");
        anfrageRepository.save(anfrage);

        Vertrag vertrag = new Vertrag();
        vertrag.setAnfrage(anfrage);
        vertrag.setStatus(VertragsStatus.UNTERZEICHNET);
        vertrag.setOrg(verein);
        vertrag.setOrgName(verein.getName());
        vertrag.setSponsorName("Max Lazy");
        vertrag.setPaketName("Gold");
        vertrag.setPreisChf(new BigDecimal("1500.00"));
        vertrag.setErstelltAm(Instant.now());
        vertragRepository.save(vertrag);

        Rechnung r = new Rechnung();
        r.setVertrag(vertrag);
        r.setOrg(verein);
        r.setRechnungsnummer("R-2026-09999");
        r.setStatus(RechnungsStatus.OFFEN);
        r.setBetragChf(new BigDecimal("1500.00"));
        r.setIban("CH4431999123000889012");
        r.setQrReferenz("210000000003139471430009017");
        r.setSponsorName("Lazy Sponsor AG");
        r.setZahlungszweck("Sponsoring · R-2026-09999");
        r.setFaelligAm(LocalDate.now().plusDays(30));
        rechnungRepository.save(r);

        return r.getId();
    }

    private static Organisation neueOrg(String name, OrgTyp typ) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID());
        o.setTyp(typ);
        return o;
    }
}

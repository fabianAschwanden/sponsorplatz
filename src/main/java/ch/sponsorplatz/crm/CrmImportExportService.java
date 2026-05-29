package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.util.Csv;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CSV-Import/-Export der CRM-Portfolio-Daten einer Marke (Cluster 4, CRM-Lücke #16).
 * Round-Trip: exportieren → in Excel bearbeiten → re-importieren (Upsert je
 * {@code verein_slug}). Excel-freundlich: Semikolon-Delimiter + UTF-8-BOM.
 *
 * <p>Wie der übrige CRM-Layer (ADR-0011) prüft jede Methode zuerst
 * {@link AccessControl#kannSponsorDatenSehen} — kein Zugriff ohne Mandanten-Recht.
 */
@Service
@Transactional
public class CrmImportExportService {

    static final String HEADER = "verein_slug;verein_name;status;tier;pipeline_stage;forecast_chf;notiz";
    private static final String BOM = "﻿";

    /** Ergebnis eines Imports — Anzahl erstellt/aktualisiert + Fehlermeldungen je Zeile. */
    public record ImportErgebnis(int erstellt, int aktualisiert, List<String> fehler) {
        public int total() {
            return erstellt + aktualisiert;
        }
        public boolean hatFehler() {
            return !fehler.isEmpty();
        }
    }

    private final SponsorAccountRepository accountRepository;
    private final OrganisationRepository organisationRepository;
    private final AccessControl accessControl;

    public CrmImportExportService(SponsorAccountRepository accountRepository,
                                  OrganisationRepository organisationRepository,
                                  AccessControl accessControl) {
        this.accountRepository = accountRepository;
        this.organisationRepository = organisationRepository;
        this.accessControl = accessControl;
    }

    /** Exportiert das Portfolio einer Marke als CSV (Excel-kompatibel, UTF-8-BOM). */
    @Transactional(readOnly = true)
    public byte[] exportiere(UUID sponsorOrgId, Authentication auth) {
        pruefeZugriff(sponsorOrgId, auth);
        StringBuilder sb = new StringBuilder(BOM).append(HEADER).append("\r\n");
        for (SponsorAccount a : accountRepository.findByBesitzerSponsorOrgIdOrderByErstelltAmDesc(sponsorOrgId)) {
            sb.append(Csv.zeile(List.of(
                    a.getVerein().getSlug(),
                    a.getVerein().getName(),
                    a.getStatus() != null ? a.getStatus().name() : "",
                    a.getTier() != null ? a.getTier().name() : "",
                    a.getPipelineStage() != null ? a.getPipelineStage().name() : "",
                    a.getForecastBetragChf() != null ? a.getForecastBetragChf().toPlainString() : "",
                    a.getNotiz() != null ? a.getNotiz() : ""))).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Importiert/aktualisiert Portfolio-Einträge aus CSV. Schlüssel ist
     * {@code verein_slug}; bestehende Accounts werden aktualisiert, fehlende
     * angelegt. Fehlerhafte Zeilen werden übersprungen und im Ergebnis gemeldet
     * (Teilimport), gültige Zeilen werden angewandt.
     */
    public ImportErgebnis importiere(UUID sponsorOrgId, byte[] csv, Authentication auth) {
        pruefeZugriff(sponsorOrgId, auth);

        int erstellt = 0;
        int aktualisiert = 0;
        List<String> fehler = new ArrayList<>();

        String inhalt = new String(csv, StandardCharsets.UTF_8).replace(BOM, "");
        String[] zeilen = inhalt.split("\r\n|\n|\r");

        boolean kopfUebersprungen = false;
        for (int i = 0; i < zeilen.length; i++) {
            String roh = zeilen[i];
            if (roh.isBlank()) continue;
            if (!kopfUebersprungen) {              // erste nicht-leere Zeile = Header
                kopfUebersprungen = true;
                continue;
            }
            int zeilenNr = i + 1;
            try {
                if (verarbeiteZeile(sponsorOrgId, Csv.parse(roh))) {
                    erstellt++;
                } else {
                    aktualisiert++;
                }
            } catch (ImportZeilenFehler f) {
                fehler.add("Zeile " + zeilenNr + ": " + f.getMessage());
            }
        }
        return new ImportErgebnis(erstellt, aktualisiert, fehler);
    }

    /** @return true wenn neu angelegt, false wenn aktualisiert. */
    private boolean verarbeiteZeile(UUID sponsorOrgId, List<String> felder) {
        String slug = feld(felder, 0);
        if (slug.isEmpty()) {
            throw new ImportZeilenFehler("verein_slug fehlt");
        }
        Organisation verein = organisationRepository.findBySlug(slug)
                .orElseThrow(() -> new ImportZeilenFehler("Verein nicht gefunden: " + slug));
        if (verein.getTyp() != OrgTyp.VEREIN) {
            throw new ImportZeilenFehler("Organisation ist kein Verein: " + slug);
        }

        Optional<SponsorAccount> bestehend =
                accountRepository.findByBesitzerSponsorOrgIdAndVereinId(sponsorOrgId, verein.getId());
        SponsorAccount account = bestehend.orElseGet(() -> {
            SponsorAccount neu = new SponsorAccount();
            neu.setBesitzerSponsorOrgId(sponsorOrgId);
            neu.setVerein(verein);
            return neu;
        });

        // NOT-NULL-Felder (status/pipeline): leer → aktuellen Wert behalten (neu = Default LEAD).
        String status = feld(felder, 2);
        if (!status.isEmpty()) account.setStatus(parseStatus(status));
        String pipeline = feld(felder, 4);
        if (!pipeline.isEmpty()) account.setPipelineStage(parsePipeline(pipeline));
        // Nullable-Felder (tier/forecast/notiz): leer → löschen.
        String tier = feld(felder, 3);
        account.setTier(tier.isEmpty() ? null : parseTier(tier));
        account.setForecastBetragChf(parseBetrag(feld(felder, 5)));
        String notiz = feld(felder, 6);
        account.setNotiz(notiz.isEmpty() ? null : notiz);

        accountRepository.save(account);
        return bestehend.isEmpty();
    }

    private static String feld(List<String> felder, int index) {
        return index < felder.size() ? felder.get(index).trim() : "";
    }

    private static AccountStatus parseStatus(String wert) {
        try {
            return AccountStatus.valueOf(wert.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ImportZeilenFehler("Ungültiger Status: " + wert);
        }
    }

    private static AccountTier parseTier(String wert) {
        try {
            return AccountTier.valueOf(wert.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ImportZeilenFehler("Ungültiges Tier: " + wert);
        }
    }

    private static PipelineStage parsePipeline(String wert) {
        try {
            return PipelineStage.valueOf(wert.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ImportZeilenFehler("Ungültige Pipeline-Stufe: " + wert);
        }
    }

    private static BigDecimal parseBetrag(String wert) {
        if (wert.isEmpty()) return null;
        String normalisiert = wert.replace("'", "").replace(" ", "").replace(',', '.');
        try {
            return new BigDecimal(normalisiert);
        } catch (NumberFormatException e) {
            throw new ImportZeilenFehler("Ungültiger Forecast-Betrag: " + wert);
        }
    }

    private void pruefeZugriff(UUID sponsorOrgId, Authentication auth) {
        if (!accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)) {
            throw new AccessDeniedException("Kein Zugriff auf die CRM-Daten dieser Sponsor-Organisation");
        }
    }

    /** Interne Markierung für eine fehlerhafte Import-Zeile (wird gesammelt, nicht propagiert). */
    private static final class ImportZeilenFehler extends RuntimeException {
        ImportZeilenFehler(String message) {
            super(message);
        }
    }
}

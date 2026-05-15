package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Statistik-Service für die Marken-Landing-Page.
 * Liefert Zähler pro Branche und Gesamtzahl aktiver Projekte.
 *
 * <p>Beide Methoden sind {@code @Cacheable} mit TTL 5 min — Marketing-Traffic
 * auf {@code /fuer-marken} würde sonst pro View eine Aggregat-Query treffen.
 * Cache-Region und TTL sind in {@code shared/cache/CacheConfig} definiert.
 */
@Service
public class StatistikService {

    /** Stati, die als „aktive Vereine" zählen — PENDING/SUSPENDED bewusst draussen. */
    private static final Set<OrgStatus> AKTIVE_STATI = Set.of(OrgStatus.VERIFIED, OrgStatus.ACTIVE);

    private final OrganisationRepository orgRepository;
    private final ProjektRepository projektRepository;

    public StatistikService(OrganisationRepository orgRepository, ProjektRepository projektRepository) {
        this.orgRepository = orgRepository;
        this.projektRepository = projektRepository;
    }

    /**
     * Zählt verifizierte/aktive Vereine pro Branche via Aggregat-Query
     * (ein DB-Roundtrip statt findAll + Java-Stream).
     */
    @Cacheable(CacheConfig.STATISTIK_VEREINE_PRO_BRANCHE)
    public Map<Branche, Long> vereineProBranche() {
        Map<Branche, Long> ergebnis = new EnumMap<>(Branche.class);
        List<Object[]> zeilen = orgRepository.zaehleVereineNachBranche(AKTIVE_STATI);
        for (Object[] zeile : zeilen) {
            ergebnis.put((Branche) zeile[0], (Long) zeile[1]);
        }
        return ergebnis;
    }

    /**
     * Anzahl öffentlich sichtbarer Projekte. {@code COUNT(*)} statt Liste laden.
     */
    @Cacheable(CacheConfig.STATISTIK_ANZAHL_PROJEKTE)
    public long anzahlAktiveProjekte() {
        return projektRepository.countBySichtbarkeit(Sichtbarkeit.OEFFENTLICH);
    }
}


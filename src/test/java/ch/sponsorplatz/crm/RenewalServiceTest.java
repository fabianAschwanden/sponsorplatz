package ch.sponsorplatz.crm;

import ch.sponsorplatz.anfrage.Vertrag;
import ch.sponsorplatz.anfrage.VertragRepository;
import ch.sponsorplatz.anfrage.VertragsStatus;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.Organisation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RENEWAL-01..04 — Renewal-Pipeline der CRM-Layer (ADR-0011). Prüft Mapping inkl.
 * {@code tageVerbleibend}/Überfälligkeit, dass der Mandanten-Check ZUERST läuft
 * (vor jedem Repo-Zugriff) und dass nur UNTERZEICHNETE Verträge bis zum
 * 90-Tage-Stichtag abgefragt werden.
 */
@ExtendWith(MockitoExtension.class)
class RenewalServiceTest {

    @Mock private VertragRepository vertragRepository;
    @Mock private AccessControl accessControl;
    @InjectMocks private RenewalService service;

    private final UUID sponsorOrgId = UUID.randomUUID();
    private final Authentication auth = new UsernamePasswordAuthenticationToken("u@s.ch", null, List.of());

    /** RENEWAL-01: Mapping flacht den Verein ein und rechnet tageVerbleibend. */
    @Test
    void mapptUndRechnetVerbleibendeTage() {
        when(accessControl.kannSponsorDatenSehen(eq(sponsorOrgId), any())).thenReturn(true);
        Vertrag bald = vertrag("FC Bald", "fc-bald", "Goldpaket",
                new BigDecimal("5000.00"), LocalDate.now().plusDays(30));
        when(vertragRepository
                .findBySponsorOrgIdAndStatusAndLaufzeitBisNotNullAndLaufzeitBisLessThanEqualOrderByLaufzeitBisAsc(
                        eq(sponsorOrgId), eq(VertragsStatus.UNTERZEICHNET), any()))
                .thenReturn(List.of(bald));

        List<RenewalView> result = service.findeAuslaufende(sponsorOrgId, auth);

        assertThat(result).hasSize(1);
        RenewalView v = result.get(0);
        assertThat(v.vereinName()).isEqualTo("FC Bald");
        assertThat(v.vereinSlug()).isEqualTo("fc-bald");
        assertThat(v.paketName()).isEqualTo("Goldpaket");
        assertThat(v.tageVerbleibend()).isEqualTo(30);
        assertThat(v.istUeberfaellig()).isFalse();
    }

    /** RENEWAL-02: bereits abgelaufener Vertrag → negative Tage + überfällig. */
    @Test
    void ueberfaelligerVertragIstNegativ() {
        when(accessControl.kannSponsorDatenSehen(eq(sponsorOrgId), any())).thenReturn(true);
        Vertrag alt = vertrag("FC Alt", "fc-alt", "Silberpaket",
                new BigDecimal("2000.00"), LocalDate.now().minusDays(5));
        when(vertragRepository
                .findBySponsorOrgIdAndStatusAndLaufzeitBisNotNullAndLaufzeitBisLessThanEqualOrderByLaufzeitBisAsc(
                        eq(sponsorOrgId), eq(VertragsStatus.UNTERZEICHNET), any()))
                .thenReturn(List.of(alt));

        RenewalView v = service.findeAuslaufende(sponsorOrgId, auth).get(0);

        assertThat(v.tageVerbleibend()).isEqualTo(-5);
        assertThat(v.istUeberfaellig()).isTrue();
    }

    /** RENEWAL-03: Stichtag = heute + 90 Tage, Status = UNTERZEICHNET. */
    @Test
    void fragtNur90TageUndUnterzeichnetAb() {
        when(accessControl.kannSponsorDatenSehen(eq(sponsorOrgId), any())).thenReturn(true);
        when(vertragRepository
                .findBySponsorOrgIdAndStatusAndLaufzeitBisNotNullAndLaufzeitBisLessThanEqualOrderByLaufzeitBisAsc(
                        any(), any(), any()))
                .thenReturn(List.of());

        service.findeAuslaufende(sponsorOrgId, auth);

        ArgumentCaptor<LocalDate> stichtag = ArgumentCaptor.forClass(LocalDate.class);
        verify(vertragRepository)
                .findBySponsorOrgIdAndStatusAndLaufzeitBisNotNullAndLaufzeitBisLessThanEqualOrderByLaufzeitBisAsc(
                        eq(sponsorOrgId), eq(VertragsStatus.UNTERZEICHNET), stichtag.capture());
        assertThat(stichtag.getValue()).isEqualTo(LocalDate.now().plusDays(90));
    }

    /** RENEWAL-04: fremder Sponsor → AccessDenied, Repository wird nie berührt. */
    @Test
    void fremderSponsorBekommtAccessDenied() {
        UUID fremd = UUID.randomUUID();
        when(accessControl.kannSponsorDatenSehen(eq(fremd), any())).thenReturn(false);

        assertThatThrownBy(() -> service.findeAuslaufende(fremd, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(vertragRepository, never())
                .findBySponsorOrgIdAndStatusAndLaufzeitBisNotNullAndLaufzeitBisLessThanEqualOrderByLaufzeitBisAsc(
                        any(), any(), any());
    }

    private Vertrag vertrag(String vereinName, String vereinSlug, String paketName,
                            BigDecimal preis, LocalDate laufzeitBis) {
        Organisation verein = new Organisation();
        verein.setId(UUID.randomUUID());
        verein.setName(vereinName);
        verein.setSlug(vereinSlug);

        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setOrg(verein);
        v.setOrgName(vereinName);
        v.setPaketName(paketName);
        v.setPreisChf(preis);
        v.setLaufzeitBis(laufzeitBis);
        v.setStatus(VertragsStatus.UNTERZEICHNET);
        return v;
    }
}

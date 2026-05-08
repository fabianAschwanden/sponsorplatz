package ch.sponsorplatz.anfrage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private LokalerStubProvider stubProvider;

    @Test
    @DisplayName("PAY-06: erstelleZahlung delegiert korrekt an aktiven Provider")
    void delegiertAnProvider() {
        org.mockito.Mockito.when(stubProvider.providerName()).thenReturn("stub");
        org.mockito.Mockito.when(stubProvider.erstelleZahlung(any(), any(), any()))
                .thenReturn(new PaymentProvider.ZahlungsErgebnis("TX-1", PaymentProvider.ZahlungsStatus.BEZAHLT, "ok"));

        PaymentService service = new PaymentService(List.of(stubProvider));
        PaymentProvider.ZahlungsErgebnis ergebnis = service.erstelleZahlung(
                UUID.randomUUID(), new BigDecimal("250.00"), "Paket Gold");

        assertThat(ergebnis.status()).isEqualTo(PaymentProvider.ZahlungsStatus.BEZAHLT);
        verify(stubProvider).erstelleZahlung(any(), eq(new BigDecimal("250.00")), eq("Paket Gold"));
    }
}


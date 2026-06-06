package ch.sponsorplatz.anfrage.payment;
import ch.sponsorplatz.anfrage.Rechnung;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link PaymentTransactionService}.
 * Test-IDs: PAY-TX-01..03
 */
@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

    @Mock
    private PaymentTransactionRepository repository;

    private PaymentTransactionService service;

    @BeforeEach
    void setup() {
        service = new PaymentTransactionService(repository);
    }

    @Test
    @DisplayName("PAY-TX-01: speichere persistiert mit korrekten Feldern")
    void speichere() {
        Rechnung rechnung = new Rechnung();
        rechnung.setId(UUID.randomUUID());

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = service.speichere(
                rechnung, "datatrans", "DT-TX-99",
                BigDecimal.valueOf(250.00), "https://pay.datatrans.com/v1/start/DT-TX-99");

        assertThat(result.getProvider()).isEqualTo("datatrans");
        assertThat(result.getProviderReference()).isEqualTo("DT-TX-99");
        assertThat(result.getBetragChf()).isEqualByComparingTo(BigDecimal.valueOf(250.00));
        assertThat(result.getCheckoutUrl()).contains("DT-TX-99");
        assertThat(result.getStatus()).isEqualTo(PaymentTransactionStatus.ERSTELLT);
        assertThat(result.getErstelltAm()).isNotNull();
    }

    @Test
    @DisplayName("PAY-TX-03: aktualisiereStatus setzt aktualisiert_am")
    void aktualisiereStatus() {
        PaymentTransaction bestehend = new PaymentTransaction();
        bestehend.setId(UUID.randomUUID());
        bestehend.setStatus(PaymentTransactionStatus.ERSTELLT);

        when(repository.findByProviderAndProviderReference("datatrans", "TX-1"))
                .thenReturn(Optional.of(bestehend));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<PaymentTransaction> result = service.aktualisiereStatus(
                "datatrans", "TX-1", PaymentTransactionStatus.BEZAHLT, "{\"status\":\"settled\"}");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PaymentTransactionStatus.BEZAHLT);
        assertThat(result.get().getAktualisiertAm()).isNotNull();
        assertThat(result.get().getRawPayload()).contains("settled");
    }

    @Test
    @DisplayName("PAY-TX-03b: aktualisiereStatus bei unbekannter Referenz → empty")
    void aktualisiereStatusUnbekannt() {
        when(repository.findByProviderAndProviderReference("datatrans", "UNKNOWN"))
                .thenReturn(Optional.empty());

        Optional<PaymentTransaction> result = service.aktualisiereStatus(
                "datatrans", "UNKNOWN", PaymentTransactionStatus.BEZAHLT, null);

        assertThat(result).isEmpty();
    }
}


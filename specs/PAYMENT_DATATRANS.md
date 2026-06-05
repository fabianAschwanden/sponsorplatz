# Datatrans-Integration — Spec

> **Status:** Aktiv (Juni 2026)
> **Phase:** 15.1
> **Bezug:** `SPONSORING_ZAHLUNGSFLUSS.md` §11, Roadmap 9.2

---

## 1. Ziel

Sponsoren können eine offene Rechnung online bezahlen (TWINT, Kreditkarte,
PostFinance Card/E-Finance) via Datatrans Hosted Payment Page (HPP).
Die bestehende Offline-Zahlung per Swiss QR-Bill bleibt parallel bestehen.

## 2. Architektur

```
Sponsor klickt „Online bezahlen"
    → GET /organisationen/{slug}/rechnungen/{id}/checkout
    → PaymentService.erstelleCheckoutSession(rechnung)
    → DatatransProvider.erstelleZahlung(...)
    → POST https://api.datatrans.com/v1/transactions (Init)
    → Redirect auf https://pay.datatrans.com/v1/start/{txId}

Sponsor bezahlt auf Datatrans HPP (TWINT / Visa / MC / PFC)

Datatrans sendet Webhook:
    → POST /payment/webhook/datatrans
    → Signatur (HMAC-SHA256) verifiziert
    → payment_transaction persistiert (Idempotenz via provider_reference UNIQUE)
    → RechnungService.markiereAlsBezahltViaWebhook()
    → Benachrichtigung an Verein-Owner
```

## 3. Konfiguration

```properties
# application-prod.properties
sponsorplatz.payment.datatrans.merchant-id=${DATATRANS_MERCHANT_ID}
sponsorplatz.payment.datatrans.password=${DATATRANS_PASSWORD}
sponsorplatz.payment.datatrans.hmac-hex-key=${DATATRANS_HMAC_HEX_KEY}
sponsorplatz.payment.datatrans.base-url=https://api.datatrans.com
sponsorplatz.payment.datatrans.success-url=https://sponsorplatz.ch/payment/erfolg?ref={rechnungId}
sponsorplatz.payment.datatrans.cancel-url=https://sponsorplatz.ch/payment/abgebrochen?ref={rechnungId}
sponsorplatz.payment.datatrans.error-url=https://sponsorplatz.ch/payment/fehler?ref={rechnungId}
```

## 4. DB-Migration V51

```sql
CREATE TABLE payment_transaction (
    id                    UUID         PRIMARY KEY,
    rechnung_id           UUID         NOT NULL REFERENCES rechnung(id),
    provider              VARCHAR(50)  NOT NULL,
    provider_reference    VARCHAR(255) NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    betrag_chf            NUMERIC(10,2) NOT NULL,
    checkout_url          VARCHAR(2000),
    raw_payload           TEXT,
    erstellt_am           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_am       TIMESTAMP,

    CONSTRAINT chk_pay_tx_status   CHECK (status IN ('ERSTELLT','AUTORISIERT','BEZAHLT','FEHLGESCHLAGEN','STORNIERT')),
    CONSTRAINT chk_pay_tx_provider CHECK (provider IN ('stub','datatrans')),
    CONSTRAINT uq_pay_provider_ref UNIQUE (provider, provider_reference)
);
```

## 5. Zahlungsmethoden

| Methode | Datatrans-Code | Priorität |
|---------|----------------|-----------|
| TWINT | `TWI` | P1 — CH-Standard |
| PostFinance Card | `PFC` | P1 |
| Visa | `VIS` | P1 |
| Mastercard | `ECA` | P1 |
| Apple Pay | `APL` | P2 |

## 6. Sicherheit

- **HMAC-SHA256** auf eingehende Webhooks (Header `Datatrans-Signature`)
- **Idempotenz** via UNIQUE-Constraint auf `(provider, provider_reference)`
- **CSRF-Ausnahme** auf `/payment/webhook/**` (bereits konfiguriert)
- **Kein Card-Data-Handling** — HPP-Modell, PCI-Scope bleibt minimal

## 7. Test-IDs

| ID | Klasse | Beschreibung |
|---|---|---|
| PAY-DT-01 | `DatatransProviderTest` | `erstelleZahlung` sendet korrekten POST, parst Antwort |
| PAY-DT-02 | `DatatransProviderTest` | `verifiziereSignatur` erkennt gültige HMAC |
| PAY-DT-03 | `DatatransProviderTest` | `verifiziereSignatur` lehnt falsche HMAC ab |
| PAY-DT-04 | `DatatransProviderTest` | `bestaetigeZahlung` mappt `settled` → BEZAHLT |
| PAY-DT-05 | `DatatransProviderTest` | `widerrufe` sendet Cancel-Request |
| PAY-TX-01 | `PaymentTransactionServiceTest` | `speichere` persistiert mit korrekten Feldern |
| PAY-TX-02 | `PaymentTransactionServiceTest` | Doppelter `provider_reference` wirft Exception (Idempotenz) |
| PAY-TX-03 | `PaymentTransactionServiceTest` | `aktualisiereStatus` setzt `aktualisiert_am` |
| PAY-CK-01 | `RechnungControllerTest` | GET checkout für OFFEN-Rechnung → 302 redirect |
| PAY-CK-02 | `RechnungControllerTest` | GET checkout für BEZAHLT-Rechnung → Fehler-Flash |

## 8. User-Facing Seiten

- **Rechnungs-Detail** erhält „Online bezahlen"-Button (nur bei Status OFFEN)
- **`/payment/erfolg`** — Danke-Seite (öffentlich, zeigt Bestätigung)
- **`/payment/abgebrochen`** — Hinweis „Zahlung wurde abgebrochen"
- **`/payment/fehler`** — Fehlermeldung mit Kontakt-Link


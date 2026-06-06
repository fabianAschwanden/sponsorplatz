package ch.sponsorplatz.anfrage.payment;

/**
 * Status einer Payment-Transaktion beim Provider.
 */
public enum PaymentTransactionStatus {
    ERSTELLT,
    AUTORISIERT,
    BEZAHLT,
    FEHLGESCHLAGEN,
    STORNIERT
}


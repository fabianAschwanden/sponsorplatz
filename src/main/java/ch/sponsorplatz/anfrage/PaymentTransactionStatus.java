package ch.sponsorplatz.anfrage;

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


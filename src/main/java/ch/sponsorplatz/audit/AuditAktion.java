package ch.sponsorplatz.audit;

/**
 * Mögliche Audit-Aktionen.
 */
public final class AuditAktion {

    public static final String ERSTELLT = "ERSTELLT";
    public static final String AKTUALISIERT = "AKTUALISIERT";
    public static final String GELOESCHT = "GELOESCHT";
    public static final String VERIFIZIERT = "VERIFIZIERT";
    public static final String SUSPENDIERT = "SUSPENDIERT";
    public static final String GESPERRT = "GESPERRT";
    public static final String ENTSPERRT = "ENTSPERRT";
    public static final String ROLLE_GEAENDERT = "ROLLE_GEAENDERT";
    public static final String REGISTRIERT = "REGISTRIERT";
    public static final String LOGIN = "LOGIN";
    public static final String PASSWORT_GEAENDERT = "PASSWORT_GEAENDERT";
    public static final String BACKUP_ERSTELLT = "BACKUP_ERSTELLT";
    public static final String DATEI_BACKUP_ERSTELLT = "DATEI_BACKUP_ERSTELLT";
    public static final String DATEI_BACKUP_RESTORED = "DATEI_BACKUP_RESTORED";

    // 2-Faktor-Authentifizierung (Phase 13.2) — siehe specs/AUTH_2FA_TOTP.md
    public static final String TOTP_AKTIVIERT = "TOTP_AKTIVIERT";
    public static final String TOTP_DEAKTIVIERT = "TOTP_DEAKTIVIERT";
    public static final String TOTP_BACKUP_CODES_NEU = "TOTP_BACKUP_CODES_NEU";
    public static final String TOTP_LOGIN_OK = "TOTP_LOGIN_OK";
    public static final String TOTP_LOGIN_FAIL = "TOTP_LOGIN_FAIL";
    public static final String TOTP_BACKUP_CODE_GENUTZT = "TOTP_BACKUP_CODE_GENUTZT";
    public static final String LOGIN_2FA_LOCKOUT = "LOGIN_2FA_LOCKOUT";
    public static final String EINLADUNG_GESENDET = "EINLADUNG_GESENDET";
    public static final String ANFRAGE_ANGENOMMEN = "ANFRAGE_ANGENOMMEN";
    public static final String ANFRAGE_ABGELEHNT = "ANFRAGE_ABGELEHNT";

    // Vertrag-/Rechnung-Lifecycle (Pflicht laut SPONSORING_ZAHLUNGSFLUSS.md §10)
    public static final String VERTRAG_ERSTELLT = "VERTRAG_ERSTELLT";
    public static final String VERTRAG_UNTERZEICHNET = "VERTRAG_UNTERZEICHNET";
    public static final String VERTRAG_GEKUENDIGT = "VERTRAG_GEKUENDIGT";
    public static final String RECHNUNG_ERSTELLT = "RECHNUNG_ERSTELLT";
    public static final String RECHNUNG_BEZAHLT = "RECHNUNG_BEZAHLT";
    public static final String RECHNUNG_STORNIERT = "RECHNUNG_STORNIERT";
    public static final String RECHNUNG_MAHNUNG_VERSENDET = "RECHNUNG_MAHNUNG_VERSENDET";
    public static final String RECHNUNG_PDF_HERUNTERGELADEN = "RECHNUNG_PDF_HERUNTERGELADEN";

    private AuditAktion() {}
}


package ch.sponsorplatz.model;

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
    public static final String EINLADUNG_GESENDET = "EINLADUNG_GESENDET";
    public static final String ANFRAGE_ANGENOMMEN = "ANFRAGE_ANGENOMMEN";
    public static final String ANFRAGE_ABGELEHNT = "ANFRAGE_ABGELEHNT";

    private AuditAktion() {}
}


/**
 * Sponsorplatz — Structurizr-DSL-Workspace (C4 Model)
 *
 * Single source of truth für die Architektur-Diagramme.
 * CI-Render zu Mermaid + PlantUML siehe `.github/workflows/architektur-diagramme.yml`.
 * Lokales Rendern via Structurizr Lite (Docker) — siehe `docs/architektur/README.md`.
 *
 * Bei Änderungen: Workspace bewusst klein halten. Wenn ein neues Feature-Folder
 * dazukommt, hier als Component ergänzen und die Beziehungen modellieren.
 */

workspace "Sponsorplatz" "Schweizer Sponsoring-Plattform für Sport und Gesundheit" {

    model {

        // -------------------------------------------------------------------
        // Personas (C1)
        // -------------------------------------------------------------------
        vereinMitglied = person "Verein-Mitglied" "Pflegt Vereinsprofil, Projekte, Anfragen" "Verein"
        sponsorUser    = person "Sponsor-User" "Stellt Sponsoring-Anfragen, scannt QR-Bill" "Sponsor"
        cssTeam        = person "CSS Sponsoring-Team" "Kuratiert Vereine, sponsert zentral" "CSS"
        cssAgentur     = person "CSS-Agentur" "Regionale CSS-Vertretung" "CSS"
        versicherte    = person "CSS-Versicherte" "Sieht lokales CSS-Engagement" "Versicherte"
        admin          = person "Plattform-Admin" "Verifiziert Vereine, Backups, Audit"

        // -------------------------------------------------------------------
        // Externe Systeme
        // -------------------------------------------------------------------
        entra      = softwareSystem "Microsoft Entra ID" "OIDC Identity Provider (Phase 1.4)" "External,Backlog"
        datatrans  = softwareSystem "Datatrans" "Online-Payment-Provider (Phase 9.2)" "External,Backlog"
        smtp       = softwareSystem "SMTP-Server" "Mail-Versand (MailHog dev, prod-SMTP)" "External"
        zefix      = softwareSystem "Zefix" "Schweizer Handelsregister (Stub)" "External,Backlog"
        oci        = softwareSystem "OCI Object Storage" "Backup-Ziel + Medien-Storage (cloud-free)" "External"
        azureBlob  = softwareSystem "Azure Blob Storage" "Backup-Ziel + Medien-Storage (cloud-azure, Phase 15.3 Warm-DR)" "External"
        sentry     = softwareSystem "Sentry / GlitchTip" "Error-Tracking mit Umgebungs-Tag (DSG-konform)" "External"

        // -------------------------------------------------------------------
        // Hauptsystem: Sponsorplatz
        // -------------------------------------------------------------------
        sponsorplatz = softwareSystem "Sponsorplatz" "Kuratierte Health-Sponsoring-Plattform — strikt fokussiert, breit gefasst" {

            web = container "Web-Anwendung (cloud-free / OCI)" "Spring Boot 3.5 + Thymeleaf, server-rendered HTML — primary Zone" "Java 21" {

                // Feature-Folder als Komponenten — direkte Spiegelung der Paket-Struktur
                organisation     = component "organisation"      "Vereine, Mitgliedschaften, AccessControl, Branche-Enum"
                projekt          = component "projekt"           "Projekte, Pakete, Marktplatz, Volltextsuche"
                anfrage          = component "anfrage"           "Anfrage → Vertrag → Rechnung → Payment-Provider"
                benutzer         = component "benutzer"          "AppUser, Auth, Passwort-Reset, Profile"
                einladung        = component "einladung"         "Token-basierte Org-Einladungen"
                audit            = component "audit"             "AuditLog, DSG-Datenexport"
                benachrichtigung = component "benachrichtigung"  "In-App-Notifications + E-Mail-Versand"
                adminMod         = component "admin"             "Verifizierungs-Queue, Feature-Backlog, Mail-Einstellungen"
                home             = component "home"              "Public-Pages, Marken-Landing, Statistik"
                dashboard        = component "dashboard"         "Eingeloggter Dashboard-Hub"
                kontakt          = component "kontakt"           "Support / Kontakt-Formular"
                aufgabe          = component "aufgabe"           "Workflow-Aufgaben / Tasks"
                backup           = component "backup"            "Automatische DB-Backups"
                ops              = component "ops"               "Ops-Dashboard, Monitoring-Endpoints"
                seed             = component "seed"              "Demo- und Dev-Seed-Daten"
                shared           = component "shared"            "Querschnitt: Config, Util, Exception, PDF, Mail, Storage" "Querschnitt"
            }

            db = container "PostgreSQL 17 (Docker, OCI-VM)" "Persistenz aller Sponsorplatz-Daten — primary Zone" "Database" "Database"

            // ---------------------------------------------------------------
            // Phase 15.3 Multi-Cloud — Azure-Zone als Warm-DR
            // ---------------------------------------------------------------
            webAzure = container "Web-Anwendung (cloud-azure / DR)" "Identisches Image, eigene VM in Azure Sweden Central — Warm-DR-Zone" "Java 21"
            dbAzure  = container "PostgreSQL 17 (Azure Flex)" "Azure Database for PostgreSQL Flexible Server, VNet-privat — DR-Zone" "Database" "Database"
        }

        // -------------------------------------------------------------------
        // Beziehungen — System-Ebene
        // -------------------------------------------------------------------
        vereinMitglied -> sponsorplatz "Pflegt Profil, antwortet Anfragen, erstellt Rechnungen"
        sponsorUser    -> sponsorplatz "Stellt Anfragen, scannt QR-Bill"
        cssTeam        -> sponsorplatz "Kuratiert Vereine, sponsert via Anfragen"
        cssAgentur     -> sponsorplatz "Regionale Sponsoring-Anfragen, Vor-Ort-Engagement"
        versicherte    -> sponsorplatz "Sieht lokale CSS-Engagements (öffentlich)"
        admin          -> sponsorplatz "Verifiziert, moderiert, exportiert"

        sponsorplatz -> entra     "SSO-Authentifizierung (OIDC)" "OAuth2"
        sponsorplatz -> datatrans "Online-Zahlungen + Webhook" "REST"
        sponsorplatz -> smtp      "Mail-Versand" "SMTP/STARTTLS"
        sponsorplatz -> zefix     "Verein-/Sponsor-Verifikation" "REST"
        sponsorplatz -> oci       "Backup-Spiegelung + Medien-Storage (cloud-free)" "OCI Object Storage SDK"
        sponsorplatz -> azureBlob "Backup-Spiegelung + Medien-Storage (cloud-azure)" "Azure Storage Blob SDK"
        sponsorplatz -> sentry    "Error-Events + sponsorplatz.umgebung-Tag" "HTTPS"

        // -------------------------------------------------------------------
        // Beziehungen — Container-Ebene
        // -------------------------------------------------------------------
        // OCI-Zone (primary)
        web      -> db        "JPA/Hibernate + Flyway" "JDBC"
        web      -> entra     "OIDC Authorization-Code-Flow mit PKCE"
        web      -> datatrans "Zahlung initiieren + Webhook empfangen"
        web      -> smtp      "Spring-Mail JavaMailSender"
        web      -> zefix     "ZefixService (heute Stub, Phase X echte API)"
        web      -> oci       "BackupService + StorageService — Instance Principal Auth"
        web      -> sentry    "BeforeSend-Filter (DSG) + Tag sponsorplatz.umgebung=oci-staging-free"

        // Azure-Zone (warm-DR, Phase 15.3)
        webAzure -> dbAzure   "JPA/Hibernate + Flyway, VNet-private Connection" "JDBC + SSL"
        webAzure -> azureBlob "BackupService + StorageService — UAMI Managed Identity"
        webAzure -> smtp      "Spring-Mail JavaMailSender (gleicher Relay wie OCI)"
        webAzure -> sentry    "BeforeSend-Filter (DSG) + Tag sponsorplatz.umgebung=azure-staging"

        // Manueller Restore-Pfad heute (Slice 5/6 automatisiert das später)
        web -> webAzure "Manueller pg_dump + ZIP-Restore via /admin/backups + /admin/datei-backups" "Operator"

        // -------------------------------------------------------------------
        // Beziehungen — Komponenten-Ebene
        // (Feature-Folder dürfen nicht im Kreis — siehe ARCH-06 in ArchUnit)
        // -------------------------------------------------------------------
        organisation     -> shared           "Util, Exception, AccessControl"
        projekt          -> shared           "PDF, Util"
        projekt          -> organisation     "Org-Lookup, Branche-Filter"
        anfrage          -> shared           "PDF, Util, Storage"
        anfrage          -> organisation     "Org + Mitgliedschaft + AccessControl"
        anfrage          -> projekt          "Paket-Snapshot, Projekt-Ref"
        anfrage          -> benachrichtigung "Mail bei Status-Übergang"
        anfrage          -> audit            "AuditLog Status-Events"
        benutzer         -> shared           "Mail, Util, Token-Gen"
        benutzer         -> benachrichtigung "Reset-Mail, Verifikations-Mail"
        einladung        -> shared           "Token-Gen, Mail"
        einladung        -> benutzer         "User-Lookup"
        einladung        -> organisation     "Mitgliedschaft anlegen"
        audit            -> shared           "Async-Eventing"
        benachrichtigung -> shared           "Mail-Versand"
        benachrichtigung -> benutzer         "Empfänger-Lookup"
        adminMod         -> organisation     "Verifizierungs-Queue"
        adminMod         -> audit            "Admin-Aktionen loggen"
        adminMod         -> backup           "Backup-UI"
        home             -> organisation     "Statistik vereineProBranche"
        home             -> projekt          "Statistik anzahlAktiveProjekte"
        dashboard        -> organisation     "Eigene Orgs"
        dashboard        -> projekt          "Eigene Projekte"
        dashboard        -> anfrage          "Eigene Anfragen"
        dashboard        -> benachrichtigung "Glocke + Liste"
        kontakt          -> shared           "Mail-Versand"
        aufgabe          -> shared
        aufgabe          -> benutzer         "Aufgaben-Empfänger"
        backup           -> shared
        ops              -> shared
        seed             -> organisation     "Demo-Vereine"
        seed             -> projekt          "Demo-Projekte"
        seed             -> benutzer         "Demo-User"
    }

    // -------------------------------------------------------------------
    // Views — C4-Diagramme
    // -------------------------------------------------------------------
    views {

        systemContext sponsorplatz "C1_SystemContext" {
            include *
            autoLayout lr
            description "C1 — Sponsorplatz im Kontext seiner Nutzer und externen Systeme."
        }

        container sponsorplatz "C2_Container" {
            include *
            autoLayout lr
            description "C2 — Container der Sponsorplatz-Plattform (Web + DB)."
        }

        component web "C3_Komponenten_Web" {
            include *
            autoLayout
            description "C3 — Feature-Folder als Komponenten der Web-Anwendung. Spiegelt direkt die Paket-Struktur. Die Querschnitts-Komponente `shared` ist von allen erreichbar, importiert aber selbst keine Feature-Folder (ARCH-07)."
        }

        // -------------------------------------------------------------------
        // Styling — CSS-Brand + Visual Markers
        // -------------------------------------------------------------------
        styles {
            element "Person" {
                shape Person
                background #00327D
                color #FFFFFF
            }
            element "Verein" {
                background #1E3A5F
                color #FFFFFF
            }
            element "CSS" {
                background #00327D
                color #FFFFFF
            }
            element "Versicherte" {
                background #00A4E0
                color #FFFFFF
            }
            element "Sponsor" {
                background #E07A5F
                color #FFFFFF
            }
            element "Software System" {
                background #1168BD
                color #FFFFFF
            }
            element "External" {
                background #999999
                color #FFFFFF
            }
            element "Backlog" {
                opacity 60
                border dashed
            }
            element "Container" {
                background #438DD5
                color #FFFFFF
            }
            element "Database" {
                shape Cylinder
                background #2B3674
                color #FFFFFF
            }
            element "Component" {
                background #85BBF0
                color #000000
            }
            element "Querschnitt" {
                background #F4F1ED
                color #5A5A5A
            }
        }

        theme default
    }
}

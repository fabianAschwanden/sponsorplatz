# ADR-0012: Ports-&-Adapter für externe Integrationen

## Status
Akzeptiert

## Datum
2026-06-06

## Kontext

Sponsorplatz spricht mit mehreren **externen Systemen**: SMTP-Mailversand,
Zahlungs-Provider (Datatrans), Objekt-Storage (OCI / Azure Blob / lokal),
Backup-Cloud-Upload und OIDC-Identity-Provider (Entra/Google/SwissID). Diese
Integrationen sind volatil — Anbieter, SDKs und Konfigurationen ändern sich
unabhängig vom Domänen-Kern.

Einige Integrationen waren bereits sauber abstrahiert (`StorageService`,
`BackupCloudUploader`, `PaymentProvider` als Interfaces), andere nicht: Der
Mailversand lag als konkrete `@Service`-Klasse `MailService` vor, deren
HTML-Methode einen `Consumer<MimeMessageHelper>` an **alle Aufrufer** leakte —
also ein Spring-/Jakarta-Mail-Framework-Typ mitten in der Domänenlogik
(`VerifikationsService`, `RechnungsMailService`, …).

Probleme dieser uneinheitlichen Lage:

- **Framework-Leak:** Domänen-/Anwendungsdienste hingen an Transport-Details
  (`MimeMessageHelper`), nicht an einer fachlichen Schnittstelle.
- **Schlechte Testbarkeit:** Mocks mussten Framework-Typen nachbilden.
- **Kein Schutz gegen Erosion:** Nichts verhinderte, dass ein neuer Service
  direkt `JavaMailSender` oder ein Provider-SDK importiert.
- **Inkonsistenz:** Mal Port+Adapter, mal direkte Kopplung — kein erkennbares Muster.

## Entscheidung

Wir führen **Ports-&-Adapter (Hexagonal Architecture)** als verbindliches
Muster für **alle externen Integrationen** ein.

**Begriffe:**

- **Port** — ein framework-freies Interface (+ ggf. Value-Objects), das die
  fachliche Fähigkeit beschreibt („Mail versenden", „Zahlung anstossen"). Liegt
  im fachlich passenden Package, kennt **keine** Transport-/SDK-Typen.
- **Adapter** — die technologie-spezifische Implementierung eines Ports. Liegt
  in einem **eigenen Sub-Package** und ist der **einzige** Ort, der die
  Framework-/SDK-Typen kennen darf.

**Package-Konvention:**

```
<context>/<faehigkeit>/            ← Port (framework-frei)
    XxxPort.java                   ← Interface
    XxxWert.java                   ← Value-Objects (framework-frei)
<context>/<faehigkeit>/<tech>/     ← Adapter (technologie-spezifisch)
    TechXxx.java                   ← implements XxxPort
```

Beispiel Mail (Referenz-Umsetzung dieses ADR):

```
shared/mail/         → MailVersand (Port), MailAnhang (Value-Object)
shared/mail/smtp/    → SmtpMailVersand (Adapter, kennt JavaMailSender)
```

**Aufrufer hängen ausschliesslich am Port.** Spring injiziert den einzigen
Adapter automatisch (Component-Scan greift in Sub-Packages). Mehrere Adapter
(z.B. Storage Lokal/OCI/Azure) werden über `@Profile`/`@ConditionalOnProperty`
selektiert.

**Durchsetzung via ArchUnit** (Schicht 1, ADR-0007): Pro Integration eine
`noClasses().that().resideOutsideOfPackage("…<tech>..").should()
.dependOnClassesThat().resideInAnyPackage("<framework>..")`-Regel. Damit darf
nur das Adapter-Package die jeweiligen Framework-/SDK-Typen kennen — selbst das
Port-Package bleibt sauber.

**Inkrementelle Einführung** (ein Port pro Commit, niedriges Regressionsrisiko):

| Integration | Port | Adapter-Package | ArchUnit | Status |
|---|---|---|---|---|
| Mail | `MailVersand` | `shared.mail.smtp` | ARCH-20 | ✅ umgesetzt |
| Payment | `PaymentProvider` | `anfrage.payment.datatrans` / `…​.stub` | ARCH-21 | ✅ umgesetzt |
| Storage | `StorageService` | `shared.storage.lokal` / `.oci` / `.azure` | ARCH-22 | ✅ umgesetzt |
| Backup-Cloud | `BackupCloudUploader` | `backup.cloud.oci` / `.azure` | (siehe Hinweis) | ✅ umgesetzt |
| OIDC/IdP | — | `benutzer` | — geplant | offen (tief in Spring Security) |

> **Hinweis OCI-SDK / Backup-Cloud-Guard:** Für Azure ist der SDK-Guard präzise
> (`com.azure` nur im Azure-Adapter, ARCH-22). Eine analoge OCI-Regel wäre falsch —
> `com.oracle.bmc` wird bewusst von drei getrennten Integrationen genutzt
> (Storage-Adapter, Backup-Cloud-Upload, Ops-Bucket-Stats). Darum hat Backup-Cloud
> **keinen** eigenen SDK-Guard: der OCI-Uploader kann nicht eingesperrt werden,
> der Azure-Uploader nutzt ohnehin nur das SDK-freie `AzureBlobOperations`-Seam
> (also bereits durch ARCH-22 abgedeckt). Der Mehrwert der Backup-Cloud-Trennung
> ist die Package-Topologie, nicht ein neuer Wächter. Port-Disziplin ≠ blinde Symmetrie.

## Konsequenzen

**Positiv:**

- Domänen-/Anwendungsdienste sind framework-frei und ohne SDK-Mocks testbar.
- Anbieter-/SDK-Wechsel bleibt auf das Adapter-Package begrenzt.
- ArchUnit verhindert erneutes Einlecken von Framework-Typen — im roten Build.
- Einheitliches, wiedererkennbares Muster über alle Integrationen.

**Negativ / Spannungsfelder:**

- Mehr Typen (Port + Value-Objects + Adapter) pro Integration — für eine
  triviale Fähigkeit Overhead. Mitigation: nur für **externe** Integrationen,
  nicht für rein interne Services.
- Value-Objects müssen Framework-Strukturen nachbilden (z.B. `MailAnhang` statt
  `MimeMessageHelper.addAttachment`). Akzeptabel — entkoppelt bewusst.
- OIDC ist tief in Spring Security verdrahtet (`OidcUserService`-Vererbung); ein
  vollständiger Port ist dort teurer und wird separat bewertet.

## Alternativen

- **Status quo (gemischt) belassen** — verworfen: Framework-Leak + Erosion ohne Wächter.
- **Anti-Corruption-Layer pro Aufrufer** — verworfen: dupliziert Mapping-Logik,
  kein zentraler Port.
- **Spring Modulith Application-Modules** — als Schicht-2-Folge geführt (siehe
  ADR-0007); ersetzt nicht den hier definierten Port-Schnitt, sondern ergänzt
  Modul-Boundaries später.

## Referenzen

- [`specs/TESTSTRATEGIE.md`](../../specs/TESTSTRATEGIE.md) §Architektur-Verifikation (ARCH-20, ARCH-21, ARCH-22)
- [`specs/TECHNISCHE_SPEZIFIKATION.md`](../../specs/TECHNISCHE_SPEZIFIKATION.md) §Ports-&-Adapter für externe Integrationen
- `src/main/java/ch/sponsorplatz/shared/mail/` (Port) + `…/smtp/` (Adapter)
- `src/test/java/ch/sponsorplatz/architektur/ArchitekturRegelnTest.java` (ARCH-20)
- [ADR-0001](0001-feature-folder-statt-schichten.md) Feature-Folder, [ADR-0007](0007-archunit-fuer-statische-verifikation.md) ArchUnit

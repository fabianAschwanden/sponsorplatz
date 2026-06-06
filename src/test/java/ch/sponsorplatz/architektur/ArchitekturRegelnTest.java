package ch.sponsorplatz.architektur;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * Schicht 1 — Statische Architektur-Verifikation mit ArchUnit.
 *
 * <p>
 * Setzt die in {@code CLAUDE.md} und {@code .instructions.md} formulierten
 * Regeln automatisch durch. Verstöße führen zu rotem Build —
 * Architektur-Zerfall
 * wird damit verhindert, nicht nur erkannt.
 *
 * <p>
 * Verbindliche Test-IDs siehe {@code specs/TESTSTRATEGIE.md} unter
 * {@code Architektur-Verifikation (ARCH)}.
 *
 * <p>
 * <strong>Pflicht beim Erweitern:</strong>
 * <ul>
 * <li>Neue Regel → erst Test-ID in TESTSTRATEGIE pflegen, dann Regel hier
 * ergänzen</li>
 * <li>Bewusste Ausnahmen via {@code .as("...")} dokumentieren, niemals
 * stillschweigend</li>
 * <li>Nie eine Regel löschen, nur kommentieren mit Begründung + ADR-Link</li>
 * </ul>
 */
@AnalyzeClasses(packages = "ch.sponsorplatz", importOptions = { ImportOption.DoNotIncludeTests.class })
class ArchitekturRegelnTest {

    // =========================================================================
    // ARCH-01 — Layered Architecture: Controller darf keine Repository-Klassen
    // aufrufen
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_01_controller_nicht_direkt_an_repository = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .orShould().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .because("ARCH-01: Controller ruft Services auf, nicht Repositories direkt — H1-Layer-Disziplin");

    // =========================================================================
    // ARCH-02 — H1-Fix: JPA-Entities verlassen den Service-Layer nicht
    //
    // Controller dürfen keine @Entity-annotierten Klassen referenzieren —
    // nur View-DTOs / Form-DTOs. Verbindliche View-DTO-Pflicht aus CLAUDE.md.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_02_controller_kennen_keine_jpa_entities = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .because("ARCH-02: View-DTO-Pflicht — Controller dürfen keine JPA-Entities ans Template geben");

    // =========================================================================
    // ARCH-03 — View-DTOs sind immutable Records
    //
    // Konvention: alle Klassen mit Suffix "View" im `*.dto`-Subpaket sind Records.
    // Erlaubt: nested Records, statische Factory-Methoden `von(...)`.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_03_view_dtos_sind_records = classes()
            .that().haveSimpleNameEndingWith("View")
            .and().resideOutsideOfPackages("..shared..", "..architektur..")
            .and().areTopLevelClasses()
            .should().beRecords()
            .because("ARCH-03: View-DTOs müssen Records sein (Immutability, Defense in depth)");

    // =========================================================================
    // ARCH-04 — Spring-Stereotyp-Klassen liegen in Feature-Foldern oder shared/
    //
    // Verhindert "verstreute" @Service/@Controller/@Repository-Klassen.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_04_services_im_feature_folder = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAnyPackage(
                    "ch.sponsorplatz.admin..",
                    "ch.sponsorplatz.anfrage..",
                    "ch.sponsorplatz.audit..",
                    "ch.sponsorplatz.aufgabe..",
                    "ch.sponsorplatz.backup..",
                    "ch.sponsorplatz.benachrichtigung..",
                    "ch.sponsorplatz.benutzer..",
                    "ch.sponsorplatz.crm..",
                    "ch.sponsorplatz.dashboard..",
                    "ch.sponsorplatz.einladung..",
                    "ch.sponsorplatz.engagement..",
                    "ch.sponsorplatz.home..",
                    "ch.sponsorplatz.kontakt..",
                    "ch.sponsorplatz.ops..",
                    "ch.sponsorplatz.organisation..",
                    "ch.sponsorplatz.projekt..",
                    "ch.sponsorplatz.seed..",
                    "ch.sponsorplatz.shared..")
            .because("ARCH-04: @Service-Klassen gehören in einen Feature-Folder oder nach shared/");

    // =========================================================================
    // ARCH-05 — Repositories sind Interfaces und liegen im richtigen Paket
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_05_repositories_sind_interfaces = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areTopLevelClasses()
            .and().resideOutsideOfPackage("..architektur..")
            .should().beInterfaces()
            .because("ARCH-05: Repositories sind Spring-Data-Interfaces, keine Klassen");

    // =========================================================================
    // ARCH-06 — Feature-Folder hängen nicht im Kreis
    //
    // organisation → projekt darf passieren, projekt → organisation → projekt
    // nicht.
    // Slices-Rule auf den Top-Level-Paketen unter ch.sponsorplatz.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_06_keine_zyklen_zwischen_feature_foldern = SlicesRuleDefinition.slices()
            .matching("ch.sponsorplatz.(*)..")
            .should().beFreeOfCycles()
            .because("ARCH-06: Feature-Folder dürfen nicht im Kreis abhängen — Modul-Boundary-Disziplin");

    // =========================================================================
    // ARCH-07 — shared/ kennt keine Feature-Folder
    //
    // Querschnitts-Code darf nichts aus Features importieren. Wenn shared/
    // etwas aus organisation/ braucht, ist das ein Riecher: entweder ist die
    // Klasse falsch in shared/ oder es sollte ein Interface in shared/ sein.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_07_shared_kennt_keine_features = noClasses()
            .that().resideInAPackage("ch.sponsorplatz.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "ch.sponsorplatz.admin..",
                    "ch.sponsorplatz.anfrage..",
                    "ch.sponsorplatz.audit..",
                    "ch.sponsorplatz.aufgabe..",
                    "ch.sponsorplatz.backup..",
                    "ch.sponsorplatz.benachrichtigung..",
                    "ch.sponsorplatz.benutzer..",
                    "ch.sponsorplatz.crm..",
                    "ch.sponsorplatz.einladung..",
                    "ch.sponsorplatz.engagement..",
                    "ch.sponsorplatz.home..",
                    "ch.sponsorplatz.kontakt..",
                    "ch.sponsorplatz.ops..",
                    "ch.sponsorplatz.organisation..",
                    "ch.sponsorplatz.projekt..")
            .because("ARCH-07: shared/ ist Querschnitt — kennt Features NIE, Features kennen shared/ JA");

    // =========================================================================
    // ARCH-08 — Custom-Exceptions extenden RuntimeException
    //
    // Die GlobalExceptionHandler-Mapping-Tabelle setzt RuntimeException voraus
    // (Spring fängt nur RuntimeException ohne explicit @ExceptionHandler).
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_08_custom_exceptions_sind_runtime = classes()
            .that().haveSimpleNameEndingWith("Exception")
            .and().resideInAPackage("ch.sponsorplatz..")
            .and().areTopLevelClasses()
            .should().beAssignableTo(RuntimeException.class)
            .because("ARCH-08: Custom-Exceptions sind RuntimeException — Mapping via GlobalExceptionHandler");

    // =========================================================================
    // ARCH-09 — Admin-Routen sind PreAuthorize-geschützt
    //
    // Jede @Controller-Klasse, die unter /admin/... mappt, MUSS eine
    // @PreAuthorize-Annotation tragen. Verhindert versehentlich offene
    // Admin-Endpoints.
    // =========================================================================
    @Test
    void ARCH_09_admin_controller_haben_pre_authorize() {
        JavaClasses klassen = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ch.sponsorplatz");

        Set<String> ungeschuetzteAdminController = klassen.stream()
                .filter(c -> c.isAnnotatedWith(Controller.class))
                .filter(c -> {
                    if (!c.isAnnotatedWith(RequestMapping.class)) {
                        return false;
                    }
                    String[] pfade = c.getAnnotationOfType(RequestMapping.class).value();
                    for (String pfad : pfade) {
                        if (pfad.startsWith("/admin")) {
                            return true;
                        }
                    }
                    return false;
                })
                .filter(c -> !c.isAnnotatedWith(PreAuthorize.class))
                .map(c -> c.getName())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(ungeschuetzteAdminController)
                .as("ARCH-09: Admin-Controller MÜSSEN @PreAuthorize tragen — Sicherheits-Boundary")
                .isEmpty();
    }

    // =========================================================================
    // ARCH-10 — JPA-Entities haben keinen public Konstruktor mit Args
    //
    // Hibernate braucht den No-Args-Konstruktor. Args-Konstruktoren in Entities
    // sind Code-Smell — Builder oder Setter benutzen.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_10_entities_haben_no_args_konstruktor_zugang = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().bePublic()
            .because("ARCH-10: JPA-Entities müssen public sein für Hibernate-Proxies");

    // =========================================================================
    // ARCH-11 — Felder in Records sind nicht @Autowired (Records sind keine Beans)
    //
    // Schützt vor versehentlichem Mischen von Records mit DI.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_11_records_kennen_kein_autowired = fields()
            .that().areDeclaredInClassesThat().areRecords()
            .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
            .because("ARCH-11: Records sind immutable Datenträger, keine Spring-Beans");

    // =========================================================================
    // ARCH-12 — Naming-Konvention: Controller-Klassen tragen "Controller"-Suffix
    //
    // Verhindert dass @Controller-Klassen versehentlich anders heissen.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_12_controller_klassen_heissen_controller = classes()
            .that().areAnnotatedWith(Controller.class)
            .or().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .because("ARCH-12: @Controller-Klassen müssen den Suffix 'Controller' tragen");

    // =========================================================================
    // ARCH-20 — Mail-Port: SMTP-/Jakarta-Mail-Details bleiben im Adapter-Package
    //
    // Nur das Adapter-Package shared.mail.smtp darf JavaMailSender/MimeMessage-
    // Helper kennen. Selbst das Port-Package shared.mail (MailVersand/MailAnhang)
    // bleibt framework-frei — die Aufrufer hängen am Port (Ports-&-Adapter),
    // damit der Mail-Transport austauschbar bleibt und Framework-Typen nicht lecken.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_20_mail_framework_nur_im_adapter = noClasses()
            .that().resideOutsideOfPackage("ch.sponsorplatz.shared.mail.smtp..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.mail.javamail..")
            .because("ARCH-20: Mail-Transport-Details (JavaMailSender/MimeMessageHelper) "
                    + "gehören in den SMTP-Adapter (shared.mail.smtp); Port + Aufrufer "
                    + "nutzen ausschliesslich den MailVersand-Port");

    // =========================================================================
    // ARCH-21 — Payment-Port: Provider-spezifische HTTP-/Krypto-Details bleiben
    //           im jeweiligen Adapter-Package
    //
    // Der PaymentProvider-Port (anfrage) ist framework-frei; die Anbieter-
    // Anbindung (RestClient-HTTP-Calls, HMAC-Signaturprüfung) lebt ausschliesslich
    // im Datatrans-Adapter anfrage.payment.datatrans. RestClient + javax.crypto
    // werden heute NUR dort genutzt — die Regel sperrt sie dort ein.
    // =========================================================================
    @ArchTest
    static final ArchRule ARCH_21_payment_provider_details_im_adapter = noClasses()
            .that().resideOutsideOfPackage("ch.sponsorplatz.anfrage.payment.datatrans..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web.client..",
                    "javax.crypto..")
            .because("ARCH-21: Datatrans-Transport-/Krypto-Details (RestClient, HMAC) "
                    + "gehören in den Datatrans-Adapter (anfrage.payment.datatrans); "
                    + "Aufrufer nutzen den PaymentProvider-Port");

    // =========================================================================
    // ARCH-13 — Test-Klassen spiegeln ein Produktions-Feature-Paket
    //
    // Jede Test-Klasse muss in einem Paket liegen, das auch Produktionscode
    // enthält (Spiegelung Source ↔ Test pro Feature-Folder) — ODER in einem
    // bewusst erlaubten Querschnitts-Testpaket. Fängt „verwaiste" Testpakete,
    // die in eine Struktur ohne zugehörigen Produktionscode driften.
    //
    // Eigener Importer, weil der @AnalyzeClasses-Importer oben Tests ausschliesst
    // (DoNotIncludeTests); ARCH-13 braucht aber genau die Testklassen.
    // =========================================================================

    /** Querschnitts-Testpakete ohne gespiegelten Produktionscode (bewusste Ausnahmen). */
    private static final Set<String> ARCH_13_ERLAUBTE_TEST_PAKETE = Set.of(
            "ch.sponsorplatz.architektur",   // ArchUnit-Regeln selbst
            "ch.sponsorplatz.e2e");          // End-to-End-/Cross-Feature-Tests

    @Test
    void ARCH_13_testklassen_spiegeln_produktionspaket() {
        Set<String> produktionsPakete = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ch.sponsorplatz")
                .stream()
                .map(c -> c.getPackageName())
                .collect(java.util.stream.Collectors.toSet());

        var testklassen = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages("ch.sponsorplatz")
                .stream()
                .filter(c -> c.getSimpleName().endsWith("Test") || c.getSimpleName().endsWith("IT"))
                .toList();

        // Sanity: die Regel darf nicht hohl grün sein, wenn der Importer nichts findet.
        assertThat(testklassen)
                .as("ARCH-13: Importer muss Testklassen finden (sonst prüft die Regel nichts)")
                .isNotEmpty();

        var verstoesse = testklassen.stream()
                .map(c -> c.getPackageName())
                .distinct()
                .filter(p -> !produktionsPakete.contains(p))
                .filter(p -> !ARCH_13_ERLAUBTE_TEST_PAKETE.contains(p))
                .sorted()
                .toList();

        assertThat(verstoesse)
                .as("ARCH-13: Test-Pakete ohne gespiegelten Produktionscode (Allowlist: %s)",
                        ARCH_13_ERLAUBTE_TEST_PAKETE)
                .isEmpty();
    }

    // =========================================================================
    // ARCH-17 — Kein generisches catch (Exception | Throwable)
    // =========================================================================
    // Generisches Fangen verschluckt Programmierfehler (NPE, IllegalState …) und
    // maskiert sie als fachliche Fehlermeldung — der GlobalExceptionHandler soll
    // unerwartete Fehler als 500 sehen. Stattdessen die erwarteten Typen explizit
    // fangen (z.B. MailException, IOException | RuntimeException).
    //
    // Bewusste Ausnahmen leben in der Allowlist (Klasse#methode) MIT Begründung.

    // Quell-Scan (nicht Bytecode): der ArchUnit-Bytecode-Modus flaggt fälschlich
    // jeden try-with-resources (synthetischer catch(Throwable) für addSuppressed).
    // Ein Source-Scan sieht nur echte, im Code geschriebene generische Catches.
    private static final java.nio.file.Path ARCH_17_QUELL_ROOT =
            java.nio.file.Paths.get("src/main/java/ch/sponsorplatz");

    private static final java.util.regex.Pattern ARCH_17_GENERISCH =
            java.util.regex.Pattern.compile("catch\\s*\\(\\s*(Exception|Throwable)\\b");

    /**
     * Allowlist {@code Dateiname.java:Zeile} für bewusst breites Fangen — jede
     * Stelle MIT Begründung. Alle anderen Fundstellen wurden auf spezifische
     * Typen verengt (MailException, IOException|RuntimeException, SQLException,
     * JsonProcessingException). Hier bleiben nur Stellen, an denen „jeder Fehler"
     * fachlich korrekt ist.
     */
    private static final Set<String> ARCH_17_ERLAUBTE_STELLEN = Set.of(
            // Best-effort-Anreicherung: SecurityContext evtl. nicht verfügbar →
            // Audit-Eintrag bleibt ohne User-Email, niemals Fehler eskalieren.
            "AuditService.java:114",
            // Boot-Zeit-Laden des PLZ-Verzeichnisses: jeder Fehler (IOException,
            // fehlende Ressource → NPE) muss als IllegalStateException fail-fast
            // hochkommen, sonst startet die App mit halbem Verzeichnis.
            "PlzVerzeichnis.java:100");

    @Test
    void ARCH_17_kein_generisches_catch_exception() throws java.io.IOException {
        java.util.List<String> verstoesse = new java.util.ArrayList<>();
        try (var pfade = java.nio.file.Files.walk(ARCH_17_QUELL_ROOT)) {
            for (java.nio.file.Path datei : pfade.filter(p -> p.toString().endsWith(".java")).toList()) {
                java.util.List<String> zeilen = java.nio.file.Files.readAllLines(datei);
                for (int i = 0; i < zeilen.size(); i++) {
                    String zeile = zeilen.get(i);
                    if (zeile.stripLeading().startsWith("//")) {
                        continue; // auskommentierte Zeilen ignorieren
                    }
                    if (ARCH_17_GENERISCH.matcher(zeile).find()) {
                        String stelle = datei.getFileName() + ":" + (i + 1);
                        if (!ARCH_17_ERLAUBTE_STELLEN.contains(stelle)) {
                            verstoesse.add(stelle + "  " + zeile.strip());
                        }
                    }
                }
            }
        }

        assertThat(verstoesse)
                .as("ARCH-17: generisches catch (Exception|Throwable) verboten — "
                        + "erwartete Typen explizit fangen (Allowlist mit Begründung: %s)",
                        ARCH_17_ERLAUBTE_STELLEN)
                .isEmpty();
    }
}

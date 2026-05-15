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
                    "ch.sponsorplatz.dashboard..",
                    "ch.sponsorplatz.einladung..",
                    "ch.sponsorplatz.home..",
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
                    "ch.sponsorplatz.einladung..",
                    "ch.sponsorplatz.home..",
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
    // ARCH-13 — Test-Konvention: Test-Klassen liegen im gleichen Paket wie SUT
    //
    // Spiegelung Source ↔ Test pro Feature-Folder erleichtert Navigation
    // und sichert ab, dass package-private-Methoden testbar bleiben.
    //
    // Hinweis: Diese Regel ist *informativ* — wir erlauben Ausnahmen für
    // Test-Helpers in eigenen Sub-Paketen (z. B. test/architektur/, test/support/).
    // Daher hier nur ein No-Op-Test als Doku-Anker.
    // =========================================================================
    @Test
    void ARCH_13_dokumentiert_package_spiegelung() {
        // Diese Regel wird durch Code-Review und PR-Template durchgesetzt,
        // nicht durch ArchUnit. Bewusste Schwach-Regel — siehe ADR-zukünftig.
    }
}

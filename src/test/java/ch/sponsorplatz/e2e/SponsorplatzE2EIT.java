package ch.sponsorplatz.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Entry-Point der E2E-Suite. Failsafe matched diese Klasse via
 * `**\/e2e/**\/*IT.java`. Cucumber-JUnit-Platform-Engine erkennt die
 * .feature-Dateien unter `src/test/resources/features` und die Step-
 * Definitions im selben Package via {@code cucumber.glue}.
 *
 * <p>Im aktuellen Pilot existiert nur ein Scenario:
 * {@code features/sponsor-anfrage-zu-vertrag.feature}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "ch.sponsorplatz.e2e")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, summary, html:target/cucumber-report.html, json:target/cucumber.json")
public class SponsorplatzE2EIT {
}

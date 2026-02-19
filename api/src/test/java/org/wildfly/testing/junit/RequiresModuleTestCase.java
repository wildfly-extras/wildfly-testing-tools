/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.EventConditions;
import org.wildfly.testing.junit.annotation.JBossHome;

/**
 * Tests for the {@link org.wildfly.testing.junit.annotation.RequiresModule} annotation.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Tag("env-var")
@Tag("system-property")
public class RequiresModuleTestCase {

    @BeforeAll
    public static void setup(@JBossHome final Path jbossHome) throws Exception {
        createJar("resource-root", jbossHome, "2.0.0.Final");
        createJar("snapshot", jbossHome, "1.0.0.Beta2-SNAPSHOT");
    }

    @Test
    public void artifactPassed() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireArtifact.class, "passing"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void artifactSkippedVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireArtifact.class, "skippedVersion"))
                .configurationParameter("jboss.home", System.getProperty("jboss.home"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "Found version 1.0.0.Final and required a minimum of version 2.0.0. Disabling test.")));
    }

    @Test
    public void artifactSkippedMissingModule(@JBossHome final Path jbossHome) {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireArtifact.class, "skippedMissingModule"))
                .configurationParameter("jboss.home", jbossHome.toString())
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "Module org.wildfly.testing.junit.test.artifact.invalid not found in %s. Disabling test.".formatted(
                                jbossHome.resolve("modules")))));
    }

    @Test
    public void resourceRootPassed() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireResourceRoot.class, "passing"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void resourceRootPassedVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireResourceRoot.class, "passingVersion"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void resourceRootSkippedVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireResourceRoot.class, "skippedVersion"))
                .configurationParameter("jboss.home", System.getProperty("jboss.home"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "Found version 2.0.0.Final and required a minimum of version 2.0.1. Disabling test.")));
    }

    @Test
    public void resourceRootSkippedMissingModule(@JBossHome final Path jbossHome) {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireResourceRoot.class, "skippedMissingModule"))
                .configurationParameter("jboss.home", jbossHome.toString())
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "Module org.wildfly.testing.junit.test.resource-root.invalid not found in %s. Disabling test."
                                .formatted(
                                        jbossHome.resolve("modules")))));
    }

    @Test
    public void snapshotPassedVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireSnapshot.class, "passingVersion"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void snapshotSkippedVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireSnapshot.class, "skippedVersion"))
                .configurationParameter("jboss.home", System.getProperty("jboss.home"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "Found version 1.0.0.Beta2-SNAPSHOT and required a minimum of version 1.0.0.Beta3. Disabling test.")));
    }

    @Test
    public void client() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireArtifact.class, "client"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void clientApi() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireArtifact.class, "clientApi"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void clientApiTest() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireArtifact.class, "clientApiTest"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void clientSpi() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireArtifact.class, "clientSpi"))
                .configurationParameter("jboss.home", System.getProperty("jboss.home"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "Found version 1.0.0.Beta1 and required a minimum of version 1.0.0.Final. Disabling test.")));
    }

    @Test
    public void multiModulePassingVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireMultiModules.class, "passingVersion"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void multiModuleSkippedVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireMultiModules.class, "skippedVersion"))
                .configurationParameter("jboss.home", System.getProperty("jboss.home"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "Found version 2.0.0.Final and required a minimum of version 2.0.1. Disabling test.")));
    }

    @Test
    public void multiModuleRequiresModulesPassingVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireMultiModules.class, "passingVersionRequiresModules"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void multiModuleRequiresModulesSkippedVersion() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireMultiModules.class, "skippedVersionRequiresModules"))
                .configurationParameter("jboss.home", System.getProperty("jboss.home"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "Found version 2.0.0.Final and required a minimum of version 2.0.1. Disabling test.")));
    }

    @Test
    public void multiModulePassingAnyOf() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireMultiModules.class, "passingAnyOf"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.succeeded(1L));
    }

    @Test
    public void multiModuleSkippedAnyOf() {
        final var testEvents = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(RequireMultiModules.class, "skippedAnyOf"))
                .configurationParameter("jboss.home", System.getProperty("jboss.home"))
                .execute()
                .testEvents();

        testEvents.assertStatistics((stats) -> stats.skipped(1L));
        testEvents.assertThatEvents().haveExactly(1, EventConditions.event(
                EventConditions.skippedWithReason(
                        "None of the modules have been found, disabling the test.")));
    }

    private static void createJar(final String moduleName, final Path jbossHome, final String version) throws IOException {
        // Create the JAR with a manifest only
        final Path jarPath = jbossHome.resolve(
                Path.of("modules", "org", "wildfly", "testing", "junit", "test", moduleName, "main",
                        "test-%s.jar".formatted(version)));
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, version);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            // Simply flush to write the manifest
            out.flush();
        }
    }
}

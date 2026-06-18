/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.condition;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Tests for {@link Support} class using JUnit Platform TestKit.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class SupportTest {

    @Test
    void resolveJBossHomeFromJBossHomeProperty() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "resolveJBossHome"))
                .configurationParameter("jboss.home", "/opt/wildfly")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void resolveJBossHomeFromJBossHomeDirProperty() {
        // jboss.home.dir is fallback when jboss.home not set
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "resolveJBossHomeDir"))
                .configurationParameter("jboss.home.dir", "/opt/wildfly-alt")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void resolveJBossHomePreferenceOrder() {
        // jboss.home should take precedence over jboss.home.dir
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "resolveJBossHomePreference"))
                .configurationParameter("jboss.home", "/opt/wildfly-primary")
                .configurationParameter("jboss.home.dir", "/opt/wildfly-secondary")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void resolveModulePathPresent() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "resolveModulePath"))
                .configurationParameter("wildfly.module.path", "/opt/wildfly/modules")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void resolveModulePathAbsent() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "resolveModulePathEmpty"))
                // No configuration parameter set
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    // Test methods executed by the TestKit - ExtensionContext is injected by JUnit
    static class TestMethods {
        @Test
        void resolveJBossHome(final ExtensionContext context) {
            final Optional<Path> result = Support.resolveJBossHome(context);
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals(Path.of("/opt/wildfly"), result.get());
        }

        @Test
        void resolveJBossHomeDir(final ExtensionContext context) {
            final Optional<Path> result = Support.resolveJBossHome(context);
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals(Path.of("/opt/wildfly-alt"), result.get());
        }

        @Test
        void resolveJBossHomePreference(final ExtensionContext context) {
            final Optional<Path> result = Support.resolveJBossHome(context);
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals(Path.of("/opt/wildfly-primary"), result.get());
        }

        @Test
        void resolveModulePath(final ExtensionContext context) {
            final Optional<String> result = Support.resolveModulePath(context);
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals("/opt/wildfly/modules", result.get());
        }

        @Test
        void resolveModulePathEmpty(final ExtensionContext context) {
            final Optional<String> result = Support.resolveModulePath(context);
            Assertions.assertTrue(result.isEmpty());
        }
    }
}

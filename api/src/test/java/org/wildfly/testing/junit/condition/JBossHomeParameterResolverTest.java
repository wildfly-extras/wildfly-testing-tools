/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.condition;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.wildfly.testing.junit.annotation.JBossHome;

/**
 * Tests for {@link JBossHomeParameterResolver} using JUnit Platform TestKit.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class JBossHomeParameterResolverTest {

    @Test
    void resolveParameterAsPath() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "withJBossHomePath", Path.class))
                .configurationParameter("jboss.home", "/opt/wildfly")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void resolveParameterAsString() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "withJBossHomeString", String.class))
                .configurationParameter("jboss.home", "/opt/wildfly")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void resolveParameterAsFile() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "withJBossHomeFile", File.class))
                .configurationParameter("jboss.home", "/opt/wildfly")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void resolveParameterWhenJBossHomeNotSet() {
        // Don't set jboss.home - should fail with ParameterResolutionException
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "withJBossHomePath", Path.class))
                // No configuration parameter set
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
    }

    @Test
    void resolveParameterWithUnsupportedType() {
        // Integer is not a supported type
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "withUnsupportedType", Integer.class))
                .configurationParameter("jboss.home", "/opt/wildfly")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
    }

    @Test
    void parameterWithoutJBossHomeAnnotationShouldFail() {
        // Parameter without @JBossHome should not be resolved by this resolver
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectMethod(TestMethods.class, "withoutJBossHome", Path.class))
                .configurationParameter("jboss.home", "/opt/wildfly")
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));
    }

    // Test methods executed by the TestKit
    static class TestMethods {
        @Test
        void withJBossHomePath(@JBossHome final Path path) {
            Assertions.assertNotNull(path);
            Assertions.assertTrue(path.isAbsolute());
        }

        @Test
        void withJBossHomeString(@JBossHome final String path) {
            Assertions.assertNotNull(path);
            Assertions.assertFalse(path.isEmpty());
        }

        @Test
        void withJBossHomeFile(@JBossHome final File file) {
            Assertions.assertNotNull(file);
            Assertions.assertTrue(file.isAbsolute());
        }

        @Test
        void withUnsupportedType(@JBossHome final Integer unsupported) {
            Assertions.fail("Should not reach here - parameter resolution should fail");
        }

        @Test
        void withoutJBossHome(final Path path) {
            Assertions.fail("Should not reach here - parameter resolution should fail");
        }
    }
}

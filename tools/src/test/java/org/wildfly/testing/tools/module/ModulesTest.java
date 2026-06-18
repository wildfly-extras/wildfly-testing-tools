/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.tools.module;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class ModulesTest {

    @BeforeAll
    public static void createModuleDir() throws IOException {
        ModuleEnvironment.createBaseModuleDir();
    }

    @Test
    public void checkModulePath() {
        Assertions.assertEquals(ModuleEnvironment.BASE_MODULE_DIR, Modules.discoverModulePath(),
                "Failed to discover the expected module path. The module path should be the first path which is not said to be immutable.");
    }

    @Test
    public void discoverModulePathWhenNeitherPropertySet() {
        // Save and clear both properties
        final String originalModulePath = System.getProperty("module.path");
        final String originalJBossHome = System.getProperty("jboss.home");
        try {
            System.clearProperty("module.path");
            System.clearProperty("jboss.home");

            final IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                    Modules::discoverModulePath);
            Assertions.assertTrue(exception.getMessage().contains("module.path") ||
                    exception.getMessage().contains("jboss.home"));
        } finally {
            if (originalModulePath != null) {
                System.setProperty("module.path", originalModulePath);
            }
            if (originalJBossHome != null) {
                System.setProperty("jboss.home", originalJBossHome);
            }
        }
    }
}

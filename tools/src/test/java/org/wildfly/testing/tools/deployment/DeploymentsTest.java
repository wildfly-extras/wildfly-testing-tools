/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.tools.deployment;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Deployments} utility class.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class DeploymentsTest {

    @Test
    void warWithName() {
        final WebArchive archive = Deployments.war("myapp");
        Assertions.assertNotNull(archive);
        Assertions.assertEquals("myapp.war", archive.getName());
    }

    @Test
    void warWithClass() {
        final WebArchive archive = Deployments.war(DeploymentsTest.class);
        Assertions.assertNotNull(archive);
        Assertions.assertEquals("DeploymentsTest.war", archive.getName());
    }

    @Test
    void jarWithName() {
        final JavaArchive archive = Deployments.jar("mylib");
        Assertions.assertNotNull(archive);
        Assertions.assertEquals("mylib.jar", archive.getName());
    }

    @Test
    void jarWithClass() {
        final JavaArchive archive = Deployments.jar(DeploymentsTest.class);
        Assertions.assertNotNull(archive);
        Assertions.assertEquals("DeploymentsTest.jar", archive.getName());
    }

    @Test
    void rarWithName() {
        final ResourceAdapterArchive archive = Deployments.rar("myra");
        Assertions.assertNotNull(archive);
        Assertions.assertEquals("myra.rar", archive.getName());
    }

    @Test
    void rarWithClass() {
        final ResourceAdapterArchive archive = Deployments.rar(DeploymentsTest.class);
        Assertions.assertNotNull(archive);
        Assertions.assertEquals("DeploymentsTest.rar", archive.getName());
    }
}

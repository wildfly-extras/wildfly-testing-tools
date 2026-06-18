/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.tools.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Utility class for creating ShrinkWrap archives with conventional naming.
 * <p>
 * This is particularly useful when building complex deployments like EARs that contain multiple modules:
 * </p>
 *
 * <pre>
 * &#64;GenerateDeployment(DeploymentType.EAR)
 * public static void deployment(EnterpriseArchive ear) {
 *     ear.addAsModule(Deployments.war("myapp")
 *             .addClasses(MyServlet.class));
 *     ear.addAsLibrary(Deployments.jar("mylib")
 *             .addClasses(MyLibrary.class));
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 */
public class Deployments {

    private Deployments() {
        // Utility class
    }

    /**
     * Creates a {@link WebArchive} with the specified name and {@code .war} extension.
     *
     * @param name the name of the archive (without extension)
     *
     * @return a new WebArchive
     */
    public static WebArchive war(final String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war");
    }

    /**
     * Creates a {@link WebArchive} using the test class simple name and {@code .war} extension.
     *
     * @param testClass the test class to derive the name from
     *
     * @return a new WebArchive
     */
    public static WebArchive war(final Class<?> testClass) {
        return war(testClass.getSimpleName());
    }

    /**
     * Creates a {@link JavaArchive} with the specified name and {@code .jar} extension.
     *
     * @param name the name of the archive (without extension)
     *
     * @return a new JavaArchive
     */
    public static JavaArchive jar(final String name) {
        return ShrinkWrap.create(JavaArchive.class, name + ".jar");
    }

    /**
     * Creates a {@link JavaArchive} using the test class simple name and {@code .jar} extension.
     *
     * @param testClass the test class to derive the name from
     *
     * @return a new JavaArchive
     */
    public static JavaArchive jar(final Class<?> testClass) {
        return jar(testClass.getSimpleName());
    }

    /**
     * Creates a {@link ResourceAdapterArchive} with the specified name and {@code .rar} extension.
     *
     * @param name the name of the archive (without extension)
     *
     * @return a new ResourceAdapterArchive
     */
    public static ResourceAdapterArchive rar(final String name) {
        return ShrinkWrap.create(ResourceAdapterArchive.class, name + ".rar");
    }

    /**
     * Creates a {@link ResourceAdapterArchive} using the test class simple name and {@code .rar} extension.
     *
     * @param testClass the test class to derive the name from
     *
     * @return a new ResourceAdapterArchive
     */
    public static ResourceAdapterArchive rar(final Class<?> testClass) {
        return rar(testClass.getSimpleName());
    }
}
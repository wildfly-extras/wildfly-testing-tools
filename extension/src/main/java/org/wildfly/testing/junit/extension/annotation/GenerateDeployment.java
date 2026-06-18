/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Used to identify a method as a method which creates a deployment. The method must be static and must have a return
 * type of {@code void}.
 * <p>
 * The method must accept a parameter of a known {@link Archive} type, see {@linkplain DeploymentType the deployment types}
 * for details. An additional parameter of {@link org.junit.jupiter.api.TestInfo} may also be included on the method,
 * but is not required.
 * </p>
 * <p>
 * The name of the deployment will be the name of the test.
 * </p>
 * <p>
 * A {@link GenerateDeployment} method cannot be present on the same test that includes a {@link DeploymentProducer}.
 * These are mutually exclusive deployment descriptors.
 * </p>
 *
 * <h2>Examples</h2>
 *
 * <pre>
 * // Simple case - infer WAR from parameter type
 * &#64;GenerateDeployment
 * public static void deployment(WebArchive war) {
 *     war.addClasses(TestServlet.class);
 * }
 *
 * // Explicit JAR type
 * &#64;GenerateDeployment(DeploymentType.JAR)
 * public static void deployment(JavaArchive jar) {
 *     jar.addClasses(MyClass.class);
 * }
 *
 * // With TestInfo parameter
 * &#64;GenerateDeployment
 * public static void deployment(WebArchive war, TestInfo testInfo) {
 *     if (testInfo.getTags().contains("cdi")) {
 *         war.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
 *              .addClass(CdiResource.class)
 *     }
 *     war.addClasses(TestServlet.class);
 * }
 *
 * // Building an EAR with modules
 * &#64;GenerateDeployment(DeploymentType.EAR)
 * public static void deployment(EnterpriseArchive ear) {
 *     ear.addAsModule(Deployments.war("myapp").addClasses(TestServlet.class));
 *     ear.addAsLibrary(Deployments.jar("mylib").addClasses(CommonUtils.class));
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 */
@Inherited
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateDeployment {

    /**
     * The supported deployment types.
     */
    enum DeploymentType {
        /**
         * Creates a {@link WebArchive} deployment.
         */
        WAR(WebArchive.class, ".war"),
        /**
         * Creates a {@link EnterpriseArchive} deployment.
         */
        EAR(EnterpriseArchive.class, ".ear"),
        /**
         * Creates a {@link JavaArchive} deployment.
         */
        JAR(JavaArchive.class, ".jar"),
        /**
         * Creates a {@link ResourceAdapterArchive} deployment.
         */
        RAR(ResourceAdapterArchive.class, ".rar"),
        /**
         * Indicates the deployment type should be inferred from the first parameter which must be a known extension
         * of {@link Archive}.
         */
        INFER(null, null),
        ;

        private final Class<? extends Archive<?>> archiveType;
        private final String extension;

        DeploymentType(final Class<? extends Archive<?>> archiveType, final String extension) {
            this.archiveType = archiveType;
            this.extension = extension;
        }

        /**
         * The type of the archive.
         *
         * @return the type
         */
        public Class<? extends Archive<?>> archiveType() {
            return archiveType;
        }

        /**
         * The extension for the deployment.
         *
         * @return the extension
         */
        public String extension() {
            return extension;
        }
    }

    /**
     * Indicates the type of the deployment to be produced if the deployment method has a {@code void} return type.
     *
     * @return the type of the deployment
     */
    DeploymentType value() default DeploymentType.INFER;
}

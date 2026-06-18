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

/**
 * Used to identify a method as a method which creates a deployment. The method must be static and must have a return
 * type of any valid {@link org.jboss.shrinkwrap.api.Archive}.
 * <p>
 * The method can have no parameters or at most one parameter of type {@link org.junit.jupiter.api.TestInfo}.
 * </p>
 * <p>
 * A {@link DeploymentProducer} method cannot be present on the same test that includes a {@link GenerateDeployment}. These are
 * mutually exclusive deployment descriptors.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 */
@Inherited
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface DeploymentProducer {
}

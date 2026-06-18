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

import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.testing.junit.extension.ServerResourceExtension;

/**
 * Used to inject a resource as fields, constructor parameters or method parameters into tests.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 */
@Inherited
@Documented
@Target({
        ElementType.FIELD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({
        ServerResourceExtension.class
})
public @interface ServerResource {
}

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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.testing.junit.extension.WildFlyExtension;

/**
 * An annotation which starts a WildFly server and manages a server in standalone mode.
 * <p>
 * Standalone tests are automatically tagged with {@code "wildfly-standalone"} to allow selective test execution.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("wildfly-standalone")
@ExtendWith({
        WildFlyExtension.class
})
public @interface WildFlyTest {
}

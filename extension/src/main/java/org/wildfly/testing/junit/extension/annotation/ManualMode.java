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
import org.wildfly.plugin.tools.server.ServerManager;

/**
 * Indicates a test is a manual mode test. Meaning the test itself is responsible for the lifecycle of the server. If
 * {@linkplain #value() auto-start} is set to {@code true}, the server will start at the beginning of the test.
 * <p>
 * Manual mode tests are automatically tagged with {@code "wildfly-manual"} to allow selective test execution.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("wildfly-manual")
public @interface ManualMode {

    /**
     * If set to {@code true}, indicates the server should start at the beginning of the test. If set to {@code false},
     * the server will not be running for the test until explicitly {@linkplain ServerManager#start() started}.
     *
     * @return {@code true} to auto-start the server when the test is started, otherwise {@code false} to control the
     *             lifecycle
     */
    boolean value() default false;
}

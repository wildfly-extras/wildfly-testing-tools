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
 * Indicates this test should run in WildFly domain mode.
 * <p>
 * This annotation is used in conjunction with {@link ServerGroup} to specify deployment targets
 * and {@link DomainServer} to qualify resource injection for specific servers.
 * <p>
 * Domain tests are automatically tagged with {@code "wildfly-domain"} to allow selective test execution.
 * You cannot mix domain and standalone tests in the same execution, unless the all tests are {@link ManualMode} tests
 * and the {@link ManualMode#value()} is set to {@code false}.
 *
 * <p>
 * Example:
 *
 * <pre>
 * &#x40;WildFlyDomainTest
 * public class DomainDeploymentTest {
 *
 *     &#x40;DeploymentProducer
 *     &#x40;ServerGroup("main-server-group")
 *     public static WebArchive deployment() {
 *         return ShrinkWrap.create(WebArchive.class)
 *                 .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
 *     }
 *
 *     &#x40;ServerResource
 *     &#x40;DomainServer("server-one")
 *     private URI uri;
 *
 *     &#x40;Test
 *     public void test() {
 *         // Test against domain deployment
 *     }
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @see ServerGroup
 * @see DomainServer
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("wildfly-domain")
@ExtendWith(WildFlyExtension.class)
public @interface WildFlyDomainTest {
}

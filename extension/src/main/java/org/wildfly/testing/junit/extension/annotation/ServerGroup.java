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
 * Specifies the WildFly domain server group(s) to which a deployment should be deployed.
 * <p>
 * This annotation is placed on {@link DeploymentProducer} or {@link GenerateDeployment} methods
 * in tests annotated with {@link WildFlyDomainTest}.
 * <p>
 * The value can be a single server group or an array of server groups. Server group names must
 * match those defined in the domain configuration ({@code domain.xml}).
 * <p>
 * <strong>Note:</strong> This annotation is only used on tests annotated with {@link WildFlyDomainTest}.
 * It is silently ignored on standalone tests ({@link WildFlyTest}), which can be useful when using
 * test inheritance where a domain test extends a standalone test.
 *
 * <p>
 * Example with single server group:
 *
 * <pre>
 * &#x40;DeploymentProducer
 * &#x40;ServerGroup("main-server-group")
 * public static WebArchive deployment() {
 *     return ShrinkWrap.create(WebArchive.class);
 * }
 * </pre>
 *
 * <p>
 * Example with multiple server groups:
 *
 * <pre>
 * &#x40;DeploymentProducer
 * &#x40;ServerGroup({ "main-server-group", "other-server-group" })
 * public static WebArchive deployment() {
 *     return ShrinkWrap.create(WebArchive.class);
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 * @see WildFlyDomainTest
 * @see DomainServer
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServerGroup {

    /**
     * The name(s) of the server group(s) to deploy to.
     *
     * @return an array of server group names
     */
    String[] value();
}

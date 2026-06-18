/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Annotations for configuring WildFly server lifecycle and deployment management in JUnit tests.
 * <p>
 * This package contains the core annotations used to configure the WildFly JUnit Extension, which automatically
 * manages WildFly server startup, shutdown, and deployment lifecycle during test execution.
 * </p>
 *
 * <h2>Test Mode Annotations</h2>
 * <ul>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.WildFlyTest @WildFlyTest} - Enables the extension
 * for standalone server mode</li>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.WildFlyDomainTest @WildFlyDomainTest} - Enables
 * the extension for domain mode</li>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.ManualMode @ManualMode} - Disables automatic
 * server lifecycle management</li>
 * </ul>
 *
 * <h2>Deployment Annotations</h2>
 * <ul>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.DeploymentProducer @DeploymentProducer} - Marks
 * a method that creates a deployment using ShrinkWrap</li>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.GenerateDeployment @GenerateDeployment} - Automatically
 * generates a deployment from test class annotations</li>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.ServerGroup @ServerGroup} - Specifies target server
 * groups for domain mode deployments</li>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.DomainServer @DomainServer} - Qualifies resources
 * for a specific domain server</li>
 * </ul>
 *
 * <h2>Resource Injection Annotations</h2>
 * <ul>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.ServerResource @ServerResource} - Injects server
 * resources like {@link java.net.URI}, {@link org.wildfly.plugin.tools.ServerManager}, or custom resources</li>
 * <li>{@link org.wildfly.testing.junit.extension.annotation.RequestPath @RequestPath} - Qualifies URI injection
 * with a specific path</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>
 * {
 *     &#64;code
 *     &#64;WildFlyTest
 *     public class HelloWorldTest {
 *
 *         &#64;DeploymentProducer
 *         public static WebArchive createDeployment() {
 *             return ShrinkWrap.create(WebArchive.class)
 *                     .addClass(HelloServlet.class);
 *         }
 *
 *         @Test
 *         public void testHello(@ServerResource URI uri, @ServerResource HttpClient client) throws Exception {
 *             HttpRequest request = HttpRequest.newBuilder(uri.resolve("hello")).build();
 *             HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
 *             Assertions.assertEquals(200, response.statusCode());
 *         }
 *     }
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 *
 * @see org.wildfly.testing.junit.extension.annotation.WildFlyTest
 * @see org.wildfly.testing.junit.extension.annotation.WildFlyDomainTest
 * @see org.wildfly.testing.junit.extension.annotation.DeploymentProducer
 * @see org.wildfly.testing.junit.extension.annotation.ServerResource
 */
package org.wildfly.testing.junit.extension.annotation;
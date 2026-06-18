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
import java.net.URI;

/**
 * Qualifies a {@link URI} or {@link jakarta.ws.rs.client.WebTarget} injection point with a relative path that is
 * appended to the deployment's base URI. This annotation is used in conjunction with {@link ServerResource} to create
 * URIs or WebTargets that point to specific endpoints within your deployed application.
 * <p>
 * The path specified in this annotation is appended to the base URI of the deployment, handling slashes
 * automatically. For example, if the deployment base URI is {@code http://localhost:8080/myapp} and you
 * specify {@code @RequestPath("/api/users")}, the resulting URI will be {@code http://localhost:8080/myapp/api/users}.
 * </p>
 *
 * <p>
 * <strong>Example usage with URI injection:</strong>
 * </p>
 *
 * <pre>
 * &#x40;WildFlyTest
 * public class OrderTest {
 *     &#x40;ServerResource
 *     &#x40;RequestPath("/api/orders")
 *     private URI ordersUri;
 *
 *     &#x40;Test
 *     public void testGetOrders(&#x40;ServerResource HttpClient client) throws Exception {
 *         HttpRequest request = HttpRequest.newBuilder(ordersUri).GET().build();
 *         HttpResponse&lt;String&gt; response = client.send(request, HttpResponse.BodyHandlers.ofString());
 *         Assertions.assertEquals(200, response.statusCode());
 *     }
 * }
 * </pre>
 *
 * <p>
 * <strong>Example usage with WebTarget injection:</strong>
 * </p>
 *
 * <pre>
 * &#x40;WildFlyTest
 * public class UserTest {
 *     &#x40;ServerResource
 *     &#x40;RequestPath("/api/users")
 *     private WebTarget usersTarget;
 *
 *     &#x40;Test
 *     public void testGetUsers() throws Exception {
 *         try (Response response = usersTarget.request().get()) {
 *             Assertions.assertEquals(200, response.getStatus());
 *             User[] users = response.readEntity(User[].class);
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>
 * <strong>Example usage with parameter injection:</strong>
 * </p>
 *
 * <pre>
 * &#x40;WildFlyTest
 * public class ProductTest {
 *     &#x40;Test
 *     public void testGetProducts(&#x40;ServerResource &#x40;RequestPath("/api/products") WebTarget productsTarget) {
 *         try (Response response = productsTarget.request().get()) {
 *             Assertions.assertEquals(200, response.getStatus());
 *         }
 *     }
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @see ServerResource
 */
@Inherited
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestPath {

    /**
     * The relative path to append to the deployment's base URI. The path can start with or without a leading slash;
     * the framework handles slash normalization automatically.
     *
     * @return the relative path
     */
    String value();
}

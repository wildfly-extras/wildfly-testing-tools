/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.extension.extension.deployment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.testing.junit.extension.annotation.DeploymentProducer;
import org.wildfly.testing.junit.extension.annotation.RequestPath;
import org.wildfly.testing.junit.extension.annotation.ServerResource;
import org.wildfly.testing.junit.extension.annotation.WildFlyTest;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@WildFlyTest
public class StandaloneDeploymentIT {

    @DeploymentProducer
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(TestServlet.class);
    }

    @ServerResource
    @RequestPath("/test")
    private URI uri;

    @Test
    public void validateUri() {
        Assertions.assertNotNull(uri);
        Assertions.assertTrue(uri.toString().endsWith("/test"),
                () -> "Expected URI to contain the request path /test at the end: %s".formatted(uri));
    }

    @Test
    public void checkResponse(@ServerResource final HttpClient client) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode(),
                () -> "Expected HTTP status code %d: %s".formatted(response.statusCode(), response.body()));
        Assertions.assertTrue(response.body().startsWith("Test"));
    }
}

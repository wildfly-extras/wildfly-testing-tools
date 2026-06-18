/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.extension.extension.deployment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.plugin.tools.server.ServerManager;
import org.wildfly.testing.junit.extension.annotation.ServerResource;

/**
 * Abstract base class for EAR deployment tests. Subclasses only need to provide the deployment method.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
abstract class AbstractEarDeploymentTest {

    @ServerResource
    private ServerManager serverManager;

    @Test
    public void serverRunningWithEarDeployment() {
        Assertions.assertTrue(serverManager.isRunning(), "Server should be running with EAR deployment");
    }

    @Test
    public void validateUri() {
        final URI uri = uri();
        Assertions.assertNotNull(uri);
        Assertions.assertTrue(uri.toString().endsWith("/test"),
                () -> "Expected URI to contain the request path /test at the end: %s".formatted(uri));
    }

    @Test
    public void checkResponse(@ServerResource final HttpClient client) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri())
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode(),
                () -> "Expected HTTP status code %d: %s".formatted(response.statusCode(), response.body()));
        Assertions.assertTrue(response.body().startsWith("Test"));
    }

    protected abstract URI uri();
}

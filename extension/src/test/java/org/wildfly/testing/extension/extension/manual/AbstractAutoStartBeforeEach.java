/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.extension.extension.manual;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.plugin.tools.server.ServerManager;
import org.wildfly.testing.junit.extension.annotation.ManualMode;
import org.wildfly.testing.junit.extension.annotation.ServerResource;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ManualMode(true)
abstract class AbstractAutoStartBeforeEach implements ManualModeTest {

    public static void createDefaultDeployment(final WebArchive war) {
        war.addClasses(TestServlet.class);
    }

    @ServerResource
    private ServerManager serverManager;

    @BeforeEach
    public void serverStarted() {
        Assertions.assertTrue(serverManager.isRunning(), "Server should be running");
        Assertions.assertEquals(expectedLaunchType(), serverManager.launchType());
    }

    @Test
    public void validateUri() {
        final var uri = getUri();
        Assertions.assertNotNull(uri);
        Assertions.assertTrue(uri.toString().endsWith("/test"),
                () -> "Expected URI to contain the request path /test at the end: %s".formatted(uri));
    }

    @Test
    public void checkResponse(@ServerResource final HttpClient client) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(getUri())
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode(),
                () -> "Expected HTTP status code %d: %s".formatted(response.statusCode(), response.body()));
        Assertions.assertTrue(response.body().startsWith("Test"));
    }

    @WebServlet("test")
    public static class TestServlet extends HttpServlet {
        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().println("Test");
            resp.getWriter().flush();
        }
    }

    protected abstract URI getUri();
}

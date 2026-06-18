/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.extension.extension.resources;

import java.net.URI;
import java.net.http.HttpClient;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.jboss.as.controller.client.ModelControllerClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.plugin.tools.DeploymentManager;
import org.wildfly.plugin.tools.server.ServerManager;
import org.wildfly.testing.junit.extension.annotation.RequestPath;
import org.wildfly.testing.junit.extension.annotation.ServerResource;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
abstract class ServerResourceInjection {

    @ServerResource
    private static ModelControllerClient staticClient;

    @ServerResource
    private static DeploymentManager staticDeploymentManager;

    @ServerResource
    private static URI staticBaseUri;

    @ServerResource
    @RequestPath("/test")
    private static URI staticTestUri;

    @ServerResource
    private static ServerManager staticServerManager;

    @ServerResource
    private static Client staticRestClient;

    @ServerResource
    private static HttpClient staticHttpClient;

    @ServerResource
    private static WebTarget staticBaseWebTarget;

    @ServerResource
    @RequestPath("/test")
    private static WebTarget staticTestWebTarget;

    @ServerResource
    private ModelControllerClient client;

    @ServerResource
    private DeploymentManager deploymentManager;

    @ServerResource
    private URI baseUri;

    @ServerResource
    @RequestPath("/test")
    private URI testUri;

    @ServerResource
    private ServerManager serverManager;

    @ServerResource
    private Client restClient;

    @ServerResource
    private HttpClient httpClient;

    @ServerResource
    private WebTarget baseWebTarget;

    @ServerResource
    @RequestPath("/test")
    private WebTarget testWebTarget;

    @BeforeEach
    void clientBeforeEach(@ServerResource final ModelControllerClient client) {
        Assertions.assertNotNull(client, "The ModelControllerClient should not be null");
    }

    @BeforeEach
    void baseUriBeforeEach(@ServerResource final URI uri) {
        Assertions.assertNotNull(uri, "The URI should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080"), uri);
    }

    @BeforeEach
    void testUriBeforeEach(@ServerResource @RequestPath("test") final URI uri) {
        Assertions.assertNotNull(uri, "The URI should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080/test"), uri);
    }

    @BeforeEach
    void deploymentManagerBeforeEach(@ServerResource final DeploymentManager deploymentManager) {
        Assertions.assertNotNull(deploymentManager, "The DeploymentManager should not be null");
    }

    @BeforeEach
    void serverManagerBeforeEach(@ServerResource final ServerManager serverManager) {
        Assertions.assertNotNull(serverManager, "The ServerManager should not be null");
    }

    @Test
    void staticClient() {
        Assertions.assertNotNull(staticClient, "The static ModelControllerClient should not be null");
    }

    @Test
    void client() {
        Assertions.assertNotNull(client, "The ModelControllerClient should not be null");
    }

    @Test
    void clientParameter(@ServerResource final ModelControllerClient client) {
        Assertions.assertNotNull(client, "The ModelControllerClient parameter should not be null");
    }

    @Test
    void staticBaseUri() {
        Assertions.assertNotNull(staticBaseUri, "The static URI should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080"), staticBaseUri);
    }

    @Test
    void baseUri() {
        Assertions.assertNotNull(baseUri, "The URI should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080"), baseUri);
    }

    @Test
    void baseUriParameter(@ServerResource final URI baseUri) {
        Assertions.assertNotNull(baseUri, "The URI parameter should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080"), baseUri);
    }

    @Test
    void staticTestUri() {
        Assertions.assertNotNull(staticTestUri, "The static URI should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080/test"), staticTestUri);
    }

    @Test
    void testUri() {
        Assertions.assertNotNull(testUri, "The URI should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080/test"), testUri);
    }

    @Test
    void testUriParameter(@ServerResource @RequestPath("test") final URI testUri) {
        Assertions.assertNotNull(testUri, "The URI parameter should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080/test"), testUri);
    }

    @Test
    void staticDeploymentManager() {
        Assertions.assertNotNull(staticDeploymentManager, "The static DeploymentManager should not be null");
    }

    @Test
    void deploymentManager() {
        Assertions.assertNotNull(deploymentManager, "The DeploymentManager should not be null");
    }

    @Test
    void deploymentManagerParameter(@ServerResource final DeploymentManager deploymentManager) {
        Assertions.assertNotNull(deploymentManager, "The DeploymentManager parameter should not be null");
    }

    @Test
    void staticServerManager() {
        Assertions.assertNotNull(staticServerManager, "The static ServerManager should not be null");
        Assertions.assertTrue(staticServerManager.isRunning(), "The ServerManager should be running");
    }

    @Test
    void setStaticServerManager() {
        Assertions.assertNotNull(serverManager, "The ServerManager should not be null");
        Assertions.assertTrue(serverManager.isRunning(), "The ServerManager should be running");
    }

    @Test
    void serverManagerParameter(@ServerResource final ServerManager serverManager) {
        Assertions.assertNotNull(serverManager, "The ServerManager parameter should not be null");
        Assertions.assertTrue(serverManager.isRunning(), "The ServerManager should be running");
    }

    @Test
    void staticRestClient() {
        Assertions.assertNotNull(staticRestClient, "The static Client should not be null");
    }

    @Test
    void restClient() {
        Assertions.assertNotNull(restClient, "The Client should not be null");
    }

    @Test
    void serverRestClient(@ServerResource final Client client) {
        Assertions.assertNotNull(client, "The ServerManager parameter should not be null");
    }

    @Test
    void staticHttpClient() {
        Assertions.assertNotNull(staticHttpClient, "The static Client should not be null");
    }

    @Test
    void httpClient() {
        Assertions.assertNotNull(httpClient, "The Client should not be null");
    }

    @Test
    void serverHttpClient(@ServerResource final HttpClient client) {
        Assertions.assertNotNull(client, "The ServerManager parameter should not be null");
    }

    @Test
    void staticBaseWebTarget() {
        Assertions.assertNotNull(staticBaseWebTarget, "The static WebTarget should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080"), staticBaseWebTarget.getUri());
    }

    @Test
    void baseWebTarget() {
        Assertions.assertNotNull(baseWebTarget, "The WebTarget should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080"), baseWebTarget.getUri());
    }

    @Test
    void baseWebTargetParameter(@ServerResource final WebTarget baseWebTarget) {
        Assertions.assertNotNull(baseWebTarget, "The WebTarget parameter should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080"), baseWebTarget.getUri());
    }

    @Test
    void staticTestWebTarget() {
        Assertions.assertNotNull(staticTestWebTarget, "The static WebTarget should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080/test"), staticTestWebTarget.getUri());
    }

    @Test
    void testWebTarget() {
        Assertions.assertNotNull(testWebTarget, "The WebTarget should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080/test"), testWebTarget.getUri());
    }

    @Test
    void testWebTargetParameter(@ServerResource @RequestPath("test") final WebTarget testWebTarget) {
        Assertions.assertNotNull(testWebTarget, "The WebTarget parameter should not be null");
        Assertions.assertEquals(URI.create("http://localhost:8080/test"), testWebTarget.getUri());
    }
}

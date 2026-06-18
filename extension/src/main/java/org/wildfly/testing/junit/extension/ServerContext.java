/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.support.AnnotationSupport;
import org.wildfly.plugin.tools.server.Configuration;
import org.wildfly.plugin.tools.server.ServerManager;
import org.wildfly.plugin.tools.server.ServerManagerListener;
import org.wildfly.testing.junit.extension.annotation.WildFlyDomainTest;
import org.wildfly.testing.junit.extension.api.DomainConfigurationFactory;
import org.wildfly.testing.junit.extension.api.ServerConfiguration;
import org.wildfly.testing.junit.extension.api.StandaloneConfigurationFactory;

/**
 * Holds server-scoped resources that live for the duration of the server lifecycle.
 * <p>
 * Resources are stored in the global (launcher session) scope and are shared across all test classes.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class ServerContext {
    private static final Logger LOGGER = Logger.getLogger(ServerContext.class);
    private static final ExtensionContext.Namespace SERVER_NAMESPACE = ExtensionContext.Namespace
            .create("WildFly.Server");
    private static final String SERVER_KEY = "wildfly.server";
    private static final String CLIENT_KEY = "wildfly.rest-client";
    private static final String HTTP_CLIENT_KEY = "wildfly.http-client";

    /**
     * Gets the already-started server, if available.
     *
     * @param context the extension context
     *
     * @return the server manager, or empty if not started
     */
    static Optional<ServerManager> getServer(final ExtensionContext context) {
        final Store store = getGlobalStore(context);
        final ServerResource resource = store.get(SERVER_KEY, ServerResource.class);
        return Optional.ofNullable(resource).map(ServerResource::serverManager);
    }

    /**
     * Gets or starts the shared WildFly server instance.
     * The server is stored in the root context store and shared across all test classes.
     *
     * @param context the extension context
     *
     * @return the server manager
     */
    static ServerManager getOrCreateServerManager(final ExtensionContext context) {
        return ServerContext.getOrCreateServer(context, () -> {
            // Start the server
            final ServerManager serverManager = createServer(context);
            // Return a CloseableResource that stops server on cleanup
            return new ServerResource(serverManager, context);
        });
    }

    /**
     * Gets or creates the shared Jakarta REST Client instance.
     * The client is stored in the global scope and reused across all tests.
     *
     * @param context the extension context
     *
     * @return the REST client
     */
    static Client getOrCreateClient(final ExtensionContext context) {
        final Store store = getGlobalStore(context);
        return store.computeIfAbsent(CLIENT_KEY,
                key -> new WrappedClient(ClientBuilder.newClient()), Client.class);
    }

    /**
     * Gets or creates the shared HttpClient instance.
     * The client is stored in the global scope and reused across all tests.
     *
     * @param context the extension context
     *
     * @return the HTTP client
     */
    static HttpClient getOrCreateHttpClient(final ExtensionContext context) {
        final Store store = getGlobalStore(context);
        return store.computeIfAbsent(HTTP_CLIENT_KEY,
                key -> new WrappedHttpClient(HttpClient.newHttpClient()),
                HttpClient.class);
    }

    private static void removeClient(final ExtensionContext context) {
        final Store store = getGlobalStore(context);
        final Client client = store.remove(CLIENT_KEY, Client.class);
        if (client instanceof WrappedClient) {
            ((WrappedClient) client).internalClose();
        }
    }

    private static void removeHttpClient(final ExtensionContext context) {
        final Store store = getGlobalStore(context);
        final HttpClient client = store.remove(HTTP_CLIENT_KEY, HttpClient.class);
        if (client instanceof WrappedHttpClient) {
            try {
                ((WrappedHttpClient) client).internalClose();
            } catch (Exception e) {
                LOGGER.debugf(e, "Failed to close client %s", client);
            }
        }
    }

    /**
     * Gets or creates the shared ServerManager instance.
     * The server is stored in the root context store and shared across all test classes.
     *
     * @param context  the extension context
     * @param supplier supplier to create the ServerManager if it doesn't exist
     *
     * @return the server manager
     */
    @SuppressWarnings("resource")
    private static ServerManager getOrCreateServer(final ExtensionContext context,
            final Supplier<ServerResource> supplier) {
        final Store store = getGlobalStore(context);
        return store.computeIfAbsent(SERVER_KEY, key -> supplier.get(), ServerResource.class).serverManager();
    }

    private static Store getGlobalStore(final ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.StoreScope.LAUNCHER_SESSION, SERVER_NAMESPACE);
    }

    @SuppressWarnings("resource")
    private static ServerManager createServer(final ExtensionContext context) {
        // Determine configuration based on launch type
        final Class<?> testClass = context.getRequiredTestClass();
        final boolean hasDomainTest = AnnotationSupport
                .findAnnotation(testClass, WildFlyDomainTest.class).isPresent();

        final Configuration<?> configuration;
        if (hasDomainTest) {
            configuration = DomainConfigurationFactory.create()
                    .configuration(context);
        } else {
            configuration = StandaloneConfigurationFactory.create()
                    .configuration(context);
        }
        return ServerManager.of(configuration).addServerManagerListener(new ServerManagerListener() {
            @Override
            public void beforeShutdown(final ServerManager serverManager) {
                removeClient(context);
                removeHttpClient(context);
            }
        });
    }

    static void stopServer(final ExtensionContext context, final ServerManager serverManager) {
        try {
            // Get timeout from configuration (defaults to 60 seconds)
            final long timeout = ServerConfiguration.timeout(context);
            serverManager.shutdown(timeout);
        } catch (IOException e) {
            // If graceful shutdown fails, kill the server
            serverManager.kill();
            LOGGER.error("Failed to stop server", e);
        } finally {
            removeClient(context);
            removeHttpClient(context);
        }
    }

    /**
     * Wrapper for ServerManager that implements AutoCloseable for automatic cleanup.
     * Package-private to allow access from {@link ServerContext}.
     */
    private record ServerResource(ServerManager serverManager, ExtensionContext context) implements AutoCloseable {

        @Override
        public void close() {
            stopServer(context, serverManager);
        }
    }

    private static class WrappedClient implements Client {
        private final Client delegate;

        WrappedClient(final Client delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            // Do nothing
        }

        @Override
        public WebTarget target(final String uri) {
            return delegate.target(uri);
        }

        @Override
        public WebTarget target(final URI uri) {
            return delegate.target(uri);
        }

        @Override
        public WebTarget target(final UriBuilder uriBuilder) {
            return delegate.target(uriBuilder);
        }

        @Override
        public WebTarget target(final Link link) {
            return delegate.target(link);
        }

        @Override
        public Invocation.Builder invocation(final Link link) {
            return delegate.invocation(link);
        }

        @Override
        public SSLContext getSslContext() {
            return delegate.getSslContext();
        }

        @Override
        public HostnameVerifier getHostnameVerifier() {
            return delegate.getHostnameVerifier();
        }

        @Override
        public jakarta.ws.rs.core.Configuration getConfiguration() {
            return delegate.getConfiguration();
        }

        @Override
        public Client property(final String name, final Object value) {
            return delegate.property(name, value);
        }

        @Override
        public Client register(final Class<?> componentClass) {
            return delegate.register(componentClass);
        }

        @Override
        public Client register(final Class<?> componentClass, final int priority) {
            return delegate.register(componentClass, priority);
        }

        @Override
        public Client register(final Class<?> componentClass, final Class<?>... contracts) {
            return delegate.register(componentClass, contracts);
        }

        @Override
        public Client register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
            return delegate.register(componentClass, contracts);
        }

        @Override
        public Client register(final Object component) {
            return delegate.register(component);
        }

        @Override
        public Client register(final Object component, final int priority) {
            return delegate.register(component, priority);
        }

        @Override
        public Client register(final Object component, final Class<?>... contracts) {
            return delegate.register(component, contracts);
        }

        @Override
        public Client register(final Object component, final Map<Class<?>, Integer> contracts) {
            return delegate.register(component, contracts);
        }

        void internalClose() {
            delegate.close();
        }
    }

    private static class WrappedHttpClient extends HttpClient implements AutoCloseable {
        private final HttpClient delegate;

        private WrappedHttpClient(final HttpClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return delegate.cookieHandler();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return delegate.connectTimeout();
        }

        @Override
        public Redirect followRedirects() {
            return delegate.followRedirects();
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return delegate.proxy();
        }

        @Override
        public SSLContext sslContext() {
            return delegate.sslContext();
        }

        @Override
        public SSLParameters sslParameters() {
            return delegate.sslParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return delegate.authenticator();
        }

        @Override
        public Version version() {
            return delegate.version();
        }

        @Override
        public Optional<Executor> executor() {
            return delegate.executor();
        }

        @Override
        public <T> HttpResponse<T> send(final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            return delegate.send(request, responseBodyHandler);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(final HttpRequest request,
                final HttpResponse.BodyHandler<T> responseBodyHandler) {
            return delegate.sendAsync(request, responseBodyHandler);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(final HttpRequest request,
                final HttpResponse.BodyHandler<T> responseBodyHandler,
                final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler);
        }

        @Override
        public WebSocket.Builder newWebSocketBuilder() {
            return delegate.newWebSocketBuilder();
        }

        @Override
        public void close() throws Exception {
            // Do nothing
        }

        void internalClose() throws Exception {
            if (delegate instanceof AutoCloseable) {
                ((AutoCloseable) delegate).close();
            }
        }
    }
}

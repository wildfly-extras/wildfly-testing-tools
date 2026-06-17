/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.support.AnnotationSupport;
import org.wildfly.plugin.tools.server.Configuration;
import org.wildfly.plugin.tools.server.ServerManager;
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
        return ServerManager.of(configuration);
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
}

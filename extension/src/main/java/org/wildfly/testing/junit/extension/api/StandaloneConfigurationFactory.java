/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension.api;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.wildfly.core.launcher.StandaloneCommandBuilder;
import org.wildfly.plugin.tools.server.Configuration;
import org.wildfly.plugin.tools.server.StandaloneConfiguration;

/**
 * Factory for creating {@link StandaloneConfiguration} instances.
 * <p>
 * Configuration is resolved in the following order (highest priority first):
 * <ol>
 * <li>ServiceLoader - custom implementations via META-INF/services</li>
 * <li>JUnit Platform configuration properties (junit-platform.properties)</li>
 * <li>System properties</li>
 * <li>Environment variables</li>
 * </ol>
 *
 * <p>
 * Supported JUnit Platform configuration properties:
 * <ul>
 * <li>{@code jboss.home} - Path to WildFly installation</li>
 * <li>{@code wildfly.java.home} - Java home to use for the server</li>
 * <li>{@code wildfly.module.path} - Module path for the server</li>
 * <li>{@code wildfly.java.opts} - Additional JVM arguments for the server</li>
 * <li>{@code wildfly.server.args} - Additional server arguments (e.g., --stability=preview)</li>
 * <li>{@code wildfly.http.protocol} - HTTP protocol (http or https)</li>
 * <li>{@code wildfly.http.port} - HTTP/HTTPS port</li>
 * <li>{@code wildfly.debug} - Enable remote debugging (true if empty or "true")</li>
 * <li>{@code wildfly.debug.port} - Remote debug port (default: 8787)</li>
 * <li>{@code wildfly.debug.suspend} - Suspend on startup (default: true)</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class StandaloneConfigurationFactory {

    /**
     * Creates a standalone configuration.
     *
     * @param context the extension context for accessing JUnit configuration
     *
     * @return the standalone configuration
     */
    public final StandaloneConfiguration configuration(final ExtensionContext context) {
        final Path jbossHome = ServerConfiguration.resolveJBossHome(context).orElseThrow(() -> new JUnitException(
                "Server home not configured. Set jboss.home in junit-platform.properties, " +
                        "jboss.home system property, or JBOSS_HOME environment variable."));

        final StandaloneCommandBuilder commandBuilder = StandaloneCommandBuilder.of(jbossHome);

        // Configure optional properties
        ServerConfiguration.resolveJavaHome(context).ifPresent(commandBuilder::setJavaHome);

        ServerConfiguration.resolveModulePath(context)
                .ifPresent(modulePath -> commandBuilder.setModuleDirs(modulePath));

        context.getConfigurationParameter("wildfly.java.opts")
                .ifPresent(value -> commandBuilder.addJavaOptions(ServerConfiguration.splitArguments(value)));

        context.getConfigurationParameter("wildfly.server.args")
                .ifPresent(value -> commandBuilder.addServerArguments(ServerConfiguration.splitArguments(value)));

        final var protocolOpt = context.getConfigurationParameter(ServerConfiguration.PROTOCOL_PROPERTY);
        final var portOpt = context.getConfigurationParameter(ServerConfiguration.PORT_PROPERTY, Integer::parseInt);

        if (portOpt.isPresent()) {
            final int port = portOpt.get();
            if (protocolOpt.isPresent()) {
                commandBuilder.addJavaOption("-Djboss.%s.port=%d".formatted(protocolOpt.get(), port));
            } else {
                commandBuilder.addJavaOption("-Djboss.http.port=%d".formatted(port));
            }
        }
        // Check the context parameters for wildfly.debug, wildfly.debug.port and wildfly.debug.suspend
        final var debugProperty = context.getConfigurationParameter("wildfly.debug");
        if (debugProperty.isPresent()) {
            final var debugEnabled = debugProperty.get().isEmpty() || Boolean.parseBoolean(debugProperty.get());
            if (debugEnabled) {
                final int port = context.getConfigurationParameter("wildfly.debug.port")
                        .map(Integer::parseInt).orElse(8787);
                final boolean suspend = context.getConfigurationParameter("wildfly.debug.suspend")
                        .map((value) -> {
                            if (value.isEmpty()) {
                                return true;
                            }
                            return Boolean.parseBoolean(value);
                        })
                        .orElse(true);
                commandBuilder.setDebug(suspend, port);
            }
        }

        return configure(commandBuilder);
    }

    /**
     * Configure the {@link StandaloneCommandBuilder} and create the {@link StandaloneConfiguration}.
     *
     * @param commandBuilder the command builder used to create the configuration
     *
     * @return the standalone configuration
     */
    protected StandaloneConfiguration configure(final StandaloneCommandBuilder commandBuilder) {
        return Configuration.create(commandBuilder);
    }

    /**
     * Creates a factory instance using the hybrid configuration approach.
     *
     * @return a configuration factory
     */
    public static StandaloneConfigurationFactory create() {
        // Try ServiceLoader first for custom implementations
        final ServiceLoader<StandaloneConfigurationFactory> loader = ServiceLoader.load(StandaloneConfigurationFactory.class);
        final Optional<StandaloneConfigurationFactory> factory = loader.findFirst();
        return factory.orElseGet(StandaloneConfigurationFactory::new);
    }

}

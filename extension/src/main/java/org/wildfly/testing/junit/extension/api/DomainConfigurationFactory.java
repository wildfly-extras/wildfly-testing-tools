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
import org.wildfly.core.launcher.DomainCommandBuilder;
import org.wildfly.plugin.tools.server.Configuration;
import org.wildfly.plugin.tools.server.DomainConfiguration;

/**
 * Factory for creating {@link DomainConfiguration} instances.
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
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class DomainConfigurationFactory {

    /**
     * Creates a domain configuration.
     *
     * @param context the extension context for accessing JUnit configuration
     *
     * @return the domain configuration
     */
    public final DomainConfiguration configuration(final ExtensionContext context) {
        final Path jbossHome = ServerConfiguration.resolveJBossHome(context).orElseThrow(() -> new JUnitException(
                "Server home not configured. Set jboss.home in junit-platform.properties, " +
                        "jboss.home system property, or JBOSS_HOME environment variable."));

        final DomainCommandBuilder commandBuilder = DomainCommandBuilder.of(jbossHome);

        // Configure optional properties
        ServerConfiguration.resolveJavaHome(context).ifPresent(commandBuilder::setJavaHome);

        ServerConfiguration.resolveModulePath(context)
                .ifPresent(modulePath -> commandBuilder.setModuleDirs(modulePath));

        final var protocolOpt = context.getConfigurationParameter(ServerConfiguration.PROTOCOL_PROPERTY);
        final var portOpt = context.getConfigurationParameter(ServerConfiguration.PORT_PROPERTY, Integer::parseInt);

        context.getConfigurationParameter("wildfly.java.opts").ifPresent(value -> {
            final var javaOpts = ServerConfiguration.splitArguments(value);
            commandBuilder.addHostControllerJavaOptions(javaOpts);
            commandBuilder.addProcessControllerJavaOptions(javaOpts);
        });

        context.getConfigurationParameter("wildfly.server.args")
                .ifPresent(value -> commandBuilder.addServerArguments(ServerConfiguration.splitArguments(value)));

        if (portOpt.isPresent()) {
            final int port = portOpt.get();
            if (protocolOpt.isPresent()) {
                commandBuilder.addProcessControllerJavaOption("-Djboss.%s.port=%d".formatted(protocolOpt.get(), port));
            } else {
                commandBuilder.addProcessControllerJavaOption("-Djboss.http.port=%d".formatted(port));
            }
        }

        return configure(commandBuilder);
    }

    /**
     * Configure the {@link DomainCommandBuilder} and create the {@link DomainConfiguration}.
     *
     * @param commandBuilder the command builder used to create the configuration
     *
     * @return the domain configuration
     */
    protected DomainConfiguration configure(final DomainCommandBuilder commandBuilder) {
        return Configuration.create(commandBuilder);
    }

    /**
     * Creates a factory instance using the hybrid configuration approach.
     *
     * @return a configuration factory
     */
    public static DomainConfigurationFactory create() {
        // Try ServiceLoader first for custom implementations
        final ServiceLoader<DomainConfigurationFactory> loader = ServiceLoader.load(DomainConfigurationFactory.class);
        final Optional<DomainConfigurationFactory> factory = loader.findFirst();
        return factory.orElseGet(DomainConfigurationFactory::new);
    }

}

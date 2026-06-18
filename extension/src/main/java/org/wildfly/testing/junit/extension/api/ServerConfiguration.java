/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * The configuration settings for the server.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 */
public class ServerConfiguration {

    private static final Pattern WHITESPACE_IF_NOT_QUOTED = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    static final String PROTOCOL_PROPERTY = "wildfly.http.protocol";
    static final String PORT_PROPERTY = "wildfly.http.port";

    /**
     * Resolves the servers base directory. This first uses the
     * {@link ExtensionContext#getConfigurationParameter(String)} with the {@code jboss.home} key. If not found, it
     * checks for the environment variable {@code JBOSS_HOME}. If none of those are set, a
     * {@link org.junit.platform.commons.JUnitException} is thrown.
     *
     * @param context the extension context used to look up the property
     *
     * @return the path to the server
     */
    static Optional<Path> resolveJBossHome(final ExtensionContext context) {
        return context.getConfigurationParameter("jboss.home")
                .or(() -> Optional.ofNullable(System.getenv("JBOSS_HOME")))
                .or(() -> context.getConfigurationParameter("jboss.home.dir"))
                .map(Path::of);
    }

    /**
     * Resolves the {@code wildfly.java.home} property from the context.
     *
     * @param context the extension context
     *
     * @return the value of the {@code wildfly.java.home}, otherwise empty
     */
    static Optional<Path> resolveJavaHome(final ExtensionContext context) {
        return context.getConfigurationParameter("wildfly.java.home", Path::of);
    }

    /**
     * Resolves the {@code wildfly.module.path} property from the context.
     *
     * @param context the extension context
     *
     * @return the value of the {@code wildfly.module.path}, otherwise empty
     */
    static Optional<String> resolveModulePath(final ExtensionContext context) {
        return context.getConfigurationParameter("wildfly.module.path");
    }

    /**
     * Resolves the timeout, in seconds, from the configuration parameter {@code wildfly.timeout}. The value is used
     * for startup and shutdown timeouts. The default is 60 seconds.
     *
     * @param context the context to resolve the configuration from
     *
     * @return the timeout
     */
    public static long timeout(final ExtensionContext context) {
        return context.getConfigurationParameter("wildfly.timeout", Long::parseLong).orElse(60L);
    }

    /**
     * Resolves the base URI from the configuration parameters {@code wildfly.http.protocol}, {@code wildfly.http.host},
     * and {@code wildfly.http.port}.
     * <p>
     * The port defaults to {@code 8080} for HTTP or {@code 8443} for HTTPS if not explicitly set.
     * </p>
     * <p>
     * <table>
     * <tr>
     * <th>Property</th>
     * <th>Default</th>
     * </tr>
     * <tr>
     * <td>{@code wildfly.http.protocol}</td>
     * <td>http</td>
     * </tr>
     * <tr>
     * <td>{@code wildfly.http.host}</td>
     * <td>localhost</td>
     * </tr>
     * <tr>
     * <td>{@code wildfly.http.port}</td>
     * <td>8080 (HTTP) or 8443 (HTTPS)</td>
     * </tr>
     * </table>
     * </p>
     *
     * @param context the context to resolve the values from
     *
     * @return the base URI
     */
    public static String resolveBaseUri(final ExtensionContext context) {
        // Resolve the protocol
        final String protocol = context.getConfigurationParameter(PROTOCOL_PROPERTY).orElse("http");

        // Resolve the host
        final String host = context.getConfigurationParameter("wildfly.http.host").orElse("localhost");

        // Resolve the port
        final int port = context.getConfigurationParameter(PORT_PROPERTY, Integer::parseInt).orElseGet(() -> {
            if ("https".equalsIgnoreCase(protocol)) {
                return 8443;
            }
            return 8080;
        });

        return "%s://%s:%d".formatted(protocol, host, port);
    }

    /**
     * Splits a string containing arguments at whitespace, respecting quoted strings.
     * <p>
     * Example: {@code "-Xmx512m \"-agentlib:jdwp=transport=dt_socket,server=y\""}
     * <br/>
     * becomes {@code ["-Xmx512m", "-agentlib:jdwp=transport=dt_socket,server=y"]}
     * </p>
     *
     * @param options the options string to split
     *
     * @return a list of arguments
     */
    static List<String> splitArguments(String options) {
        final List<String> params = new ArrayList<>();
        final Matcher m = WHITESPACE_IF_NOT_QUOTED.matcher(options);
        while (m.find()) {
            if (m.group(1) != null) {
                params.add(m.group(1));
            } else {
                params.add(m.group(2));
            }
        }
        return params;
    }

}

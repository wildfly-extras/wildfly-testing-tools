/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.kohsuke.MetaInfServices;
import org.wildfly.plugin.tools.server.DomainManager;
import org.wildfly.plugin.tools.server.ServerManager;
import org.wildfly.testing.junit.extension.annotation.DomainServer;
import org.wildfly.testing.junit.extension.annotation.RequestPath;
import org.wildfly.testing.junit.extension.api.ServerConfiguration;
import org.wildfly.testing.junit.extension.api.ServerResourceProducer;

/**
 * Produces {@link URI} and {@link WebTarget} instances for injection into test fields and parameters.
 * The URI is resolved from the deployed application's base URI, optionally combined with a
 * {@link RequestPath} qualifier.
 * <p>
 * For {@link WebTarget} injection, a pre-configured client target is created pointing to the
 * resolved URI, ready for making REST requests without additional configuration.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@MetaInfServices
public class UriResourceProducer implements ServerResourceProducer {
    @Override
    public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... annotations) {
        return URI.class.isAssignableFrom(clazz) || WebTarget.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... annotations)
            throws IllegalArgumentException {
        if (!URI.class.isAssignableFrom(clazz) && !WebTarget.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(
                    "Type %s is not assignable to %s or %s".formatted(clazz.getName(), URI.class.getName(),
                            WebTarget.class.getName()));
        }

        final Optional<ServerManager> opt = ServerContext.getServer(context);
        if (opt.isEmpty()) {
            // Shouldn't happen, but we have no server so we can't resolve anything
            return URI.create(ServerConfiguration.resolveBaseUri(context));
        }
        final ServerManager server = opt.get();
        final Supplier<URI> supplier;

        // Get deployment info from cache
        final Optional<DeploymentInfo> deploymentInfo = DeploymentContext.resolveDeployment(context);
        if (deploymentInfo.isEmpty()) {
            supplier = () -> URI.create(ServerConfiguration.resolveBaseUri(context));
        } else {
            // Create the supplier for cases when we have not yet resolved the URI
            final DomainServer domainServer = findQualifier(DomainServer.class, annotations);
            if (domainServer == null) {
                supplier = () -> resolveDeploymentUri(context, server, deploymentInfo.get().deploymentName());
            } else {
                supplier = () -> resolveDeploymentUri(context, server, deploymentInfo.get()
                        .deploymentName(), domainServer.value());
            }
        }

        final URI baseUri = DeploymentContext.computeIfAbsent(context, supplier);

        URI resultUri = baseUri;

        // Check for RequestPath qualifier to append to base URI
        final RequestPath requestPath = findQualifier(RequestPath.class, annotations);
        if (requestPath != null) {
            resultUri = createUri(baseUri, requestPath.value());
        }

        if (URI.class.isAssignableFrom(clazz)) {
            return resultUri;
        }
        final Client client = ServerContext.getOrCreateClient(context);
        return client.target(resultUri);
    }

    /**
     * Creates a URI by appending a path to a base URI, handling slashes correctly.
     *
     * @param baseUri the base URI
     * @param path    the path to append
     *
     * @return the combined URI
     */
    private URI createUri(final URI baseUri, final String path) {
        final String uriString = baseUri.toString();
        if (uriString.endsWith("/")) {
            if (path.startsWith("/")) {
                return URI.create(uriString + path.substring(1));
            } else {
                return URI.create(uriString + path);
            }
        }
        // Handle double slashes when both base ends with "/" and path starts with "/"
        if (path.startsWith("/")) {
            return URI.create(uriString + path);
        }
        return URI.create("%s/%s".formatted(uriString, path));
    }

    /**
     * Finds a qualifier annotation in an array of annotations.
     *
     * @param qualifier   the qualifier type to find
     * @param annotations the annotations to search
     * @param <T>         the qualifier type
     *
     * @return the qualifier annotation, or {@code null} if not found
     */
    private static <T extends Annotation> T findQualifier(final Class<T> qualifier, final Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(qualifier)) {
                return qualifier.cast(annotation);
            }
        }
        return null;
    }

    /**
     * Resolves the URI for a deployed application by querying the Undertow subsystem.
     * If the context-root cannot be determined (e.g., Undertow not available, non-web deployment),
     * returns a default base URI.
     *
     * @param context        the extension context
     * @param serverManager  the server manager
     * @param deploymentName the deployment name
     *
     * @return the resolved URI
     */
    private URI resolveDeploymentUri(final ExtensionContext context, final ServerManager serverManager,
            final String deploymentName) {
        String baseUri = ServerConfiguration.resolveBaseUri(context);

        // Try to get the context-root from Undertow subsystem
        final ModelNode address = resolveAddress(serverManager, null, deploymentName);

        try {
            final ModelNode result = serverManager.client()
                    .execute(Operations.createReadAttributeOperation(address, "context-root"));

            // If successful, append the context-root to base URI
            if (Operations.isSuccessfulOutcome(result)) {
                final String contextRoot = Operations.readResult(result).asString();
                baseUri = baseUri + (contextRoot.startsWith("/") ? contextRoot : "/" + contextRoot);
            }
            // If not successful, just use base URI (might not be a web deployment)
        } catch (IOException ignore) {
            // Ignore - Undertow might not be available, or this might not be a web deployment
            // Just use the default base URI
        }

        return URI.create(baseUri);
    }

    private URI resolveDeploymentUri(final ExtensionContext context, final ServerManager serverManager,
            final String deploymentName,
            final String domainServer) {
        if (serverManager instanceof DomainManager) {
            String baseUri = ServerConfiguration.resolveBaseUri(context);

            try {
                // Try to get the context-root from Undertow subsystem
                final ModelNode address = resolveAddress(serverManager, domainServer, deploymentName);
                final ModelNode result = serverManager.client()
                        .execute(Operations.createReadAttributeOperation(address, "context-root"));

                // If successful, append the context-root to base URI
                if (Operations.isSuccessfulOutcome(result)) {
                    final String contextRoot = Operations.readResult(result).asString();
                    baseUri = baseUri + (contextRoot.startsWith("/") ? contextRoot : "/" + contextRoot);
                }
                // If not successful, just use base URI (might not be a web deployment)
            } catch (IOException e) {
                // Ignore - Undertow might not be available, or this might not be a web deployment
                // Just use the default base URI
            }
            return URI.create(baseUri);
        }
        throw new JUnitException("ServerManager %s is not a DomainManager".formatted(serverManager));
    }

    private static ModelNode resolveAddress(final ServerManager serverManager, final String domainServer,
            final String deploymentName) {
        try {
            final ModelNode address;
            if (domainServer != null && serverManager instanceof DomainManager domainManager) {
                address = domainManager.determineHostAddress()
                        .add("server", domainServer)
                        .add("deployment", deploymentName);
            } else {
                address = Operations.createAddress("deployment", deploymentName);
            }
            // Let's attempt to find the WAR
            if (deploymentName.toLowerCase(Locale.ROOT).endsWith(".ear")) {
                final ModelNode op = Operations.createOperation("read-children-names", address);
                op.get("child-type").set("subdeployment");
                final ModelNode result = serverManager.client().execute(op);
                if (Operations.isSuccessfulOutcome(result)) {
                    final List<String> subdeployments = Operations.readResult(result).asList().stream()
                            .map(ModelNode::asString)
                            .toList();
                    for (String subdeployment : subdeployments) {
                        // Just use the first WAR
                        if (subdeployment.toLowerCase(Locale.ROOT).endsWith(".war")) {
                            address.add("subdeployment", subdeployment);
                            break;
                        }
                    }
                }
            }
            return address.add("subsystem", "undertow");
        } catch (IOException ignore) {
            // If we cannot read the deployment archive, fall back to the simple address
        }
        return Operations.createAddress("deployment", deploymentName, "subsystem", "undertow");
    }
}

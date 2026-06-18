/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.kohsuke.MetaInfServices;
import org.wildfly.testing.junit.extension.api.ServerResourceProducer;

/**
 * Produces {@link Client} (Jakarta REST Client) instances for injection into test fields and parameters.
 * The Client is obtained from ServerContext and shared across all tests for the server lifecycle.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@MetaInfServices
public class RestClientProducer implements ServerResourceProducer {
    @Override
    public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... annotations) {
        return Client.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... annotations)
            throws IllegalArgumentException {
        if (!Client.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(
                    "Type %s is not assignable to %s".formatted(clazz.getName(), Client.class.getName()));
        }

        // Get the client from ServerContext
        return ServerContext.getOrCreateClient(context);
    }
}

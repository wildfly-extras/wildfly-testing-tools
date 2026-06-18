/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension.api;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * An implementation of this interface is used to inject static fields, instance fields and parameters. For static
 * fields and instance fields, the field must be annotated with
 * {@link org.wildfly.testing.junit.extension.annotation.ServerResource}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 */
public interface ServerResourceProducer {

    /**
     * Indicates the type can be produced from this producer.
     *
     * @param context     the current extension context
     * @param clazz       the type to check
     * @param annotations the annotations, if any, for the field or parameter
     *
     * @return {@code true} if this producer can create this type, otherwise {@code false}
     */
    boolean canInject(ExtensionContext context, Class<?> clazz, Annotation... annotations);

    /**
     * Creates the object which can be injected into a static field, an instance field or a parameter.
     *
     * @param context     the current extension context
     * @param clazz       the type of the field or parameter
     * @param annotations the annotations, if any, for the field or parameter
     *
     * @return the newly constructed object or {@code null} if it could not be produced
     *
     * @throws IllegalArgumentException if the type cannot be assigned to the type this producer can produce
     */
    Object produce(ExtensionContext context, Class<?> clazz, Annotation... annotations) throws IllegalArgumentException;
}

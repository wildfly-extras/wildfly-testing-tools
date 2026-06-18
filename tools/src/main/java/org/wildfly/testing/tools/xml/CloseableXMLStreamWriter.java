/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.tools.xml;

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A simple {@link XMLStreamWriter} which also implements {@link AutoCloseable}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 */
public interface CloseableXMLStreamWriter extends AutoCloseable, XMLStreamWriter {

    /**
     * Creates an {@link XMLStreamWriter} which writes to the output stream.
     *
     * @param out the output stream to write to
     *
     * @return a closeable XML stream writer
     *
     * @throws XMLStreamException if an error occurs creating the writer
     */
    static CloseableXMLStreamWriter of(final OutputStream out) throws XMLStreamException {
        return new IndentingXmlWriter(out);
    }

    /**
     * Creates an {@link XMLStreamWriter} which writes to the output stream.
     *
     * @param writer the writer to write to
     *
     * @return a closeable XML stream writer
     *
     * @throws XMLStreamException if an error occurs creating the writer
     */
    static CloseableXMLStreamWriter of(final Writer writer) throws XMLStreamException {
        return new IndentingXmlWriter(writer);
    }
}

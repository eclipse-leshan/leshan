/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.model;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.util.Validate;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * A DDF File Validator.
 * <p>
 * Validate a DDF File against the embedded LWM2M schema.
 * <p>
 * Support LWM2M version 1.0 and 1.1.
 */

public class DefaultDDFFileValidator implements DDFFileValidator {
    private static String LWM2M_V1_0_SCHEMA_PATH = "/schemas/LWM2M.xsd";
    private static String LWM2M_V1_1_SCHEMA_PATH = "/schemas/LWM2M-v1_1.xsd";

    private final String schema;

    /**
     * Create a {@link DDFFileValidator} using the LWM2M v1.1 schema.
     */
    public DefaultDDFFileValidator() {
        this(LwM2mVersion.V1_1);
    }

    /**
     * Create a {@link DDFFileValidator} using schema corresponding to LWM2M {@link LwM2mVersion}.
     */
    public DefaultDDFFileValidator(LwM2mVersion version) {
        Validate.notNull(version, "version must not be null");
        if (LwM2mVersion.V1_0.equals(version)) {
            schema = LWM2M_V1_0_SCHEMA_PATH;
        } else if (LwM2mVersion.V1_1.equals(version)) {
            schema = LWM2M_V1_1_SCHEMA_PATH;
        } else {
            throw new IllegalStateException(String.format("Unsupported version %s", version));
        }
    }

    @Override
    public void validate(Node xmlToValidate) throws InvalidDDFFileException {
        try {
            validate(new DOMSource(xmlToValidate));
        } catch (SAXException | IOException e) {
            throw new InvalidDDFFileException(e);
        }
    }

    /**
     * Validate a XML {@link Source} against the embedded LWM2M Schema.
     * 
     * @param xmlToValidate an XML source to validate
     * @throws SAXException see {@link Validator#validate(Source)}
     * @throws IOException see {@link Validator#validate(Source)}
     */
    public void validate(Source xmlToValidate) throws SAXException, IOException {
        Validator validator = getEmbeddedLwM2mSchema().newValidator();
        validator.validate(xmlToValidate);
    }

    /**
     * Get the Embedded the LWM2M.xsd Schema.
     * 
     * @throws SAXException see {@link SchemaFactory#newSchema(Source)}
     */
    protected Schema getEmbeddedLwM2mSchema() throws SAXException {
        InputStream inputStream = DDFFileValidator.class.getResourceAsStream(schema);
        Source source = new StreamSource(inputStream);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(source);
    }
}

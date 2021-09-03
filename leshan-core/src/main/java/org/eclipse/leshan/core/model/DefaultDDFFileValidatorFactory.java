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

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Create {@link DefaultDDFFileValidator}.
 * <p>
 * Support LWM2M version 1.0 and 1.1.
 */

public class DefaultDDFFileValidatorFactory implements DDFFileValidatorFactory {

    @Override
    public LwM2mVersion extractLWM2MVersion(Document document, String DocumentName) throws InvalidDDFFileException {
        NodeList nodes = document.getElementsByTagName("LWM2M");
        if (nodes.getLength() != 1) {
            throw new InvalidDDFFileException("DDF file %s should have 1 <LWM2M> element.", DocumentName);
        }
        Node lwm2mNode = nodes.item(0);
        Node schemaLocationAttr = lwm2mNode.getAttributes().getNamedItem("xsi:noNamespaceSchemaLocation");
        if (schemaLocationAttr == null) {
            throw new InvalidDDFFileException(
                    "<LWM2M> tag in %s should have a 'xsi:noNamespaceSchemaLocation' attribute", DocumentName);
        }
        String schemaLocation = schemaLocationAttr.getTextContent();
        if (schemaLocation != null) {
            if (schemaLocation.endsWith("LWM2M.xsd")) {
                return LwM2mVersion.V1_0;
            } else if (schemaLocation.endsWith("LWM2M-v1_1.xsd")) {
                return LwM2mVersion.V1_1;
            }
        }
        throw new InvalidDDFFileException("unsupported value [%s] for attribute 'xsi:noNamespaceSchemaLocation' in %s",
                schemaLocation, DocumentName);
    }

    @Override
    public DDFFileValidator create(LwM2mVersion lwm2mVersion) {
        return new DefaultDDFFileValidator(lwm2mVersion);
    }
}
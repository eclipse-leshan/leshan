/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.lwm2m.attributes;

import java.util.Set;

import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.attributes.QuotedStringAttribute;
import org.eclipse.leshan.core.parser.StringParser;

/**
 * A Generic Attribute of type Double.
 */
public class StringAttributeModel extends LwM2mAttributeModel<String> {

    public StringAttributeModel(String coRELinkParam, Attachment attachment, Set<AssignationLevel> assignationLevels,
            AccessMode accessMode) {
        super(coRELinkParam, attachment, assignationLevels, accessMode, String.class);
    }

    @Override
    public String toCoreLinkValue(LwM2mAttribute<String> attr) {
        String valueEscaped = attr.getValue().replace("\"", "\\\"");
        return "\"" + valueEscaped + "\"";
    }

    /**
     * 
     * Validate a quoted-string from https://datatracker.ietf.org/doc/html/rfc2616#section-2.2:
     * 
     * <pre>
     * {@code
     * quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
     * qdtext         = <any TEXT except <">>
     * quoted-pair    = "\" CHAR
     * }
     * </pre>
     */
    @Override
    public <E extends Throwable> LwM2mAttribute<String> consumeAttributeValue(StringParser<E> parser) throws E {
        String strValue = QuotedStringAttribute.consumeQuotedString(parser);
        return new LwM2mAttribute<String>(this, strValue);
    }

    @Override
    public LwM2mAttribute<String> createEmptyAttribute() throws InvalidAttributeException {
        canBeValueless();
        return new LwM2mAttribute<String>(this);
    }
}

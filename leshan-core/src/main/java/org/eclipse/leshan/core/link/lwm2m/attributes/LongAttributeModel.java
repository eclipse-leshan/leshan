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

import org.eclipse.leshan.core.parser.StringParser;

/**
 * A Generic Attribute of type Long.
 */
public class LongAttributeModel extends LwM2mAttributeModel<Long> {

    public LongAttributeModel(String coRELinkParam, Attachment attachment, Set<AssignationLevel> assignationLevels,
            AccessMode accessMode, AttributeClass attributeClass) {
        super(coRELinkParam, attachment, assignationLevels, accessMode, attributeClass);
    }

    /**
     * <pre>
     * 1 * DIGIT
     * </pre>
     */
    @Override
    public <E extends Throwable> LwM2mAttribute<Long> consumeAttributeValue(StringParser<E> parser) throws E {
        // parse Value
        int start = parser.getPosition();
        parser.consumeDIGIT();
        while (parser.nextCharIsDIGIT()) {
            parser.consumeNextChar();
        }
        int end = parser.getPosition();

        // create attribute
        String strValue = parser.substring(start, end);
        try {
            return new LwM2mAttribute<Long>(this, Long.parseLong(strValue));
        } catch (NumberFormatException e) {
            parser.raiseException(e, "%s value '%s' is not a valid long in %s", getName(), strValue,
                    parser.getStringToParse());
            return null;
        } catch (IllegalArgumentException e) {
            parser.raiseException(e, "%s value '%s' is not a valid in %s", getName(), strValue,
                    parser.getStringToParse());
            return null;
        }
    }

    @Override
    public LwM2mAttribute<Long> createEmptyAttribute() {
        return new LwM2mAttribute<Long>(this);
    }
}

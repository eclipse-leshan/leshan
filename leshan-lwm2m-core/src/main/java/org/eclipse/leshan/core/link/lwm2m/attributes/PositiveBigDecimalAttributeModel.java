/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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

import java.math.BigDecimal;
import java.util.Set;

import org.eclipse.leshan.core.parser.StringParser;

public class PositiveBigDecimalAttributeModel extends BigDecimalAttributeModel {

    public PositiveBigDecimalAttributeModel(String coRELinkParam, Set<Attachment> attachment, AccessMode accessMode,
            AttributeClass attributeClass) {
        super(coRELinkParam, attachment, accessMode, attributeClass);
    }

    @Override
    public String getInvalidValueCause(BigDecimal value) {
        if (value.signum() < 0) {
            return String.format("'%s' attribute value must not be negative", getName());
        }
        return null;
    }

    /**
     * <pre>
     * 1*DIGIT ["." 1*DIGIT]
     * </pre>
     */
    @Override
    public <E extends Throwable> LwM2mAttribute<BigDecimal> consumeAttributeValue(StringParser<E> parser) throws E {
        // parse Value
        int start = parser.getPosition();
        AttributeParserUtil.consumePositiveDecimalNumber(parser);
        int end = parser.getPosition();

        // create attribute
        String strValue = parser.substring(start, end);
        try {
            return new LwM2mAttribute<>(this, new BigDecimal(strValue));
        } catch (IllegalArgumentException e) {
            parser.raiseException(e, "%s value '%s' is not a valid Positive BigDecimal in %s", getName(), strValue,
                    parser.getStringToParse());
            return null;
        }
    }
}

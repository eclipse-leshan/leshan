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
package org.eclipse.leshan.core.link.attributes;

import org.eclipse.leshan.core.parser.StringParser;

public class DefaultAttributeParser implements AttributeParser {

    @Override
    public Attribute parse(String name, String attributeValue) throws InvalidAttributeException {
        if (attributeValue == null) {
            return new ValuelessAttribute(name);
        }

        StringParser<InvalidAttributeException> parser = new StringParser<InvalidAttributeException>(attributeValue) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidAttributeException {
                throw new InvalidAttributeException(message, cause);
            }
        };

        Attribute attribute = consumeAttributeValue(name, parser);

        if (parser.hasMoreChar()) {
            parser.raiseException("Invalid attributeValue [%s] for [%s]: unexpected characters after [%s]",
                    attributeValue, name, "prout");
        }

        return attribute;
    }

    /**
     * consume Attribute as defined in RFC6690 (https://datatracker.ietf.org/doc/html/RFC6690#section-2):
     * 
     * <pre>
     * {@code
     * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
     * }
     * </pre>
     */
    @Override
    public <T extends Throwable> Attribute consumeAttributeValue(String parmName, StringParser<T> parser) throws T {
        // for now we support only ptoken and quoted-string
        if (parser.nextCharIs('\"')) {
            return QuotedStringAttribute.consumeQuotedString(parmName, parser);
        } else {
            return UnquotedStringAttribute.consumePToken(parmName, parser);
        }
    }

}

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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.parser.StringParser;
import org.eclipse.leshan.core.util.Validate;

public class DefaultAttributeParser implements AttributeParser {

    private Map<String, AttributeModel<?>> knownAttributes;

    public DefaultAttributeParser() {
        this(Attributes.ALL);
    }

    public DefaultAttributeParser(Collection<? extends AttributeModel<?>> knownAttributes) {
        Validate.notNull(knownAttributes);
        this.knownAttributes = new HashMap<>();
        for (AttributeModel<?> attributeModel : knownAttributes) {
            AttributeModel<?> previous = this.knownAttributes.put(attributeModel.getName(), attributeModel);
            if (previous != null) {
                throw new IllegalStateException(
                        String.format("Duplicate models for attribute name [%s]", attributeModel.getName()));
            }
        }
    }

    public Map<String, AttributeModel<?>> getKnownAttributes() {
        return knownAttributes;
    }

    @Override
    public Attribute parseCoreLinkValue(String name, String attributeValue) throws InvalidAttributeException {
        // handle attribute without value
        if (attributeValue == null) {
            return createEmptyAttribute(name);
        }

        // handle attribute with value
        StringParser<InvalidAttributeException> parser = new StringParser<InvalidAttributeException>(attributeValue) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidAttributeException {
                throw new InvalidAttributeException(message, cause);
            }
        };

        Attribute attribute = consumeAttributeValue(name, parser);

        if (parser.hasMoreChar()) {
            parser.raiseException("Invalid attributeValue [%s] for [%s]: unexpected characters after [%s]",
                    attributeValue, name, parser.getAlreadyParsedString());
        }

        return attribute;
    }

    @Override
    public Attribute createEmptyAttribute(String name) throws InvalidAttributeException {
        // search in known attribute
        AttributeModel<?> model = knownAttributes.get(name);
        if (model != null) {
            if (model.linkAttributeCanBeValueless()) {
                return model.createEmptyAttribute();
            } else {
                throw new InvalidAttributeException("Attribute %s must have a value in CoRE Link Format.",
                        model.getName());
            }
        }
        // ELSE fall-back on ValueLessAttribute
        else {
            return new ValuelessAttribute(name);
        }
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
        // search in known attribute
        AttributeModel<?> model = knownAttributes.get(parmName);
        if (model != null) {
            return model.consumeAttributeValue(parser);
        }
        // ELSE fall-back on quoted-string or ptoken
        //
        // quoted-String
        else if (parser.nextCharIs('\"')) {
            return QuotedStringAttribute.consumeQuotedString(parmName, parser);
        }
        // ptoken
        else {
            return UnquotedStringAttribute.consumePToken(parmName, parser);
        }
    }

}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.leshan.core.parser.StringParser;
import org.eclipse.leshan.core.util.Validate;

/**
 * A Resource Type attribute as described at https://datatracker.ietf.org/doc/html/rfc6690#section-3.1
 */
public class ResourceTypeAttribute extends BaseAttribute {

    public static ResourceTypeAttributeModel MODEL = new ResourceTypeAttributeModel();

    public ResourceTypeAttribute(String... resourceTypes) {
        super(MODEL.getName(), Collections.unmodifiableList(Arrays.asList(resourceTypes)), true);
        Validate.notEmpty(resourceTypes);
    }

    public ResourceTypeAttribute(Collection<String> resourceTypes) {
        super(MODEL.getName(), Collections.unmodifiableList(new ArrayList<>(resourceTypes)), true);
    }

    @Override
    protected void validate() {
        // We don't call super.validate() because we don't need to validate name.
        // super.validate();
        Validate.notEmpty(getValue());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getValue() {
        return (List<String>) super.getValue();
    }

    @Override
    public String getCoreLinkValue() {
        Collection<String> rts = getValue();
        StringBuilder b = new StringBuilder();
        b.append("\"");
        Iterator<String> it = rts.iterator();
        b.append(it.next());
        while (it.hasNext()) {
            b.append(" ");
            b.append(it.next());
        }
        b.append("\"");
        return b.toString();
    }

    public static class ResourceTypeAttributeModel extends AttributeModel<ResourceTypeAttribute> {

        public ResourceTypeAttributeModel() {
            super("rt");
        }

        /**
         * Consume a 'rt' attribute value as described in https://datatracker.ietf.org/doc/html/rfc6690#section-2.
         * <p>
         * Grammar:
         *
         * <pre>
         * "rt" "=" relation-types
         *
         * relation-types = relation-type
         *         / DQUOTE relation-type *( 1*SP relation-type ) DQUOTE
         * </pre>
         */
        @Override
        public <T extends Throwable> ResourceTypeAttribute consumeAttributeValue(StringParser<T> parser) throws T {
            // relation-type
            if (!parser.nextCharIs('\"')) {
                String relationType = consumeRelationType(parser);
                return new ResourceTypeAttribute(relationType);
            }
            // DQUOTE relation-type *( 1*SP relation-type ) DQUOTE
            else {
                List<String> rts = new ArrayList<>();
                // consume DQUOTE
                parser.consumeNextChar();
                String relationType = consumeRelationType(parser);
                rts.add(relationType);
                while (parser.nextCharIs(' ')) {
                    // consume SP
                    parser.consumeNextChar();
                    relationType = consumeRelationType(parser);
                    rts.add(relationType);
                }
                parser.consumeChar('\"');
                return new ResourceTypeAttribute(rts);
            }
        }

        /**
         * Consume relation-type as described in https://datatracker.ietf.org/doc/html/rfc6690#section-2.
         * <p>
         * OFFICIAL Grammar:
         *
         * <pre>
         * relation-type  = reg-rel-type / ext-rel-type
         * reg-rel-type   = LOALPHA *( LOALPHA / DIGIT / "." / "-" )
         * ext-rel-type   = URI
         * URI            = {@code <defined in [RFC3986]>}
         * </pre>
         *
         * Parsing URI could be a real challenge, so we implement a more simple grammar which could be too flexible...
         * <p>
         * IMPLEMENTED Grammar :
         *
         * <pre>
         * relation-type  = reg-rel-type
         * reg-rel-type   = LOALPHA *( LOALPHA / DIGIT / "." / "-" )
         * </pre>
         */
        protected <T extends Throwable> String consumeRelationType(StringParser<T> parser) throws T {
            int start = parser.getPosition();
            parser.consumeLOALPHA();
            while (parser.nextCharIsLOALPHA() || parser.nextCharIsDIGIT() || parser.nextCharIs('.')
                    || parser.nextCharIs('-')) {
                parser.consumeNextChar();
            }
            int end = parser.getPosition();
            String relationType = parser.substring(start, end);
            return relationType;
        }
    }
}

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
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Validate;

/**
 * A ContentFormat Attribute as defined in https://datatracker.ietf.org/doc/html/rfc7252#section-7.2.1
 */
public class ContentFormatAttribute extends BaseAttribute {

    public static ContentFormatAttributeModel MODEL = new ContentFormatAttributeModel();

    public ContentFormatAttribute(ContentFormat... contentFormats) {
        super(MODEL.getName(), Collections.unmodifiableList(Arrays.asList(contentFormats)), true);
    }

    public ContentFormatAttribute(Collection<ContentFormat> contentFormats) {
        super(MODEL.getName(), Collections.unmodifiableList(new ArrayList<>(contentFormats)), true);
    }

    @Override
    protected void validate() {
        // We don't call super.validate() because we don't need to validate name.
        // super.validate();
        Validate.notEmpty(getValue());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ContentFormat> getValue() {
        return (List<ContentFormat>) super.getValue();
    }

    @Override
    public String getCoreLinkValue() {
        Collection<ContentFormat> cts = getValue();
        if (cts.size() == 1) {
            return Integer.toString(cts.iterator().next().getCode());
        } else {
            StringBuilder b = new StringBuilder();
            b.append("\"");
            Iterator<ContentFormat> it = cts.iterator();
            b.append(it.next().getCode());
            while (it.hasNext()) {
                b.append(" ");
                b.append(it.next().getCode());
            }
            b.append("\"");
            return b.toString();
        }
    }

    public static class ContentFormatAttributeModel extends AttributeModel<ContentFormatAttribute> {

        public ContentFormatAttributeModel() {
            super("ct");
        }

        /**
         * Consume a 'ct' attribute value as described in :
         * <ul>
         * <li>https://datatracker.ietf.org/doc/html/rfc7252#section-7.2.1</li>
         * <li>https://datatracker.ietf.org/doc/html/rfc6690#section-2</li>
         * </ul>
         * Grammar:
         *
         * <pre>
         *  ct-value =  cardinal
         *              /  DQUOTE cardinal *( 1*SP cardinal ) DQUOTE
         *
         *  cardinal       = "0" / ( %x31-39 *DIGIT )
         * </pre>
         */
        @Override
        public <T extends Throwable> ContentFormatAttribute consumeAttributeValue(StringParser<T> parser) throws T {
            // cardinal
            if (!parser.nextCharIs('\"')) {
                String cardinal = parser.consumeCardinal();
                return new ContentFormatAttribute(ContentFormat.fromCode(cardinal));
            }
            // DQUOTE cardinal *( 1*SP cardinal ) DQUOTE
            else {
                List<ContentFormat> cts = new ArrayList<>();
                // consume DQUOTE
                parser.consumeNextChar();
                String cardinal = parser.consumeCardinal();
                cts.add(ContentFormat.fromCode(cardinal));
                while (parser.nextCharIs(' ')) {
                    // consume SP
                    parser.consumeNextChar();
                    cardinal = parser.consumeCardinal();
                    cts.add(ContentFormat.fromCode(cardinal));
                }
                parser.consumeChar('\"');
                return new ContentFormatAttribute(cts);
            }
        }
    }
}

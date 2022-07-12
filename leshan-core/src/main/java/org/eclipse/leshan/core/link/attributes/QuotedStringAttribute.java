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
import org.eclipse.leshan.core.util.Validate;

/**
 * a String Attribute described as quoted-string in RFC2616.
 * <p>
 * See <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-2.2"> rfc2616#section-2.2</a>.
 *
 * <pre>
 * {@code
 * quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
 * qdtext         = <any TEXT except <">>
 * quoted-pair    = "\" CHAR
 * }
 * </pre>
 *
 * About quoted-pair we only auto-escaped/unescaped {@code <">} char to have a lossless conversion.
 */
public class QuotedStringAttribute extends BaseAttribute {

    public QuotedStringAttribute(String name, String value) {
        super(name, value, true);
    }

    public QuotedStringAttribute(String name, String value, boolean validate) {
        super(name, value, validate);
    }

    @Override
    protected void validate() {
        super.validate();
        Validate.notEmpty(getValue());
    }

    @Override
    public String getValue() {
        return (String) super.getValue();
    }

    @Override
    public String getCoreLinkValue() {
        String valueEscaped = getValue().replace("\"", "\\\"");
        return "\"" + valueEscaped + "\"";
    }

    /**
     * Validate a quoted-string with rules (subset of RFC2616
     * (https://datatracker.ietf.org/doc/html/rfc2616#section-2.2)):
     *
     * <pre>
     * {@code
     * quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
     * qdtext         = <any TEXT except <">>
     * quoted-pair    = "\" CHAR
     * }
     * </pre>
     */
    public static <T extends Throwable> String consumeQuotedString(StringParser<T> parser) throws T {
        parser.consumeChar('\"');
        int start = parser.getPosition();
        while (!parser.nextCharIs('\"')) {
            if (!parser.hasMoreChar()) {
                // missing ending quote
                parser.raiseException("Unable to parse [%s] : missing ending quote to '%s'", parser.getStringToParse(),
                        parser.substring(start - 1, parser.getPosition()));
            }
            // handle escaping
            if (parser.nextCharIs('\\')) {
                // consume \ and escaped char
                parser.consumeNextChar();
                parser.consumeNextChar();
            } else {
                parser.consumeNextChar();
            }
        }
        int end = parser.getPosition();
        parser.consumeChar('\"');

        String unquotedValue = parser.substring(start, end);
        String valueUnescaped = unquotedValue.replace("\\\"", "\"");
        return valueUnescaped;
    }

    /**
     * @see QuotedStringAttribute#consumeQuotedString(StringParser)
     */
    public static <T extends Throwable> QuotedStringAttribute consumeQuotedString(String parmName,
            StringParser<T> parser) throws T {
        String value = consumeQuotedString(parser);
        return new QuotedStringAttribute(parmName, value, false);
    }
}

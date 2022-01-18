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

/**
 * A String Attribute described as ptoken in RFC6690.
 * <p>
 * See <a href="https://datatracker.ietf.org/doc/html/RFC6690#section-2"> RFC6690#section-2</a>.
 * 
 * <pre>
 * ptoken         = 1*ptokenchar
 * ptokenchar     = "!" / "#" / "$" / "%" / "{@code &}" / "'" / "("
 *                    / ")" / "*" / "+" / "-" / "." / "/" / DIGIT
 *                    / ":" / "{@code <}" / "=" / "{@code >}" / "?" / "@" / ALPHA
 *                    / "[" / "]" / "^" / "_" / "`" / "{" / "|"
 *                    / "}" / "~"
 * </pre>
 */
public class UnquotedStringAttribute extends BaseAttribute {

    public UnquotedStringAttribute(String name, String value) {
        super(name, value);
        // TODO add validation
    }

    @Override
    public String getValue() {
        return (String) super.getValue();
    }

    @Override
    public String getStringValue() {
        return getValue();
    }

    @Override
    public String getCoreLinkValue() {
        return getValue();
    }

    /**
     * Validate ptoken with rules (subset of RFC6690 (https://datatracker.ietf.org/doc/html/RFC6690#section-2)):
     * 
     * <pre>
     * ptoken         = 1*ptokenchar
     * ptokenchar     = "!" / "#" / "$" / "%" / "{@code &}" / "'" / "("
     *                    / ")" / "*" / "+" / "-" / "." / "/" / DIGIT
     *                    / ":" / "{@code <}" / "=" / "{@code >}" / "?" / "@" / ALPHA
     *                    / "[" / "]" / "^" / "_" / "`" / "{" / "|"
     *                    / "}" / "~"
     * </pre>
     */
    public static <T extends Throwable> Attribute consumePToken(String parmName, StringParser<T> parser) throws T {
        // loop for ptokenchar
        int start = parser.getPosition();
        while (parser.nextCharIsALPHA() || parser.nextCharIsDIGIT()
                || parser.nextCharIsIn("!#$%&'()*+-./:<=>?@[]^_`{|}~")) {
            parser.consumeNextChar();
        }
        int end = parser.getPosition();

        // get parmName
        String ptoken = parser.substring(start, end);

        // check parmName is at least 1 char length
        if (ptoken.length() == 0) {
            parser.raiseException("Unable to parse [%s] : ptoken should not be empty after %s",
                    parser.getStringToParse(), parser.getAlreadyParsedString());
        }
        return new UnquotedStringAttribute(parmName, ptoken);
    }
}

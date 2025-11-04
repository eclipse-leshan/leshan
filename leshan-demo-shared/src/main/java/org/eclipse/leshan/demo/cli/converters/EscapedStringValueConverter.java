/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.demo.cli.converters;

import java.util.HashSet;
import java.util.Set;

import picocli.CommandLine.ITypeConverter;

/**
 * Converter which unescapes String.
 */
public class EscapedStringValueConverter implements ITypeConverter<String> {

    private final char escapedChar;
    private final Set<Character> escapableChar;

    /**
     * @param escapedChar the character used to escaped other char (e.g. '/')
     * @param escapableEscapedChar <code>true</code> if espaced character can be escaped too (e.g. if true then "\\"
     *        will be replaced by "\")
     * @param escapableChar list of character which can be escapable (e.g. if [','] then "\,\m" will be replaced by
     *        ",\m")
     */
    public EscapedStringValueConverter(char escapedChar, boolean escapableEscapedChar, char... escapableChar) {
        this.escapedChar = escapedChar;

        this.escapableChar = new HashSet<>();
        for (char c : escapableChar) {
            this.escapableChar.add(c);
        }
        if (escapableEscapedChar) {
            this.escapableChar.add(escapedChar);
        }
    }

    @Override
    public String convert(String value) throws Exception {
        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuilder sb = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (escaping) {
                // If previous char was escape char, check if this char is escapable
                if (escapableChar.contains(c)) {
                    sb.append(c);
                } else {
                    // Keep the escape char if this char is not escapable
                    sb.append('\\').append(c);
                }
                escaping = false;
            } else if (c == escapedChar) {
                escaping = true; // next char might be escaped
            } else {
                sb.append(c);
            }
        }

        // Handle dangling escaped char at end
        if (escaping) {
            sb.append(escapedChar);
        }
        return sb.toString();
    }
}

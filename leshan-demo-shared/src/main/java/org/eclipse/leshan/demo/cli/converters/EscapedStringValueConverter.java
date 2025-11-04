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

public class EscapedStringValueConverter implements ITypeConverter<String> {

    private final char escapedChar;
    private final Set<Character> escapableChar;

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
        System.out.println(value);

        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuilder sb = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (escaping) {
                // If previous char was backslash, check if this char is escapable
                if (escapableChar.contains(c)) {
                    sb.append(c);
                } else {
                    // Keep the backslash if this char is not escapable
                    sb.append('\\').append(c);
                }
                escaping = false;
            } else if (c == escapedChar) {
                escaping = true; // next char might be escaped
            } else {
                sb.append(c);
            }
        }

        // Handle dangling espaced char at end
        if (escaping) {
            sb.append(escapedChar);
        }

        System.out.println(sb.toString());
        return sb.toString();
    }
}

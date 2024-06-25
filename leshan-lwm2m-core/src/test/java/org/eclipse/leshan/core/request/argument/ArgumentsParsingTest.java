/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - Add better support for Arguments of Execute Operation.
 *******************************************************************************/
package org.eclipse.leshan.core.request.argument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ArgumentsParsingTest {

    @Test
    public void should_parse_text_into_arguments() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3='stringValue'");

        assertNotNull(arguments);
        assertEquals(1, arguments.size());
        assertTrue(arguments.hasDigit(3));
        assertEquals(new Argument(3, "stringValue"), arguments.get(3));
    }

    @Test
    public void should_parse_text_into_arguments2() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3='stringValue',4");

        assertNotNull(arguments);
        assertEquals(2, arguments.size());
        assertEquals(new Argument(3, "stringValue"), arguments.get(3));
        assertEquals(new Argument(4, null), arguments.get(4));
    }

    @Test
    public void should_not_parse_arguments_with_same_digits() {
        assertThrowsExactly(InvalidArgumentException.class, () -> {
            Arguments.parse("4,4");
        });
    }

    @Test
    public void should_parse_null_into_arguments() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse(null);

        assertNotNull(arguments);
        assertEquals(0, arguments.size());
    }

    @Test
    public void should_parse_empty_text_into_arguments() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("");

        assertNotNull(arguments);
        assertEquals(0, arguments.size());
    }

    @Test
    public void should_parse_text_into_argument_with_empty_value() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3=''");

        assertNotNull(arguments);
        assertEquals(1, arguments.size());
        assertEquals(3, arguments.get(3).getDigit());
        assertEquals("", arguments.get(3).getValue());
    }

    @Test
    public void should_parse_text_into_argument_with_comma_value() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3=',',4");

        assertNotNull(arguments);
        assertEquals(2, arguments.size());
        assertEquals(3, arguments.get(3).getDigit());
        assertEquals(",", arguments.get(3).getValue());

        assertEquals(4, arguments.get(4).getDigit());
        assertNull(arguments.get(4).getValue());
    }

    @Test
    public void should_parse_text_into_argument_with_equal_value() throws InvalidArgumentException {
        Arguments arguments = Arguments.parse("3='=',4");

        assertNotNull(arguments);
        assertEquals(2, arguments.size());
        assertEquals(3, arguments.get(3).getDigit());
        assertEquals("=", arguments.get(3).getValue());

        assertEquals(4, arguments.get(4).getDigit());
        assertNull(arguments.get(4).getValue());
    }

    @Test
    public void should_not_parse_non_digit_into_argument() throws InvalidArgumentException {
        assertThrowsExactly(InvalidArgumentException.class, () -> {
            Arguments.parse("a='hello'");
        });
    }

    @Test
    public void should_not_parse_text_into_argument_with_value_without_equal() throws InvalidArgumentException {
        assertThrowsExactly(InvalidArgumentException.class, () -> {
            Arguments.parse("3'hello'");
        });
    }

    @Test
    public void should_not_parse_text_into_argument_with_value_without_quotes() throws InvalidArgumentException {
        assertThrowsExactly(InvalidArgumentException.class, () -> {
            Arguments.parse("3=hello");
        });
    }

    @Test
    public void should_not_parse_unquoted_value() throws InvalidArgumentException {
        assertThrowsExactly(InvalidArgumentException.class, () -> {
            Arguments.parse("3=string,4='Value'");
        });
    }

}

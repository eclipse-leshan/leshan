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
package org.eclipse.leshan.core.request.execute;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ArgumentsTest {

    @Test
    public void should_create_map_from_arguments() throws InvalidArgumentException {
        Arguments arguments = Arguments.builder() //
                .addArgument(3, "stringValue") //
                .addArgument(4) //
                .build();

        Map<Integer, String> argumentsMap = arguments.toMap();

        assertEquals(2, argumentsMap.size());
        assertEquals("stringValue", argumentsMap.get(3));
        assertNull(argumentsMap.get(4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_if_get_argument_with_nonexisting_digit() throws InvalidArgumentException {
        Arguments arguments = Arguments.builder() //
                .addArgument(4) //
                .build();

        arguments.get(5);
    }

    @Test
    public void should_allow_to_create_empty_arguments() throws InvalidArgumentException {
        Map<Integer, String> argumentsMap = Arguments.builder().build().toMap();

        assertEquals(0, argumentsMap.size());
    }

    @Test
    public void should_get_arguments_digits() throws InvalidArgumentException {
        Arguments arguments = Arguments.builder() //
                .addArgument(3, "stringValue") //
                .addArgument(4) //
                .build();

        Set<Integer> digits = arguments.getDigits();
        assertEquals(2, digits.size());
        Iterator<Integer> iterator = digits.iterator();
        assertEquals(Integer.valueOf(3), iterator.next());
        assertEquals(Integer.valueOf(4), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void should_get_arguments_values() throws InvalidArgumentException {
        Arguments arguments = Arguments.builder() //
                .addArgument(3, "stringValue") //
                .addArgument(4) //
                .build();

        Collection<Argument> values = arguments.getValues();
        assertEquals(2, values.size());
        Iterator<Argument> iterator = values.iterator();
        assertEquals(new Argument(3, "stringValue"), iterator.next());
        assertEquals(new Argument(4), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test(expected = InvalidArgumentException.class)
    public void should_forbid_arguments_with_the_same_digit() throws InvalidArgumentException {
        Arguments.builder() //
                .addArgument(3, "stringValue") //
                .addArgument(3, "stringValue") //
                .build();
    }
}
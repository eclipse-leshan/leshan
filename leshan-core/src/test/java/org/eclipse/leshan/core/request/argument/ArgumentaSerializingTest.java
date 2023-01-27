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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class ArgumentaSerializingTest {

    @Test
    public void should_serialize_arguments_into_text() throws InvalidArgumentException {
        String content = Arguments.builder() //
                .addArgument(3, "stringValue") //
                .build().serialize();

        assertEquals("3='stringValue'", content);
    }

    @Test
    public void should_serialize_multiple_arguments_into_text() throws InvalidArgumentException {
        String content = Arguments.builder() //
                .addArgument(3, "stringValue") //
                .addArgument(4) //
                .build().serialize();

        assertEquals("3='stringValue',4", content);
    }

    @Test
    public void should_serialize_argument_with_empty_string_value_into_text() throws InvalidArgumentException {
        String content = Arguments.builder() //
                .addArgument(3, "") //
                .build().serialize();

        assertEquals("3=''", content);
    }

    @Test
    public void should_serialize_as_null_empty_arguments() throws InvalidArgumentException {
        String content = Arguments.builder() //
                .build().serialize();

        assertNull(content);
    }
}

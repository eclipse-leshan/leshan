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

import static org.junit.Assert.assertEquals;

import java.util.Map;

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
        assertEquals("", argumentsMap.get(4));
    }

    @Test
    public void should_allow_to_create_empty_arguments() throws InvalidArgumentException {
        Map<Integer, String> argumentsMap = Arguments.builder().build().toMap();

        assertEquals(0, argumentsMap.size());
    }
}
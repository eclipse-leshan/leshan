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

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ArgumentValidationTest {

    static Stream<org.junit.jupiter.params.provider.Arguments> linkValueList() {
        return Stream.of(//
                arguments(0, "hello", null), //
                arguments(9, "hello", null), //
                arguments(-1, "hello", InvalidArgumentException.class), //
                arguments(10, "hello", InvalidArgumentException.class), //
                arguments(1, null, null), //
                arguments(1, "", null), //
                arguments(1,
                        "!" + new String(new byte[] { 0x23, 0x26, 0x28, 0x5B, 0x5D, 0x7E }, StandardCharsets.UTF_8),
                        null), //
                arguments(1, new String(new byte[] { 0x22 }, StandardCharsets.UTF_8), InvalidArgumentException.class),
                // " character
                arguments(1, new String(new byte[] { 0x27 }, StandardCharsets.UTF_8), InvalidArgumentException.class),
                // ' character
                arguments(1, new String(new byte[] { 0x5C }, StandardCharsets.UTF_8), InvalidArgumentException.class),
                // \ character
                arguments(1, new String(new byte[] { 0x7F }, StandardCharsets.UTF_8), InvalidArgumentException.class),
                // DEL character
                arguments(1, "`aAzZ190-=~!@#$%^&*()_+[]{}|;:<>/?,.", null) // more visualized character rules above
        );

    }

    @ParameterizedTest(name = "{index} : digit: {0}, value: {1}, expected exception: {2}")
    @MethodSource("linkValueList")
    public void perform_tests(int digit, String value, Class<? extends Throwable> exception)
            throws InvalidArgumentException {
        if (exception != null) {
            assertThrowsExactly(InvalidArgumentException.class, () -> {
                new Argument(digit, value);
            });
        } else {
            new Argument(digit, value);
        }
    }

}

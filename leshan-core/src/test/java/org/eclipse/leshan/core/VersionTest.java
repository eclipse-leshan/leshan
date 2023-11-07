/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class VersionTest {
    @Test
    public void is_supported_tests() {
        assertTrue(LwM2mVersion.isSupported("1.0"));
        assertTrue(LwM2mVersion.isSupported("1.1"));
        assertFalse(LwM2mVersion.isSupported("1.2"));
        assertFalse(LwM2mVersion.isSupported(""));
        assertFalse(LwM2mVersion.isSupported(null));
    }

    @Test
    public void compare_to_tests() {
        assertTrue(new Version("1.0").compareTo(new Version("1.2")) < 0);
        assertTrue(new Version("0.9").compareTo(new Version("1.2")) < 0);
        assertTrue(new Version("128.0").compareTo(new Version("128.2")) < 0);
        assertTrue(new Version("1.2").compareTo(new Version("1.2")) == 0);
        assertTrue(new Version("128.0").compareTo(new Version("128.0")) == 0);
        assertTrue(new Version("1.3").compareTo(new Version("1.2")) > 0);
        assertTrue(new Version("2.0").compareTo(new Version("1.2")) > 0);
        assertTrue(new Version("128.2").compareTo(new Version("128.0")) > 0);
    }

    @Test
    public void equals_tests() {
        assertEquals(Version.V1_0, Version.V1_0);
        assertEquals(new Version("1.1"), new Version("1.1"));
    }

    @Test
    public void not_equals_tests() {
        assertNotEquals(new Version("1.0"), null);
        assertNotEquals(new Version("1.0"), "");
        assertNotEquals(new Version("1.0"), new Version("1.1"));
        assertNotEquals(new Version("1.0"), new Version("0.9"));
    }

    @Test
    public void hash_code_tests() {
        assertEquals(new Version("1.1").hashCode(), new Version("1.1").hashCode());
        assertNotEquals(new Version("1.0").hashCode(), new Version("1.1").hashCode());
    }

    @Test
    public void older_than_tests() {
        assertTrue(new Version("1.0").olderThan(new Version("1.1")));
        assertTrue(new Version("0.9").olderThan(new Version("1.1")));
    }

    @Test
    public void newer_than_tests() {
        assertTrue(new Version("1.1").newerThan(new Version("1.0")));
        assertTrue(new Version("1.0").newerThan(new Version("0.9")));
    }

    @ParameterizedTest
    @MethodSource("illegal_arguments")
    public void illegal_argument_tests(Executable executable, String expectedMessage) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(expectedMessage, e.getMessage());
    }

    private static Stream<Arguments> illegal_arguments() {
        return Stream.of(args(() -> new Version(null), "version MUST NOT be null or empty"),
                args(() -> new Version(""), "version MUST NOT be null or empty"),
                args(() -> new Version("foo"), "version (foo) MUST be composed of 2 parts"),
                args(() -> new Version("1.0.0"), "version (1.0.0) MUST be composed of 2 parts"),
                args(() -> new Version("-1.0"), "version (-1.0) part 1 (-1) must not be negative"),
                args(() -> new Version("1.-1"), "version (1.-1) part 2 (-1) must not be negative"),
                args(() -> new Version("a.0"), "version (a.0) part 1 (a) is not a valid short"),
                args(() -> new Version("32768.32767"), "version (32768.32767) part 1 (32768) is not a valid short"),
                args(() -> new Version("32767.32768"), "version (32767.32768) part 2 (32768) is not a valid short"),
                args(() -> new Version(-32769, -32768),
                        "version (-32769.-32768) major part (-32769) is not a valid short"),
                args(() -> new Version(-32768, -32769),
                        "version (-32768.-32769) minor part (-32769) is not a valid short"),
                args(() -> new Version(32768, 32767), "version (32768.32767) major part (32768) is not a valid short"),
                args(() -> new Version(32767, 32768), "version (32767.32768) minor part (32768) is not a valid short"),
                args(() -> new Version(-1, 0), "version (-1.0) major part (-1) must not be negative"),
                args(() -> new Version(1, -1), "version (1.-1) minor part (-1) must not be negative"),
                args(() -> new Version((short) -1, (short) 0), "version (-1.0) major part (-1) must not be negative"),
                args(() -> new Version((short) 1, (short) -1), "version (1.-1) minor part (-1) must not be negative"));
    }

    private static Arguments args(Executable executable, String expectedMessage) {
        return Arguments.of(executable, expectedMessage);
    }
}

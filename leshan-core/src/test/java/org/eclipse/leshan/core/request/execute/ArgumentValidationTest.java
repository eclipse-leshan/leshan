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

import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ArgumentValidationTest {

    @Parameterized.Parameters(name = "{index} : {0}")
    public static Collection<?> linkValueListProvider() {
        return Arrays.asList( //
                new TestParams(0, "hello", null), //
                new TestParams(9, "hello", null), //
                new TestParams(-1, "hello", InvalidArgumentException.class), //
                new TestParams(10, "hello", InvalidArgumentException.class), //
                new TestParams(1, null, null), //
                new TestParams(1, "", null), //
                new TestParams(1,
                        "!" + new String(new byte[] { 0x23, 0x26, 0x28, 0x5B, 0x5D, 0x7E }, StandardCharsets.UTF_8),
                        null), //
                new TestParams(1, new String(new byte[] { 0x22 }, StandardCharsets.UTF_8),
                        InvalidArgumentException.class), // " character
                new TestParams(1, new String(new byte[] { 0x27 }, StandardCharsets.UTF_8),
                        InvalidArgumentException.class), // ' character
                new TestParams(1, new String(new byte[] { 0x5C }, StandardCharsets.UTF_8),
                        InvalidArgumentException.class), // \ character
                new TestParams(1, new String(new byte[] { 0x7F }, StandardCharsets.UTF_8),
                        InvalidArgumentException.class), // DEL character
                new TestParams(1, "`aAzZ190-=~!@#$%^&*()_+[]{}|;:<>/?,.", null) // more visualized character rules above
        );
    }

    private static class TestParams {
        private final int digit;
        private final String value;
        private final Class<? extends Throwable> exception;

        public TestParams(int digit, String value, Class<? extends Throwable> exception) {
            this.digit = digit;
            this.value = value;
            this.exception = exception;
        }

        @Override
        public String toString() {
            if (exception != null) {
                return "new Argument(" + digit + ", \"" + value + "\"): expects " + exception.getSimpleName()
                        + " exception";
            } else {
                return "new Argument(" + digit + ", \"" + value + "\")";
            }
        }
    }

    private final TestParams testParams;

    public ArgumentValidationTest(TestParams testParams) {
        this.testParams = testParams;
    }

    @Test
    public void perform_tests() throws InvalidArgumentException {
        if (testParams.exception != null) {
            assertThrows(testParams.exception, new ThrowingRunnable() {
                @Override
                public void run() throws InvalidArgumentException {
                    new Argument(testParams.digit, testParams.value);
                }
            });
        } else {
            new Argument(testParams.digit, testParams.value);
        }
    }

}
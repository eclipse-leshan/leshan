/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util.assertion;

import java.util.function.Consumer;

import org.assertj.core.matcher.AssertionMatcher;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.mockito.hamcrest.MockitoHamcrest;

public class Assertions extends org.assertj.core.api.Assertions {
    public static <T> T assertArg(Consumer<T> assertions) {
        return MockitoHamcrest.argThat(new AssertionMatcher<T>() {
            @Override
            public void assertion(T actual) throws AssertionError {
                assertions.accept(actual);
            }
        });
    }

    // give access to TolkienCharacter Race assertion
    public static LeshanTestClientAssert assertThat(LeshanTestClient actual) {
        return new LeshanTestClientAssert(actual);
    }
}

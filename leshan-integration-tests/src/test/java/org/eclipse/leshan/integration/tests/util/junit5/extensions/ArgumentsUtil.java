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
package org.eclipse.leshan.integration.tests.util.junit5.extensions;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

public class ArgumentsUtil {
    public static Arguments[] combine(Object[] t1, Object[][] t2) {
        Arguments[] r = new Arguments[t1.length * t2.length];

        int i = 0;
        for (int j = 0; j < t1.length; j++) {
            for (int k = 0; k < t2.length; k++) {
                r[i] = Arguments.of(Stream.concat(Stream.of(t1[j]), Stream.of(t2[k])).toArray());
                i++;
            }
        }
        return r;
    }

    public static Arguments[] combine(Object[][] t1, Object[][] t2) {
        Arguments[] r = new Arguments[t1.length * t2.length];

        int i = 0;
        for (int j = 0; j < t1.length; j++) {
            for (int k = 0; k < t2.length; k++) {
                r[i] = Arguments.of(Stream.concat(Stream.of(t1[j]), Stream.of(t2[k])).toArray());
                i++;
            }
        }
        return r;
    }
}

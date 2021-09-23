/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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

import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class LinkObjectValidationTest {

    @Parameterized.Parameters()
    public static Collection<?> senMLJsonencoderDecoder() {
        return Arrays.asList(new Object[][] {
                { "</" },
                { "<//>" },
                { "//>" },
                { "</>," },
                { "</>;" },
                { "</foo>;pąrąm" },
                { "</foo>;param=ą" },
                { "</foo>;param=\"bar" },
                { "</foo>;param=\"bar\\\"" },
                { "</>;=" },
                { "</>;param=" },
                { "</>; param=123" },
                { "</> ;param=123" },
                { "</>;param =123" },
                { "</>;param= 123" },
                { "</>;param=123 " },
                { "</>, </>" },
                { "</> ,</>" },
                { " </>,</>" },
                { "</>,</> " }
        });
    }

    private String uri;

    public LinkObjectValidationTest(String uri) {
        this.uri = uri;
    }

    @Test
    public void parse_invalid_formats() {
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                Link.parse(uri.getBytes());
            }
        });
    }

}

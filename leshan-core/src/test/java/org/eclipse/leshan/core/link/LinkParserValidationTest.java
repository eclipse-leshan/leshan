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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.core.link;

import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class LinkParserValidationTest {

    private final LinkParser parser = new DefaultLinkParser();

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<?> linkValueListProvider() {
        return Arrays.asList( //
                "<file:///etc/hosts>", //
                "</hosts?query>", //
                "</hosts#hash>", //
                "</%>", //
                "</%a>", //
                "</%1g>", //
                "</fóó>", //
                "</foo>;pąrąm", //
                "</foo>;param=ą", //
                "</foo>;param=\"bar", //
                "</foo>;param=\"bar\\\"", //
                "</>;=", //
                "</>;param=", //
                "</>; param=123", //
                "</> ;param=123", //
                "</>;param =123", //
                "</>;param= 123", //
                "</>;param=123 ", //
                "<>", //
                "</", //
                "<//>", //
                "//>", //
                "</>,", //
                "</>;", //
                "</>, </>", //
                "</> ,</>", //
                " </>,</>", //
                "</>,</> " //
        );
    }

    private String linkValueList;

    public LinkParserValidationTest(String linkValueList) {
        this.linkValueList = linkValueList;
    }

    @Test
    public void parse_invalid_formats() {
        assertThrows(LinkParseException.class, new ThrowingRunnable() {
            @Override
            public void run() throws LinkParseException {
                parser.parseCoreLinkFormat(linkValueList.getBytes(StandardCharsets.UTF_8));
            }
        });
    }

}

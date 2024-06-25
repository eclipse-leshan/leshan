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

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class LinkParserValidationTest {

    private final LinkParser parser = new DefaultLinkParser();

    @ParameterizedTest
    @ValueSource(strings = { //
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
    })
    public void parse_invalid_formats(String linkValueList) {
        assertThrowsExactly(LinkParseException.class, () -> {
            parser.parseCoreLinkFormat(linkValueList.getBytes(StandardCharsets.UTF_8));
        });
    }
}

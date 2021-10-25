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
 *     Orange - Make LinkParser extensible.
 *******************************************************************************/
package org.eclipse.leshan.core.link;

import static org.junit.Assert.*;

import org.eclipse.leshan.core.link.linkvalue.DefaultUriReferenceParser;
import org.eclipse.leshan.core.link.linkvalue.UriReferenceParser;
import org.junit.Assert;
import org.junit.Test;

public class DefaultUriReferenceParserTest {

    @Test
    public void parse_uri_reference() throws LinkParseException {
        UriReferenceParser parser = new DefaultUriReferenceParser();

        validateParser(parser, "</uri>", "/uri");
        validateParser(parser, "</uri/>", "/uri/");
        validateParser(parser, "</uri//>", "/uri//");
        validateParser(parser, "</%20>", "/%20");
        validateParser(parser, "</-._~a-zA-Z0-9:@!$&'()*+,;=>", "/-._~a-zA-Z0-9:@!$&'()*+,;=");
    }

    @Test
    public void parse_invalid_uri_reference() {
        UriReferenceParser parser = new DefaultUriReferenceParser();

        assertFalse(parser.isValid("<file:///etc/hosts>"));
        assertFalse(parser.isValid("</hosts?query>"));
        assertFalse(parser.isValid("</hosts#hash>"));
        assertFalse(parser.isValid("</%>"));
        assertFalse(parser.isValid("</%a>"));
        assertFalse(parser.isValid("</%1g>"));
        assertFalse(parser.isValid("</fóó>"));
        assertFalse(parser.isValid("<>"));
        assertFalse(parser.isValid("</"));
        assertFalse(parser.isValid("<//>"));
        assertFalse(parser.isValid("//>"));
    }

    private void validateParser(UriReferenceParser parser, String content, String expected) throws LinkParseException {
        assertTrue(parser.isValid(content));
        String parsed = parser.parse(content);
        Assert.assertEquals(expected, parsed);
    }

}

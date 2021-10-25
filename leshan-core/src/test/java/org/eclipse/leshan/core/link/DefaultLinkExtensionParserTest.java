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

import java.util.Map;

import org.eclipse.leshan.core.link.linkextension.DefaultLinkExtensionParser;
import org.eclipse.leshan.core.link.linkextension.DefaultLinkExtensionValueParser;
import org.eclipse.leshan.core.link.linkextension.DefaultParmnameParser;
import org.eclipse.leshan.core.link.linkextension.LinkExtensionParser;
import org.eclipse.leshan.core.link.linkextension.LinkExtensionValueParser;
import org.eclipse.leshan.core.link.linkextension.ParmnameParser;
import org.junit.Test;

public class DefaultLinkExtensionParserTest {

    @Test
    public void parse_extension_link_value() throws LinkParseException {
        LinkExtensionValueParser parser = new DefaultLinkExtensionValueParser();

        validateParser(parser, "foo");
        validateParser(parser, "/!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9");
        validateParser(parser, "\"\"", "\"\"");
        validateParser(parser, "\"foo\"", "\"foo\"");
        validateParser(parser, "\"foo,bar\"", "\"foo,bar\"");
        validateParser(parser, "\"foo;bar\"", "\"foo;bar\"");
        validateParser(parser, "\"\\x\"", "\"x\"");
        validateParser(parser, "\"\\ą\"", "\"\\ą\"");
        validateParser(parser, "\"\\\"\"", "\"\"\"");
        validateParser(parser, "\"\\\\\"", "\"\\\"");

        assertFalse(parser.isValid("\""));
        assertFalse(parser.isValid("\"foo"));
        assertFalse(parser.isValid("foo\""));
        assertFalse(parser.isValid("foo,bar"));
        assertFalse(parser.isValid("foo;bar"));
        assertFalse(parser.isValid("ą"));
        assertFalse(parser.isValid(""));
        assertFalse(parser.isValid(null));
        assertFalse(parser.isValid("\"\\\""));
    }

    private void validateParser(LinkExtensionValueParser parser, String content, String expected)
            throws LinkParseException {
        assertTrue(parser.isValid(content));
        assertEquals(new LinkParamValue(expected), parser.parse(content));
    }

    private void validateParser(LinkExtensionValueParser parser, String content) throws LinkParseException {
        validateParser(parser, content, content);
    }

    @Test
    public void parse_extension_link_pname() throws LinkParseException {
        ParmnameParser pnameParser = new DefaultParmnameParser();

        validateParser(pnameParser, "param");
        validateParser(pnameParser, "!#$&+\\-.^_`|~azAZ09");

        assertFalse(pnameParser.isValid(""));
        assertFalse(pnameParser.isValid(null));
        assertFalse(pnameParser.isValid("ą"));
        assertFalse(pnameParser.isValid(";"));
        assertFalse(pnameParser.isValid(","));
        assertFalse(pnameParser.isValid("="));
        assertFalse(pnameParser.isValid("\""));
    }

    private void validateParser(ParmnameParser parser, String content) throws LinkParseException {
        assertTrue(parser.isValid(content));
        assertEquals(content, parser.parse(content));
    }

    @Test
    public void parse_extension_link() throws LinkParseException {
        LinkExtensionParser linkExtensionParser = new DefaultLinkExtensionParser();

        validateParser(linkExtensionParser, "param", "param", null);
        validateParser(linkExtensionParser, "param=value", "param", "value");
        validateParser(linkExtensionParser, "!#$&+\\-.^_`|~azAZ09=value", "!#$&+\\-.^_`|~azAZ09", "value");
        validateParser(linkExtensionParser, "param=\"value\"", "param", "\"value\"");
        validateParser(linkExtensionParser, "param=\"=\"", "param", "\"=\"");
        validateParser(linkExtensionParser, "!#$&+\\-.^_`|~azAZ09=/!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9",
                "!#$&+\\-.^_`|~azAZ09", "/!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9");
        validateParser(linkExtensionParser, "param=\"\\\\\"", "param", "\"\\\"");

        assertFalse(linkExtensionParser.isValid(null));
        assertFalse(linkExtensionParser.isValid("param="));
        assertFalse(linkExtensionParser.isValid("=value"));
        assertFalse(linkExtensionParser.isValid("param=\"foo\\\""));
    }

    private void validateParser(LinkExtensionParser linkExtensionParser, String content, String param, String value)
            throws LinkParseException {
        assertTrue(linkExtensionParser.isValid(content));
        Map<String, LinkParamValue> parsed = linkExtensionParser.parse(content);
        assertEquals(1, parsed.size());
        assertTrue(parsed.containsKey(param));
        assertEquals(value, parsed.get(param) != null ? parsed.get(param).toString() : null);
    }

}
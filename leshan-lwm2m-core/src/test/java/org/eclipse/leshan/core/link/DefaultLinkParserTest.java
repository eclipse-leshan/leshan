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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.eclipse.leshan.core.link.attributes.ContentFormatAttribute;
import org.eclipse.leshan.core.link.attributes.QuotedStringAttribute;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.attributes.UnquotedStringAttribute;
import org.eclipse.leshan.core.link.attributes.ValuelessAttribute;
import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.jupiter.api.Test;

public class DefaultLinkParserTest {

    private final LinkParser parser = new DefaultLinkParser();

    @Test
    public void parse_example_uri_references() throws LinkParseException {
        Link[] parsed;

        parsed = parser.parseCoreLinkFormat("</uri>".getBytes());
        assertEquals("/uri", parsed[0].getUriReference());

        parsed = parser.parseCoreLinkFormat("</uri/>".getBytes());
        assertEquals("/uri/", parsed[0].getUriReference());

        parsed = parser.parseCoreLinkFormat("</uri//>".getBytes());
        assertEquals("/uri//", parsed[0].getUriReference());

        parsed = parser.parseCoreLinkFormat("</%20>".getBytes());
        assertEquals("/%20", parsed[0].getUriReference());

        parsed = parser.parseCoreLinkFormat("</-._~a-zA-Z0-9:@!$&'()*+,;=>".getBytes());
        assertEquals("/-._~a-zA-Z0-9:@!$&'()*+,;=", parsed[0].getUriReference());
    }

    @Test
    public void allow_less_and_greater_sign_as_attributes() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=<,</bar>;param2=>".getBytes());
        assertEquals("/foo", parsed[0].getUriReference());
        assertTrue(parsed[0].getAttributes().contains("param"));
        assertEquals("<", parsed[0].getAttributes().get("param").getValue());

        assertEquals("/bar", parsed[1].getUriReference());
        assertTrue(parsed[1].getAttributes().contains("param2"));
        assertEquals(">", parsed[1].getAttributes().get("param2").getValue());
    }

    @Test
    public void allow_slash_as_attributes() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=/".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        assertTrue(parsed[0].getAttributes().contains("param"));
        assertEquals("/", parsed[0].getAttributes().get("param").getValue());
    }

    @Test
    public void allow_escaped_characters() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\",\",</bar>".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", ","));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters2a() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\" \\\\ \",</bar>".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", " \\\\ "));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters2b() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\" \\\" \\\" \",</bar>".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", " \" \" "));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters2c() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\" \\x \",</bar>".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", " \\x "));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void dont_escape_non_ascii_chars() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\" \\ą \",</bar>".getBytes(StandardCharsets.UTF_8));

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", " \\ą "));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters3() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\";\",</bar>".getBytes());
        assertEquals("/foo", parsed[0].getUriReference());

        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", ";"));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters4a() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\"<\",</bar>".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", "<"));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters4b() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\">\",</bar>".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", ">"));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters5() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=\"=\"".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet(new QuotedStringAttribute("param", "="));
        assertEquals(attResult, parsed[0].getAttributes());
    }

    @Test
    public void allow_ptoken() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat("</foo>;param=!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9".getBytes());
        assertEquals("/foo", parsed[0].getUriReference());

        AttributeSet attResult = new AttributeSet(
                new UnquotedStringAttribute("param", "!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9"));
        assertEquals(attResult, parsed[0].getAttributes());
    }

    @Test
    public void allow_mixed_attributes() throws LinkParseException {
        Link[] parsed = parser.parseCoreLinkFormat(
                "</foo>;param=!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9;param2=\"foo\";param3,</bar>".getBytes());

        assertEquals("/foo", parsed[0].getUriReference());
        AttributeSet attResult = new AttributeSet( //
                new UnquotedStringAttribute("param", "!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9"), //
                new QuotedStringAttribute("param2", "foo"), //
                new ValuelessAttribute("param3"));
        assertEquals(attResult, parsed[0].getAttributes());

        assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void parse_with_some_attributes() throws LinkParseException {
        Link[] parse = parser.parseCoreLinkFormat(
                "</>;rt=\"oma.lwm2m\";ct=100;qs=\"quoted_string\";us=unquoted_string,</1/101>,</1/102>,</2/0>,</2/1>;empty"
                        .getBytes());
        assertEquals(5, parse.length);

        assertEquals("/", parse[0].getUriReference());
        AttributeSet attResult = new AttributeSet( //
                new ResourceTypeAttribute("oma.lwm2m"), //
                new ContentFormatAttribute(ContentFormat.fromCode(100)), //
                new QuotedStringAttribute("qs", "quoted_string"), //
                new UnquotedStringAttribute("us", "unquoted_string"));
        assertEquals(attResult, parse[0].getAttributes());

        assertEquals("/1/101", parse[1].getUriReference());
        assertTrue(!parse[1].hasAttribute());

        assertEquals("/1/102", parse[2].getUriReference());
        assertTrue(!parse[2].hasAttribute());

        assertEquals("/2/0", parse[3].getUriReference());
        assertTrue(!parse[3].hasAttribute());

        assertEquals("/2/1", parse[4].getUriReference());
        assertTrue(!parse[4].getAttributes().get("empty").hasValue());
    }

    @Test
    public void parse_quoted_ver_attributes() throws LinkParseException {
        String input = "</1>;ver=\"2.2\"";
        Link[] objs = parser.parseCoreLinkFormat(input.getBytes());
        assertEquals(objs[0].getAttributes().get("ver"), new QuotedStringAttribute("ver", "2.2"));
    }

    @Test
    public void parse_unquoted_ver_attributes() throws LinkParseException {
        String input = "</1>;ver=2.2";
        Link[] objs = parser.parseCoreLinkFormat(input.getBytes());
        assertEquals(objs[0].getAttributes().get("ver"), new UnquotedStringAttribute("ver", "2.2"));
    }
}

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

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class DefaultLinkParserTest {

    private final LinkParser parser = new DefaultLinkParser();

    @Test
    public void parse_example_uri_references() throws LinkParseException {
        Link[] parsed;

        parsed = parser.parse("</uri>".getBytes());
        Assert.assertEquals("/uri", parsed[0].getUriReference());

        parsed = parser.parse("</uri/>".getBytes());
        Assert.assertEquals("/uri/", parsed[0].getUriReference());

        parsed = parser.parse("</uri//>".getBytes());
        Assert.assertEquals("/uri//", parsed[0].getUriReference());

        parsed = parser.parse("</%20>".getBytes());
        Assert.assertEquals("/%20", parsed[0].getUriReference());

        parsed = parser.parse("</-._~a-zA-Z0-9:@!$&'()*+,;=>".getBytes());
        Assert.assertEquals("/-._~a-zA-Z0-9:@!$&'()*+,;=", parsed[0].getUriReference());
    }

    @Test
    public void allow_less_and_greater_sign_as_attributes() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=<,</bar>;param=>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());
        Assert.assertTrue(parsed[0].getLinkParams().containsKey("param"));
        Assert.assertEquals("<", parsed[0].getLinkParams().get("param").toString());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
        Assert.assertTrue(parsed[1].getLinkParams().containsKey("param"));
        Assert.assertEquals(">", parsed[1].getLinkParams().get("param").toString());
    }

    @Test
    public void allow_slash_as_attributes() throws LinkParseException {
        Link[] parsed;

        parsed = parser.parse("</foo>;param=/".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());
        Assert.assertTrue(parsed[0].getLinkParams().containsKey("param"));
        Assert.assertEquals("/", parsed[0].getLinkParams().get("param").toString());
    }

    @Test
    public void allow_escaped_characters() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\",\",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\",\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters2a() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\" \\\\ \",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\" \\ \""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters2b() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\" \\\" \",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\" \" \""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters2c() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\" \\x \",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\" x \""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void dont_escape_non_ascii_chars() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\" \\ą \",</bar>".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\" \\ą \""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters3() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\";\",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\";\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters4a() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\"<\",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\"<\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters4b() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\">\",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\">\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_characters5() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=\"=\"".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\"=\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());
    }

    @Test
    public void allow_ptoken() throws LinkParseException {
        Link[] parsed = parser.parse("</foo>;param=!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9"));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());
    }

    @Test
    public void allow_mixed_attributes() throws LinkParseException {
        Link[] parsed = parser
                .parse("</foo>;param=!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9;param2=\"foo\";param3,</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9"));
        attResult.put("param2", new LinkParamValue("\"foo\""));
        attResult.put("param3", null);
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void parse_with_some_attributes() throws LinkParseException {
        Link[] parse = parser.parse("</>;rt=\"oma.lwm2m\";ct=100,</1/101>,</1/102>,</2/0>,</2/1>;empty".getBytes());
        Assert.assertEquals(5, parse.length);
        Assert.assertEquals("/", parse[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("rt", new LinkParamValue("\"oma.lwm2m\""));
        attResult.put("ct", new LinkParamValue("100"));
        Assert.assertEquals(attResult, parse[0].getLinkParams());

        Assert.assertEquals("/1/101", parse[1].getUriReference());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[1].getLinkParams());

        Assert.assertEquals("/1/102", parse[2].getUriReference());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[2].getLinkParams());

        Assert.assertEquals("/2/0", parse[3].getUriReference());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[3].getLinkParams());
        Assert.assertEquals("/2/1", parse[4].getUriReference());

        attResult = new HashMap<>();
        attResult.put("empty", null);
        Assert.assertEquals(attResult, parse[4].getLinkParams());
    }

    @Test
    public void parse_quoted_ver_attributes() throws LinkParseException {
        String input = "</1>;ver=\"2.2\"";
        Link[] objs = parser.parse(input.getBytes());
        assertEquals(objs[0].getLinkParams().get("ver"), new LinkParamValue("\"2.2\""));
    }

    @Test
    public void parse_unquoted_ver_attributes() throws LinkParseException {
        String input = "</1>;ver=2.2";
        Link[] objs = parser.parse(input.getBytes());
        assertEquals(objs[0].getLinkParams().get("ver"), new LinkParamValue("2.2"));
    }
}

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
package org.eclipse.leshan.core.link;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class DefaultLinkParserTest {

    private final LinkParser parser = new DefaultLinkParser();

    @Test
    public void parse_example_uri_references() {
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
    public void allow_escaped_charractes() {
        Link[] parsed = parser.parse("</foo>;param=\",\",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\",\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_charractes2() {
        Link[] parsed = parser.parse("</foo>;param=\" \\\\ \",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\" \\ \""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_charractes2a() {
        Link[] parsed = parser.parse("</foo>;param=\" \\\" \",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\" \" \""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_charractes2b() {
        Link[] parsed = parser.parse("</foo>;param=\" \\x \",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\" x \""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void dont_escape_non_ascii_chars() {
        Link[] parsed = parser.parse("</foo>;param=\" \\ą \",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\" \\ą \""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_charractes3() {
        Link[] parsed = parser.parse("</foo>;param=\";\",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\";\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_charractes4() {
        Link[] parsed = parser.parse("</foo>;param=\"<\",</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\"<\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void allow_escaped_charractes5() {
        Link[] parsed = parser.parse("</foo>;param=\"=\"".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("\"=\""));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());
    }

    @Test
    public void allow_ptoken() {
        Link[] parsed = parser.parse("</foo>;param=!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9"));
        Assert.assertEquals(attResult, parsed[0].getLinkParams());
    }

    @Test
    public void allow_mixed_attributes() {
        Link[] parsed = parser.parse("</foo>;param=!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9;param2=\"foo\";param3,</bar>".getBytes());
        Assert.assertEquals("/foo", parsed[0].getUriReference());

        Map<String, LinkParamValue> attResult = new HashMap<>();
        attResult.put("param", new LinkParamValue("!#$%&'()*+-.:<=>?@[]^_`{|}~a1z9"));
        attResult.put("param2", new LinkParamValue("\"foo\""));
        attResult.put("param3", null);
        Assert.assertEquals(attResult, parsed[0].getLinkParams());

        Assert.assertEquals("/bar", parsed[1].getUriReference());
    }

    @Test
    public void parse_with_some_attributes() {
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
    public void parse_quoted_ver_attributes() {
        String input = "</1>;ver=\"2.2\"";
        Link[] objs = parser.parse(input.getBytes());
        assertEquals(objs[0].getLinkParams().get("ver"), new LinkParamValue("\"2.2\""));
    }

    @Test
    public void parse_unquoted_ver_attributes() {
        String input = "</1>;ver=2.2";
        Link[] objs = parser.parse(input.getBytes());
        assertEquals(objs[0].getLinkParams().get("ver"), new LinkParamValue("2.2"));
    }
}

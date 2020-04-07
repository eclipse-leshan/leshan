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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.Link;
import org.junit.Assert;
import org.junit.Test;

public class LinkObjectTest {

    @Test
    public void parse_with_some_attributes() {
        Link[] parse = Link.parse("</>;rt=\"oma.lwm2m\";ct=100, </1/101>,</1/102>, </2/0>, </2/1> ;empty".getBytes());
        Assert.assertEquals(5, parse.length);
        Assert.assertEquals("/", parse[0].getUrl());

        Map<String, String> attResult = new HashMap<>();
        attResult.put("rt", "\"oma.lwm2m\"");
        attResult.put("ct", "100");
        Assert.assertEquals(attResult, parse[0].getAttributes());

        Assert.assertEquals("/1/101", parse[1].getUrl());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[1].getAttributes());

        Assert.assertEquals("/1/102", parse[2].getUrl());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[2].getAttributes());

        Assert.assertEquals("/2/0", parse[3].getUrl());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[3].getAttributes());
        Assert.assertEquals("/2/1", parse[4].getUrl());

        attResult = new HashMap<>();
        attResult.put("empty", null);
        Assert.assertEquals(attResult, parse[4].getAttributes());
    }

    @Test
    public void parse_with_quoted_attributes() {
        Link[] parse = Link
                .parse("</>;k1=\"quotes\"inside\";k2=endwithquotes\";k3=noquotes;k4=\"startwithquotes".getBytes());
        Assert.assertEquals(1, parse.length);
        Assert.assertEquals("/", parse[0].getUrl());

        Map<String, String> attResult = new HashMap<>();
        attResult.put("k1", "\"quotes\"inside\"");
        attResult.put("k2", "endwithquotes\"");
        attResult.put("k3", "noquotes");
        attResult.put("k4", "\"startwithquotes");
        Assert.assertEquals(attResult, parse[0].getAttributes());
    }

    @Test
    public void serialyse_without_attribute() {
        Link obj1 = new Link("/1/0/1");
        Link obj2 = new Link("/2/1");
        Link obj3 = new Link("/3");

        String res = Link.serialize(obj1, obj2, obj3);

        Assert.assertEquals("</1/0/1>,</2/1>,</3>", res);

    }

    @Test
    public void serialyse_with_attributes() {
        Link obj1 = new Link("/1/0/1", "number", "12");
        Link obj2 = new Link("/2/1", "string", "\"stringval\"");
        Link obj3 = new Link("/3", "empty", null);

        String res = Link.serialize(obj1, obj2, obj3);

        Assert.assertEquals("</1/0/1>;number=12,</2/1>;string=\"stringval\",</3>;empty", res);

    }

    @Test
    public void serialyse_with_root_url() {
        HashMap<String, Integer> attributesObj1 = new HashMap<>();
        attributesObj1.put("number", 12);
        Link obj1 = new Link("/", attributesObj1, Integer.class);

        String res = Link.serialize(obj1);

        Assert.assertEquals("</>;number=12", res);

    }

    @Test
    public void serialyse_then_parse_with_severals_attributes() {
        HashMap<String, Object> attributesObj1 = new HashMap<>();
        attributesObj1.put("number1", 1);
        attributesObj1.put("number2", 1);
        attributesObj1.put("string1", "stringval1");
        Link obj1 = new Link("/1/0", attributesObj1, Object.class);

        HashMap<String, Integer> attributesObj2 = new HashMap<>();
        attributesObj2.put("number3", 3);
        Link obj2 = new Link("/2", attributesObj2, Integer.class);

        Link[] input = new Link[] { obj1, obj2 };
        String strObjs = Link.serialize(input);
        Link[] output = Link.parse(strObjs.getBytes());

        Assert.assertArrayEquals(input, output);

    }

    @Test
    public void parse_then_serialyse_with_rt_attribute() {
        String input = "</lwm2m>;rt=\"oma.lwm2m\",</lwm2m/1/101>,</lwm2m/1/102>,</lwm2m/2/0>";
        Link[] objs = Link.parse(input.getBytes());
        String output = Link.serialize(objs);
        Assert.assertEquals(input, output);

    }

    @Test
    public void parse_quoted_ver_attributes() {
        String input = "</1>;ver=\"2.2\"";
        Link[] objs = Link.parse(input.getBytes());
        assertEquals(objs[0].getAttributes().get("ver"), "\"2.2\"");
    }

    @Test
    public void parse_unquoted_ver_attributes() {
        String input = "</1>;ver=2.2";
        Link[] objs = Link.parse(input.getBytes());
        assertEquals(objs[0].getAttributes().get("ver"), "2.2");
    }

    @Test
    public void serialyse_ver_attributes_without_quote() {
        Map<String, String> att = new HashMap<>();
        att.put("ver", "2.2");
        Link link = new Link("/1", att);
        assertEquals("</1>;ver=2.2", Link.serialize(link));
    }
}

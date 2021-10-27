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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class DefaultLinkSerializerTest {

    private final LinkParser parser = new DefaultLinkParser();
    private final LinkSerializer serializer = new DefaultLinkSerializer();

    @Test
    public void serialise_without_attribute() {
        Link obj1 = new Link("/1/0/1");
        Link obj2 = new Link("/2/1");
        Link obj3 = new Link("/3");

        String res = serializer.serialize(obj1, obj2, obj3);

        Assert.assertEquals("</1/0/1>,</2/1>,</3>", res);

    }

    @Test
    public void serialise_with_attributes() {
        Link obj1 = new Link("/1/0/1", "number", "12");
        Link obj2 = new Link("/2/1", "string", "\"stringval\"");
        Link obj3 = new Link("/3", "empty", null);

        String res = serializer.serialize(obj1, obj2, obj3);

        Assert.assertEquals("</1/0/1>;number=12,</2/1>;string=\"stringval\",</3>;empty", res);
    }

    @Test
    public void serialise_with_root_url() {
        HashMap<String, Integer> attributesObj1 = new HashMap<>();
        attributesObj1.put("number", 12);
        Link obj1 = new Link("/", attributesObj1, Integer.class);

        String res = serializer.serialize(obj1);

        Assert.assertEquals("</>;number=12", res);
    }

    @Test
    public void serialise_then_parse_with_severals_attributes() throws LinkParseException {
        HashMap<String, Object> attributesObj1 = new HashMap<>();
        attributesObj1.put("number1", 1);
        attributesObj1.put("number2", 1);
        attributesObj1.put("string1", "stringval1");
        Link obj1 = new Link("/1/0", attributesObj1, Object.class);

        HashMap<String, Integer> attributesObj2 = new HashMap<>();
        attributesObj2.put("number3", 3);
        Link obj2 = new Link("/2", attributesObj2, Integer.class);

        Link[] input = new Link[] { obj1, obj2 };
        String strObjs = serializer.serialize(input);
        Link[] output = parser.parse(strObjs.getBytes());

        Assert.assertArrayEquals(input, output);
    }

    @Test
    public void parse_then_serialise_with_rt_attribute() throws LinkParseException {
        String input = "</lwm2m>;rt=\"oma.lwm2m\",</lwm2m/1/101>,</lwm2m/1/102>,</lwm2m/2/0>";
        Link[] objs = parser.parse(input.getBytes());
        String output = serializer.serialize(objs);
        Assert.assertEquals(input, output);

    }

    @Test
    public void serialise_ver_attributes_without_quote() {
        Map<String, String> att = new HashMap<>();
        att.put("ver", "2.2");
        Link link = new Link("/1", att, String.class);
        assertEquals("</1>;ver=2.2", serializer.serialize(link));
    }
}

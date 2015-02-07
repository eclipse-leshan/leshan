/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.LinkObject;
import org.junit.Assert;
import org.junit.Test;

public class LinkObjectTest {

    @Test
    public void parse_with_some_attributes() {
        LinkObject[] parse = LinkObject.parse("</>;rt=\"oma.lwm2m\";ct=100, </1/101>,</1/102>, </2/0>, </2/1> ;empty"
                .getBytes());
        Assert.assertEquals(5, parse.length);
        Assert.assertEquals("/", parse[0].getUrl());

        Map<String, Object> attResult = new HashMap<>();
        attResult.put("rt", "oma.lwm2m");
        attResult.put("ct", 100);
        Assert.assertEquals(attResult, parse[0].getAttributes());

        Assert.assertEquals("/1/101", parse[1].getUrl());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[1].getAttributes());
        Assert.assertEquals(Integer.valueOf(1), parse[1].getObjectId());
        Assert.assertEquals(Integer.valueOf(101), parse[1].getObjectInstanceId());
        Assert.assertNull(parse[1].getResourceId());

        Assert.assertEquals("/1/102", parse[2].getUrl());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[2].getAttributes());
        Assert.assertEquals(Integer.valueOf(1), parse[2].getObjectId());
        Assert.assertEquals(Integer.valueOf(102), parse[2].getObjectInstanceId());
        Assert.assertNull(parse[2].getResourceId());

        Assert.assertEquals("/2/0", parse[3].getUrl());
        Assert.assertEquals(Collections.EMPTY_MAP, parse[3].getAttributes());
        Assert.assertEquals("/2/1", parse[4].getUrl());
        Assert.assertEquals(Integer.valueOf(2), parse[4].getObjectId());
        Assert.assertEquals(Integer.valueOf(1), parse[4].getObjectInstanceId());
        Assert.assertNull(parse[4].getResourceId());

        attResult = new HashMap<>();
        attResult.put("empty", null);
        Assert.assertEquals(attResult, parse[4].getAttributes());
    }

    @Test
    public void parse_with_quoted_attributes() {
        LinkObject[] parse = LinkObject
                .parse("</>;k1=\"quotes\"inside\";k2=endwithquotes\";k3=noquotes;k4=\"startwithquotes".getBytes());
        Assert.assertEquals(1, parse.length);
        Assert.assertEquals("/", parse[0].getUrl());

        Map<String, String> attResult = new HashMap<>();
        attResult.put("k1", "quotes\"inside");
        attResult.put("k2", "endwithquotes\"");
        attResult.put("k3", "noquotes");
        attResult.put("k4", "\"startwithquotes");
        Assert.assertEquals(attResult, parse[0].getAttributes());
        Assert.assertNull(parse[0].getObjectId());
        Assert.assertNull(parse[0].getObjectInstanceId());
        Assert.assertNull(parse[0].getResourceId());

    }
}

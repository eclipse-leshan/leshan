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
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.util.Charsets;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link LwM2mNodeEncoder}
 */
public class LwM2mNodeEncoderTest {

    private static LwM2mModel model;

    @BeforeClass
    public static void loadModel() {
        model = new LwM2mModel(ObjectLoader.loadDefault());
    }

    @Test
    public void text_encode_single_resource() {

        byte[] encoded = LwM2mNodeEncoder.encode(LwM2mSingleResource.newFloatResource(15, 56.4D), ContentFormat.TEXT,
                new LwM2mPath("/323/0/15"), model);

        Assert.assertEquals("56.4", new String(encoded, Charsets.UTF_8));
    }

    @Test
    public void text_encode_date_as_long() {

        byte[] encoded = LwM2mNodeEncoder.encode(
                LwM2mSingleResource.newStringResource(13, "2010-01-01T12:00:00+01:00"), ContentFormat.TEXT,
                new LwM2mPath("/3/0/13"), model);

        Assert.assertEquals("1262343600", new String(encoded, Charsets.UTF_8));
    }

    @Test
    public void text_encode_date_as_iso_string() {

        byte[] encoded = LwM2mNodeEncoder.encode(LwM2mSingleResource.newIntegerResource(13, 1367491215000L),
                ContentFormat.TEXT, new LwM2mPath("/3/0/13"), model);

        Assert.assertEquals("1367491215", new String(encoded, Charsets.UTF_8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void text_encode_multiple_instances() {
        Map<Integer, Long> values = new HashMap<>();
        values.put(0, 1L);
        values.put(1, 5L);
        LwM2mNodeEncoder.encode(LwM2mMultipleResource.newIntegerResource(6, values), ContentFormat.TEXT, new LwM2mPath(
                "/3/0/6"), model);
    }

    @Test
    public void tlv_encode_device_object_instance() {

        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(LwM2mSingleResource.newStringResource(0, "Open Mobile Alliance"));
        resources.add(LwM2mSingleResource.newStringResource(1, "Lightweight M2M Client"));
        resources.add(LwM2mSingleResource.newStringResource(2, "345000123"));
        resources.add(LwM2mSingleResource.newStringResource(3, "1.0"));

        Map<Integer, Long> values = new HashMap<>();
        values.put(0, 1L);
        values.put(1, 5L);
        resources.add(LwM2mMultipleResource.newIntegerResource(6, values));

        values = new HashMap<>();
        values.put(0, 3800L);
        values.put(1, 5000L);
        resources.add(LwM2mMultipleResource.newIntegerResource(7, values));

        values = new HashMap<>();
        values.put(0, 125L);
        values.put(1, 900L);
        resources.add(LwM2mMultipleResource.newIntegerResource(8, values));

        resources.add(LwM2mSingleResource.newIntegerResource(9, 100));
        resources.add(LwM2mSingleResource.newIntegerResource(10, 15));
        resources.add(LwM2mSingleResource.newIntegerResource(11, 0));
        resources.add(LwM2mSingleResource.newDateResource(13, new Date(1367491215000L)));
        resources.add(LwM2mSingleResource.newStringResource(14, "+02:00"));
        resources.add(LwM2mSingleResource.newStringResource(15, "U"));

        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, resources);

        byte[] encoded = LwM2mNodeEncoder.encode(oInstance, ContentFormat.TLV, new LwM2mPath("/3/0"), model);

        // tlvs content the resources array
        byte[] expected = new byte[] { -56, 0, 20, 79, 112, 101, 110, 32, 77, 111, 98, 105, 108, 101, 32, 65, 108, 108,
                                105, 97, 110, 99, 101, -56, 1, 22, 76, 105, 103, 104, 116, 119, 101, 105, 103, 104,
                                116, 32, 77, 50, 77, 32, 67, 108, 105, 101, 110, 116, -56, 2, 9, 51, 52, 53, 48, 48,
                                48, 49, 50, 51, -61, 3, 49, 46, 48, -122, 6, 65, 0, 1, 65, 1, 5, -120, 7, 8, 66, 0, 14,
                                -40, 66, 1, 19, -120, -121, 8, 65, 0, 125, 66, 1, 3, -124, -63, 9, 100, -63, 10, 15,
                                -63, 11, 0, -60, 13, 81, -126, 66, -113, -58, 14, 43, 48, 50, 58, 48, 48, -63, 15, 85 };

        Assert.assertArrayEquals(expected, encoded);
    }

    @Test
    public void tlv_encode_device_object() {

        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(LwM2mSingleResource.newStringResource(0, "Open Mobile Alliance"));
        resources.add(LwM2mSingleResource.newStringResource(1, "Lightweight M2M Client"));
        resources.add(LwM2mSingleResource.newStringResource(2, "345000123"));
        resources.add(LwM2mSingleResource.newStringResource(3, "1.0"));

        Map<Integer, Long> values = new HashMap<>();
        values.put(0, 1L);
        values.put(1, 5L);
        resources.add(LwM2mMultipleResource.newIntegerResource(6, values));

        values = new HashMap<>();
        values.put(0, 3800L);
        values.put(1, 5000L);
        resources.add(LwM2mMultipleResource.newIntegerResource(7, values));

        values = new HashMap<>();
        values.put(0, 125L);
        values.put(1, 900L);
        resources.add(LwM2mMultipleResource.newIntegerResource(8, values));

        resources.add(LwM2mSingleResource.newIntegerResource(9, 100));
        resources.add(LwM2mSingleResource.newIntegerResource(10, 15));
        resources.add(LwM2mSingleResource.newIntegerResource(11, 0));
        resources.add(LwM2mSingleResource.newDateResource(13, new Date(1367491215000L)));
        resources.add(LwM2mSingleResource.newStringResource(14, "+02:00"));
        resources.add(LwM2mSingleResource.newStringResource(15, "U"));

        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, resources);
        LwM2mObject object = new LwM2mObject(3, oInstance);

        byte[] encoded = LwM2mNodeEncoder.encode(object, ContentFormat.TLV, new LwM2mPath("/3"), model);

        // tlvs content the resources array
        byte[] expected = new byte[] { -56, 0, 20, 79, 112, 101, 110, 32, 77, 111, 98, 105, 108, 101, 32, 65, 108, 108,
                                105, 97, 110, 99, 101, -56, 1, 22, 76, 105, 103, 104, 116, 119, 101, 105, 103, 104,
                                116, 32, 77, 50, 77, 32, 67, 108, 105, 101, 110, 116, -56, 2, 9, 51, 52, 53, 48, 48,
                                48, 49, 50, 51, -61, 3, 49, 46, 48, -122, 6, 65, 0, 1, 65, 1, 5, -120, 7, 8, 66, 0, 14,
                                -40, 66, 1, 19, -120, -121, 8, 65, 0, 125, 66, 1, 3, -124, -63, 9, 100, -63, 10, 15,
                                -63, 11, 0, -60, 13, 81, -126, 66, -113, -58, 14, 43, 48, 50, 58, 48, 48, -63, 15, 85 };

        Assert.assertArrayEquals(expected, encoded);
    }

    @Test
    public void json_encode_device_object_instance() {

        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(LwM2mSingleResource.newStringResource(0, "Open Mobile Alliance"));
        resources.add(LwM2mSingleResource.newStringResource(1, "Lightweight M2M Client"));
        resources.add(LwM2mSingleResource.newStringResource(2, "345000123"));
        resources.add(LwM2mSingleResource.newStringResource(3, "1.0"));

        Map<Integer, Long> values = new HashMap<>();
        values.put(0, 1L);
        values.put(1, 5L);
        resources.add(LwM2mMultipleResource.newIntegerResource(6, values));

        values = new HashMap<>();
        values.put(0, 3800L);
        values.put(1, 5000L);
        resources.add(LwM2mMultipleResource.newIntegerResource(7, values));

        values = new HashMap<>();
        values.put(0, 125L);
        values.put(1, 900L);
        resources.add(LwM2mMultipleResource.newIntegerResource(8, values));

        resources.add(LwM2mSingleResource.newIntegerResource(9, 100));
        resources.add(LwM2mSingleResource.newIntegerResource(10, 15));
        resources.add(LwM2mSingleResource.newIntegerResource(11, 0));
        resources.add(LwM2mSingleResource.newDateResource(13, new Date(1367491215000L)));
        resources.add(LwM2mSingleResource.newStringResource(14, "+02:00"));
        resources.add(LwM2mSingleResource.newStringResource(15, "U"));

        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, resources);

        byte[] encoded = LwM2mNodeEncoder.encode(oInstance, ContentFormat.JSON, new LwM2mPath("/3/0"), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"e\":[");
        b.append("{\"n\":\"0\",\"sv\":\"Open Mobile Alliance\"},");
        b.append("{\"n\":\"1\",\"sv\":\"Lightweight M2M Client\"},");
        b.append("{\"n\":\"2\",\"sv\":\"345000123\"},");
        b.append("{\"n\":\"3\",\"sv\":\"1.0\"},");
        b.append("{\"n\":\"6/0\",\"v\":1},");
        b.append("{\"n\":\"6/1\",\"v\":5},");
        b.append("{\"n\":\"7/0\",\"v\":3800},");
        b.append("{\"n\":\"7/1\",\"v\":5000},");
        b.append("{\"n\":\"8/0\",\"v\":125},");
        b.append("{\"n\":\"8/1\",\"v\":900},");
        b.append("{\"n\":\"9\",\"v\":100},");
        b.append("{\"n\":\"10\",\"v\":15},");
        b.append("{\"n\":\"11\",\"v\":0},");
        b.append("{\"n\":\"13\",\"v\":1367491215},");
        b.append("{\"n\":\"14\",\"sv\":\"+02:00\"},");
        b.append("{\"n\":\"15\",\"sv\":\"U\"}]}");

        String expected = b.toString();
        Assert.assertEquals(expected, new String(encoded));
    }
}

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
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
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
        Map<Integer, ObjectModel> models = new HashMap<>();
        for (ObjectModel model : ObjectLoader.loadDefault()) {
            models.put(model.id, model);
        }
        model = new LwM2mModel(models);
    }

    @Test
    public void text_encode_single_resource() {

        byte[] encoded = LwM2mNodeEncoder.encode(new LwM2mResource(15, Value.newDoubleValue(56.4D)),
                ContentFormat.TEXT, new LwM2mPath("/323/0/15"), model);

        Assert.assertEquals("56.4", new String(encoded, Charsets.UTF_8));
    }

    @Test
    public void text_encode_date_as_long() {

        byte[] encoded = LwM2mNodeEncoder.encode(
                new LwM2mResource(13, Value.newStringValue("2010-01-01T12:00:00+01:00")), ContentFormat.TEXT,
                new LwM2mPath("/3/0/13"), model);

        Assert.assertEquals("1262343600", new String(encoded, Charsets.UTF_8));
    }

    @Test
    public void text_encode_date_as_iso_string() {

        byte[] encoded = LwM2mNodeEncoder.encode(new LwM2mResource(13, Value.newLongValue(1367491215000L)),
                ContentFormat.TEXT, new LwM2mPath("/3/0/13"), model);

        Assert.assertEquals("1367491215", new String(encoded, Charsets.UTF_8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void text_encode_multiple_instances() {
        LwM2mNodeEncoder.encode(
                new LwM2mResource(6, new Value[] { Value.newIntegerValue(1), Value.newIntegerValue(5) }),
                ContentFormat.TEXT, new LwM2mPath("/3/0/6"), model);
    }

    @Test
    public void tlv_encode_device_object_instance() {

        Collection<LwM2mResource> resources = new ArrayList<>();

        resources.add(new LwM2mResource(0, Value.newStringValue("Open Mobile Alliance")));
        resources.add(new LwM2mResource(1, Value.newStringValue("Lightweight M2M Client")));
        resources.add(new LwM2mResource(2, Value.newStringValue("345000123")));
        resources.add(new LwM2mResource(3, Value.newStringValue("1.0")));

        resources.add(new LwM2mResource(6, new Value[] { Value.newIntegerValue(1), Value.newIntegerValue(5) }));
        resources.add(new LwM2mResource(7, new Value[] { Value.newIntegerValue(3800), Value.newIntegerValue(5000) }));
        resources.add(new LwM2mResource(8, new Value[] { Value.newIntegerValue(125), Value.newIntegerValue(900) }));
        resources.add(new LwM2mResource(9, Value.newIntegerValue(100)));
        resources.add(new LwM2mResource(10, Value.newIntegerValue(15)));
        resources.add(new LwM2mResource(11, Value.newIntegerValue(0)));
        resources.add(new LwM2mResource(13, Value.newDateValue(new Date(1367491215000L))));
        resources.add(new LwM2mResource(14, Value.newStringValue("+02:00")));
        resources.add(new LwM2mResource(15, Value.newStringValue("U")));

        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, resources.toArray(new LwM2mResource[0]));

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

        resources.add(new LwM2mResource(0, Value.newStringValue("Open Mobile Alliance")));
        resources.add(new LwM2mResource(1, Value.newStringValue("Lightweight M2M Client")));
        resources.add(new LwM2mResource(2, Value.newStringValue("345000123")));
        resources.add(new LwM2mResource(3, Value.newStringValue("1.0")));

        resources.add(new LwM2mResource(6, new Value[] { Value.newIntegerValue(1), Value.newIntegerValue(5) }));
        resources.add(new LwM2mResource(7, new Value[] { Value.newIntegerValue(3800), Value.newIntegerValue(5000) }));
        resources.add(new LwM2mResource(8, new Value[] { Value.newIntegerValue(125), Value.newIntegerValue(900) }));
        resources.add(new LwM2mResource(9, Value.newIntegerValue(100)));
        resources.add(new LwM2mResource(10, Value.newIntegerValue(15)));
        resources.add(new LwM2mResource(11, Value.newIntegerValue(0)));
        resources.add(new LwM2mResource(13, Value.newDateValue(new Date(1367491215000L))));
        resources.add(new LwM2mResource(14, Value.newStringValue("+02:00")));
        resources.add(new LwM2mResource(15, Value.newStringValue("U")));

        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, resources.toArray(new LwM2mResource[0]));
        LwM2mObject object = new LwM2mObject(3, new LwM2mObjectInstance[] { oInstance });

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

        resources.add(new LwM2mResource(0, Value.newStringValue("Open Mobile Alliance")));
        resources.add(new LwM2mResource(1, Value.newStringValue("Lightweight M2M Client")));
        resources.add(new LwM2mResource(2, Value.newStringValue("345000123")));
        resources.add(new LwM2mResource(3, Value.newStringValue("1.0")));

        resources.add(new LwM2mResource(6, new Value[] { Value.newIntegerValue(1), Value.newIntegerValue(5) }));
        resources.add(new LwM2mResource(7, new Value[] { Value.newIntegerValue(3800), Value.newIntegerValue(5000) }));
        resources.add(new LwM2mResource(8, new Value[] { Value.newIntegerValue(125), Value.newIntegerValue(900) }));
        resources.add(new LwM2mResource(9, Value.newIntegerValue(100)));
        resources.add(new LwM2mResource(10, Value.newIntegerValue(15)));
        resources.add(new LwM2mResource(11, new Value[] { Value.newIntegerValue(0) }));
        resources.add(new LwM2mResource(13, Value.newDateValue(new Date(1367491215000L))));
        resources.add(new LwM2mResource(14, Value.newStringValue("+02:00")));
        resources.add(new LwM2mResource(15, Value.newStringValue("U")));

        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, resources.toArray(new LwM2mResource[0]));

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
        b.append("{\"n\":\"11/0\",\"v\":0},");
        b.append("{\"n\":\"13\",\"v\":1367491215},");
        b.append("{\"n\":\"14\",\"sv\":\"+02:00\"},");
        b.append("{\"n\":\"15\",\"sv\":\"U\"}]}");

        String expected = b.toString();
        Assert.assertEquals(expected, new String(encoded));
    }
}

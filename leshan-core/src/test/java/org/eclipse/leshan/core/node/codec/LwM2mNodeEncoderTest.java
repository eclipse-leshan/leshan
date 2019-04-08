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
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link LwM2mNodeEncoder}
 */
public class LwM2mNodeEncoderTest {

    private static LwM2mModel model;
    private static LwM2mNodeEncoder encoder;

    @BeforeClass
    public static void loadModel() {
        model = new StaticModel(ObjectLoader.loadDefault());
        encoder = new DefaultLwM2mNodeEncoder();
    }

    @Test
    public void text_encode_single_resource_float() {

        byte[] encoded = encoder.encode(LwM2mSingleResource.newFloatResource(15, 56.4D), ContentFormat.TEXT,
                new LwM2mPath("/323/0/15"), model);

        Assert.assertEquals("56.4", new String(encoded, StandardCharsets.UTF_8));
    }

    @Test
    public void text_encode_single_resource_date() {

        byte[] encoded = encoder.encode(LwM2mSingleResource.newDateResource(13, new Date(1367491215000L)),
                ContentFormat.TEXT, new LwM2mPath("/3/0/13"), model);

        Assert.assertEquals("1367491215", new String(encoded, StandardCharsets.UTF_8));
    }

    @Test(expected = CodecException.class)
    public void text_encode_multiple_instances() {
        Map<Integer, Long> values = new HashMap<>();
        values.put(0, 1L);
        values.put(1, 5L);
        encoder.encode(LwM2mMultipleResource.newIntegerResource(6, values), ContentFormat.TEXT, new LwM2mPath("/3/0/6"),
                model);
    }

    @Test
    public void text_encode_opaque_as_base64_string() {
        byte[] opaqueValue = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5 };
        byte[] encoded = encoder.encode(LwM2mSingleResource.newBinaryResource(0, opaqueValue), ContentFormat.TEXT,
                new LwM2mPath("/5/0/0"), model);

        Assert.assertEquals("AQIDBAU=", new String(encoded, StandardCharsets.UTF_8));
    }

    // tlv content for instance 0 of device object (encoded as an array of resource TLVs)
    // Example from LWM2M spec §4.3.1
    private final static byte[] ENCODED_DEVICE_WITHOUT_INSTANCE = Hex.decodeHex(
            "C800144F70656E204D6F62696C6520416C6C69616E6365C801164c69676874776569676874204d324d20436c69656e74C80209333435303030313233C303312E30860641000141010588070842000ED842011388870841007D42010384C10964C10A0F830B410000C40D5182428FC60E2B30323A3030C11055"
                    .toCharArray());

    // tlv content for instance 0 of device object (encoded as an array of only 1 Object instance TLV)
    // Example from LWM2M spec §4.3.2 A)
    private final static byte[] ENCODED_DEVICE_WITH_INSTANCE = Hex.decodeHex(
            "080079C800144F70656E204D6F62696C6520416C6C69616E6365C801164C69676874776569676874204D324D20436C69656E74C80209333435303030313233C303312E30860641000141010588070842000ED842011388870841007D42010384C10964C10A0F830B410000C40D5182428FC60E2B30323A3030C11055"
                    .toCharArray());

    private Collection<LwM2mResource> getDeviceResources() {
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

        values = new HashMap<>();
        values.put(0, 0L);
        resources.add(LwM2mMultipleResource.newIntegerResource(11, values));

        resources.add(LwM2mSingleResource.newDateResource(13, new Date(1367491215000L)));
        resources.add(LwM2mSingleResource.newStringResource(14, "+02:00"));
        resources.add(LwM2mSingleResource.newStringResource(16, "U"));

        return resources;
    }

    @Test
    public void tlv_encode_device_object_instance_as_resources_array() {
        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, getDeviceResources());
        byte[] encoded = encoder.encode(oInstance, ContentFormat.TLV, new LwM2mPath("/3/0"), model);

        Assert.assertArrayEquals(ENCODED_DEVICE_WITHOUT_INSTANCE, encoded);
    }

    @Test
    public void tlv_encode_device_object_instance_as_resources_array__undefined_instance_id() {
        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(getDeviceResources());
        byte[] encoded = encoder.encode(oInstance, ContentFormat.TLV, new LwM2mPath("/3"), model);

        Assert.assertArrayEquals(ENCODED_DEVICE_WITHOUT_INSTANCE, encoded);
    }

    @Test
    public void tlv_encode_device_object_instance_as_instance() {
        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, getDeviceResources());
        byte[] encoded = encoder.encode(oInstance, ContentFormat.TLV, new LwM2mPath("/3"), model);

        Assert.assertArrayEquals(ENCODED_DEVICE_WITH_INSTANCE, encoded);
    }

    @Test
    public void tlv_encode_device_object() {

        LwM2mObject object = new LwM2mObject(3, new LwM2mObjectInstance(0, getDeviceResources()));
        byte[] encoded = encoder.encode(object, ContentFormat.TLV, new LwM2mPath("/3"), model);

        // encoded as an array of resource TLVs
        Assert.assertArrayEquals(ENCODED_DEVICE_WITH_INSTANCE, encoded);
    }

    @Test
    public void json_encode_device_object_instance() {

        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, getDeviceResources());
        byte[] encoded = encoder.encode(oInstance, ContentFormat.JSON, new LwM2mPath("/3/0"), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/3/0/\",\"e\":[");
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
        b.append("{\"n\":\"13\",\"v\":1.367491215E9},");
        b.append("{\"n\":\"14\",\"sv\":\"+02:00\"},");
        b.append("{\"n\":\"16\",\"sv\":\"U\"}]}");

        String expected = b.toString();
        Assert.assertEquals(expected, new String(encoded));
    }

    @Test
    public void json_encode_timestamped_resources() throws CodecException {
        List<TimestampedLwM2mNode> data = new ArrayList<>();
        data.add(new TimestampedLwM2mNode(500L, LwM2mSingleResource.newFloatResource(1, 22.9)));
        data.add(new TimestampedLwM2mNode(510L, LwM2mSingleResource.newFloatResource(1, 22.4)));
        data.add(new TimestampedLwM2mNode(520L, LwM2mSingleResource.newFloatResource(1, 24.1)));

        byte[] encoded = encoder.encodeTimestampedData(data, ContentFormat.JSON, new LwM2mPath(1024, 0, 1), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/0/1\",\"e\":[");
        b.append("{\"v\":22.9,\"t\":500},");
        b.append("{\"v\":22.4,\"t\":510},");
        b.append("{\"v\":24.1,\"t\":520}]}");

        String expected = b.toString();
        Assert.assertEquals(expected, new String(encoded));
    }

    @Test
    public void json_encode_timestamped_instances() throws CodecException {
        List<TimestampedLwM2mNode> data = new ArrayList<>();

        LwM2mObjectInstance instanceAt110 = new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 22.9));
        LwM2mObjectInstance instanceAt120 = new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 22.4),
                LwM2mSingleResource.newStringResource(0, "a string"));
        LwM2mObjectInstance instanceAt130 = new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 24.1));

        data.add(new TimestampedLwM2mNode(110L, instanceAt110));
        data.add(new TimestampedLwM2mNode(120L, instanceAt120));
        data.add(new TimestampedLwM2mNode(130L, instanceAt130));

        byte[] encoded = encoder.encodeTimestampedData(data, ContentFormat.JSON, new LwM2mPath(1024, 0), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/0/\",\"e\":[");
        b.append("{\"n\":\"1\",\"v\":22.9,\"t\":110},");
        b.append("{\"n\":\"0\",\"sv\":\"a string\",\"t\":120},");
        b.append("{\"n\":\"1\",\"v\":22.4,\"t\":120},");
        b.append("{\"n\":\"1\",\"v\":24.1,\"t\":130}]}");

        String expected = b.toString();
        Assert.assertEquals(expected, new String(encoded));
    }

    @Test
    public void json_encode_timestamped_Object() throws CodecException {
        List<TimestampedLwM2mNode> data = new ArrayList<>();

        LwM2mObject objectAt210 = new LwM2mObject(1204,
                new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 22.9)));

        LwM2mObject objectAt220 = new LwM2mObject(1204,
                new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 22.4),
                        LwM2mSingleResource.newStringResource(0, "a string")),
                new LwM2mObjectInstance(1, LwM2mSingleResource.newFloatResource(1, 23)));

        LwM2mObject objetAt230 = new LwM2mObject(1204,
                new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 24.1)));

        data.add(new TimestampedLwM2mNode(210L, objectAt210));
        data.add(new TimestampedLwM2mNode(220L, objectAt220));
        data.add(new TimestampedLwM2mNode(230L, objetAt230));

        byte[] encoded = encoder.encodeTimestampedData(data, ContentFormat.JSON, new LwM2mPath(1024), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/\",\"e\":[");
        b.append("{\"n\":\"0/1\",\"v\":22.9,\"t\":210},");
        b.append("{\"n\":\"0/0\",\"sv\":\"a string\",\"t\":220},");
        b.append("{\"n\":\"0/1\",\"v\":22.4,\"t\":220},");
        b.append("{\"n\":\"1/1\",\"v\":23,\"t\":220},");
        b.append("{\"n\":\"0/1\",\"v\":24.1,\"t\":230}]}");

        String expected = b.toString();
        Assert.assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_json_encode_device_object_instance() {
        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, getDeviceResources());
        byte[] encoded = encoder.encode(oInstance, ContentFormat.SENML_JSON, new LwM2mPath("/3/0"), model);

        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/3/0/\",\"n\":\"0\",\"vs\":\"Open Mobile Alliance\"},");
        b.append("{\"n\":\"1\",\"vs\":\"Lightweight M2M Client\"},");
        b.append("{\"n\":\"2\",\"vs\":\"345000123\"},");
        b.append("{\"n\":\"3\",\"vs\":\"1.0\"},");
        b.append("{\"n\":\"6/0\",\"v\":1},");
        b.append("{\"n\":\"6/1\",\"v\":5},");
        b.append("{\"n\":\"7/0\",\"v\":3800},");
        b.append("{\"n\":\"7/1\",\"v\":5000},");
        b.append("{\"n\":\"8/0\",\"v\":125},");
        b.append("{\"n\":\"8/1\",\"v\":900},");
        b.append("{\"n\":\"9\",\"v\":100},");
        b.append("{\"n\":\"10\",\"v\":15},");
        b.append("{\"n\":\"11/0\",\"v\":0},");
        b.append("{\"n\":\"13\",\"v\":1.3674912E9},");
        b.append("{\"n\":\"14\",\"vs\":\"+02:00\"},");
        b.append("{\"n\":\"16\",\"vs\":\"U\"}]");

        String expected = b.toString();
        Assert.assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_json_encode_single_resource() {
        LwM2mResource oResource = LwM2mSingleResource.newStringResource(0, "Open Mobile Alliance");
        byte[] encoded = encoder.encode(oResource, ContentFormat.SENML_JSON, new LwM2mPath("/3/0/0"), model);

        String expected = "[{\"bn\":\"/3/0/0\",\"vs\":\"Open Mobile Alliance\"}]";
        Assert.assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_json_encode_multiple_resource() {
        Map<Integer, Long> values = new HashMap<>();
        values.put(0, 3800L);
        values.put(1, 5000L);
        LwM2mResource oResource = LwM2mMultipleResource.newIntegerResource(7, values);
        byte[] encoded = encoder.encode(oResource, ContentFormat.SENML_JSON, new LwM2mPath("/3/0/7"), model);

        String expected = "[{\"bn\":\"/3/0/7/\",\"n\":\"0\",\"v\":3800},{\"n\":\"1\",\"v\":5000}]";
        Assert.assertEquals(expected, new String(encoded));
    }
}

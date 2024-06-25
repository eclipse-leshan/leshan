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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.senml.LwM2mNodeSenMLEncoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.senml.cbor.upokecenter.SenMLCborUpokecenterEncoderDecoder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LwM2mEncoder}
 */
public class LwM2mNodeEncoderTest {

    private static LwM2mModel model;
    private static LwM2mEncoder encoder;

    @BeforeAll
    public static void loadModel() {
        model = new StaticModel(ObjectLoader.loadDefault());

        // keep CBOR order to be able to test.
        Map<ContentFormat, NodeEncoder> defaultNodeEncoders = DefaultLwM2mEncoder.getDefaultNodeEncoders(false);
        defaultNodeEncoders.put(ContentFormat.SENML_CBOR,
                new LwM2mNodeSenMLEncoder(new SenMLCborUpokecenterEncoderDecoder(true, false)));

        encoder = new DefaultLwM2mEncoder(defaultNodeEncoders, DefaultLwM2mEncoder.getDefaultPathEncoder(),
                new LwM2mValueChecker());
    }

    @Test
    public void text_encode_single_resource_float() {

        byte[] encoded = encoder.encode(LwM2mSingleResource.newFloatResource(15, 56.4D), ContentFormat.TEXT,
                new LwM2mPath("/323/0/15"), model);

        assertEquals("56.4", new String(encoded, StandardCharsets.UTF_8));
    }

    @Test
    public void text_encode_single_resource_date() {

        byte[] encoded = encoder.encode(LwM2mSingleResource.newDateResource(13, new Date(1367491215000L)),
                ContentFormat.TEXT, new LwM2mPath("/3/0/13"), model);

        assertEquals("1367491215", new String(encoded, StandardCharsets.UTF_8));
    }

    @Test
    public void text_encode_multiple_instances() {
        Map<Integer, Long> values = new HashMap<>();
        values.put(0, 1L);
        values.put(1, 5L);

        assertThrowsExactly(CodecException.class, () -> {
            encoder.encode(LwM2mMultipleResource.newIntegerResource(6, values), ContentFormat.TEXT,
                    new LwM2mPath("/3/0/6"), model);
        });
    }

    @Test
    public void text_encode_opaque_as_base64_string() {
        byte[] opaqueValue = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5 };
        byte[] encoded = encoder.encode(LwM2mSingleResource.newBinaryResource(0, opaqueValue), ContentFormat.TEXT,
                new LwM2mPath("/5/0/0"), model);

        assertEquals("AQIDBAU=", new String(encoded, StandardCharsets.UTF_8));
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

        assertArrayEquals(ENCODED_DEVICE_WITHOUT_INSTANCE, encoded);
    }

    @Test
    public void tlv_encode_device_object_instance_as_resources_array__undefined_instance_id() {
        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(getDeviceResources());
        byte[] encoded = encoder.encode(oInstance, ContentFormat.TLV, new LwM2mPath("/3"), model);

        assertArrayEquals(ENCODED_DEVICE_WITHOUT_INSTANCE, encoded);
    }

    @Test
    public void tlv_encode_device_object_instance_as_instance() {
        LwM2mObjectInstance oInstance = new LwM2mObjectInstance(0, getDeviceResources());
        byte[] encoded = encoder.encode(oInstance, ContentFormat.TLV, new LwM2mPath("/3"), model);

        assertArrayEquals(ENCODED_DEVICE_WITH_INSTANCE, encoded);
    }

    @Test
    public void tlv_encode_device_object() {

        LwM2mObject object = new LwM2mObject(3, new LwM2mObjectInstance(0, getDeviceResources()));
        byte[] encoded = encoder.encode(object, ContentFormat.TLV, new LwM2mPath("/3"), model);

        // encoded as an array of resource TLVs
        assertArrayEquals(ENCODED_DEVICE_WITH_INSTANCE, encoded);
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
        b.append("{\"n\":\"13\",\"v\":1367491215},");
        b.append("{\"n\":\"14\",\"sv\":\"+02:00\"},");
        b.append("{\"n\":\"16\",\"sv\":\"U\"}]}");

        String expected = b.toString();
        assertEquals(expected, new String(encoded));
    }

    @Test
    public void json_encode_timestamped_resources() throws CodecException {
        List<TimestampedLwM2mNode> data = new ArrayList<>();
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(500), LwM2mSingleResource.newFloatResource(1, 22.9)));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(510), LwM2mSingleResource.newFloatResource(1, 22.4)));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(520), LwM2mSingleResource.newFloatResource(1, 24.1)));

        byte[] encoded = encoder.encodeTimestampedData(data, ContentFormat.JSON, new LwM2mPath(1024, 0, 1), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/0/1\",\"e\":[");
        b.append("{\"v\":22.9,\"t\":500},");
        b.append("{\"v\":22.4,\"t\":510},");
        b.append("{\"v\":24.1,\"t\":520}]}");

        String expected = b.toString();
        assertEquals(expected, new String(encoded));
    }

    @Test
    public void json_timestamped_resource_instances() throws CodecException {
        List<TimestampedLwM2mNode> data = new ArrayList<>();
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(500), LwM2mResourceInstance.newFloatInstance(0, 22.9)));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(510), LwM2mResourceInstance.newFloatInstance(0, 22.4)));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(520), LwM2mResourceInstance.newFloatInstance(0, 24.1)));

        byte[] encoded = encoder.encodeTimestampedData(data, ContentFormat.JSON, new LwM2mPath(1024, 0, 1, 0), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/0/1/0\",\"e\":[");
        b.append("{\"v\":22.9,\"t\":500},");
        b.append("{\"v\":22.4,\"t\":510},");
        b.append("{\"v\":24.1,\"t\":520}]}");

        String expected = b.toString();
        assertEquals(expected, new String(encoded));
    }

    @Test
    public void json_encode_timestamped_instances() throws CodecException {
        List<TimestampedLwM2mNode> data = new ArrayList<>();

        LwM2mObjectInstance instanceAt110 = new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 22.9));
        LwM2mObjectInstance instanceAt120 = new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 22.4),
                LwM2mSingleResource.newStringResource(0, "a string"));
        LwM2mObjectInstance instanceAt130 = new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(1, 24.1));

        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(110), instanceAt110));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(120), instanceAt120));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(130), instanceAt130));

        byte[] encoded = encoder.encodeTimestampedData(data, ContentFormat.JSON, new LwM2mPath(1024, 0), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/0/\",\"e\":[");
        b.append("{\"n\":\"1\",\"v\":22.9,\"t\":110},");
        b.append("{\"n\":\"0\",\"sv\":\"a string\",\"t\":120},");
        b.append("{\"n\":\"1\",\"v\":22.4,\"t\":120},");
        b.append("{\"n\":\"1\",\"v\":24.1,\"t\":130}]}");

        String expected = b.toString();
        assertEquals(expected, new String(encoded));
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

        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(210), objectAt210));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(220), objectAt220));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(230), objetAt230));

        byte[] encoded = encoder.encodeTimestampedData(data, ContentFormat.JSON, new LwM2mPath(1024), model);

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/\",\"e\":[");
        b.append("{\"n\":\"0/1\",\"v\":22.9,\"t\":210},");
        b.append("{\"n\":\"0/0\",\"sv\":\"a string\",\"t\":220},");
        b.append("{\"n\":\"0/1\",\"v\":22.4,\"t\":220},");
        b.append("{\"n\":\"1/1\",\"v\":23.0,\"t\":220},");
        b.append("{\"n\":\"0/1\",\"v\":24.1,\"t\":230}]}");

        String expected = b.toString();
        assertEquals(expected, new String(encoded));
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
        b.append("{\"n\":\"13\",\"v\":1367491215},");
        b.append("{\"n\":\"14\",\"vs\":\"+02:00\"},");
        b.append("{\"n\":\"16\",\"vs\":\"U\"}]");

        String expected = b.toString();
        assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_json_encode_single_resource() {
        LwM2mResource oResource = LwM2mSingleResource.newStringResource(0, "Open Mobile Alliance");
        byte[] encoded = encoder.encode(oResource, ContentFormat.SENML_JSON, new LwM2mPath("/3/0/0"), model);

        String expected = "[{\"bn\":\"/3/0/0\",\"vs\":\"Open Mobile Alliance\"}]";
        assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_json_encode_multiple_resource() {
        Map<Integer, Long> values = new HashMap<>();
        values.put(0, 3800L);
        values.put(1, 5000L);
        LwM2mResource oResource = LwM2mMultipleResource.newIntegerResource(7, values);
        byte[] encoded = encoder.encode(oResource, ContentFormat.SENML_JSON, new LwM2mPath("/3/0/7"), model);

        String expected = "[{\"bn\":\"/3/0/7/\",\"n\":\"0\",\"v\":3800},{\"n\":\"1\",\"v\":5000}]";
        assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_encode_timestamped_resources() throws CodecException {
        List<TimestampedLwM2mNode> data = new ArrayList<>();
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(268_500_000),
                LwM2mSingleResource.newFloatResource(1, 22.9)));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(268_500_010),
                LwM2mSingleResource.newFloatResource(1, 22.4)));
        data.add(new TimestampedLwM2mNode(Instant.ofEpochSecond(268_500_020),
                LwM2mSingleResource.newFloatResource(1, 24.1)));

        byte[] encoded = encoder.encodeTimestampedData(data, ContentFormat.SENML_JSON, new LwM2mPath(1024, 0, 1),
                model);

        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/1024/0/1\",\"bt\":268500000,\"v\":22.9},");
        b.append("{\"bn\":\"/1024/0/1\",\"bt\":268500010,\"v\":22.4},");
        b.append("{\"bn\":\"/1024/0/1\",\"bt\":268500020,\"v\":24.1}]");

        String expected = b.toString();
        assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_json_encode_timestamped_nodes() throws CodecException {
        Instant timestamp = Instant.ofEpochSecond(500_000_000);
        TimestampedLwM2mNodes timestampedLwM2mNodes = TimestampedLwM2mNodes.builder()
                .put(timestamp.plusSeconds(1), new LwM2mPath(0, 0, 0),
                        LwM2mSingleResource.newStringResource(0, "TestString"))
                .put(timestamp.plusSeconds(2), new LwM2mPath(0, 1),
                        new LwM2mObjectInstance(1, Arrays.asList(LwM2mSingleResource.newBooleanResource(1, true),
                                LwM2mSingleResource.newIntegerResource(2, 123))))
                .build();

        byte[] encoded = encoder.encodeTimestampedNodes(timestampedLwM2mNodes, ContentFormat.SENML_JSON, model);

        String expectedString = new StringBuilder()
                .append("[{\"bn\":\"/0/0/0\",\"bt\":500000001,\"vs\":\"TestString\"},") //
                .append("{\"bn\":\"/0/1/\",\"bt\":500000002,\"n\":\"1\",\"vb\":true},") //
                .append("{\"n\":\"2\",\"v\":123}]") //
                .toString();

        assertEquals(expectedString, new String(encoded));
    }

    @Test
    public void senml_cbor_encode_timestamped_nodes() throws CodecException {
        Instant timestamp = Instant.ofEpochSecond(500_000_000);
        TimestampedLwM2mNodes timestampedLwM2mNodes = TimestampedLwM2mNodes.builder()
                .put(timestamp.plusSeconds(4), new LwM2mPath(0, 0, 0),
                        LwM2mSingleResource.newStringResource(0, "SampleString"))
                .put(timestamp.plusSeconds(5), new LwM2mPath(0, 0, 1), LwM2mSingleResource.newBooleanResource(1, false))
                .put(timestamp.plusSeconds(6), new LwM2mPath(0, 0, 2), LwM2mSingleResource.newIntegerResource(2, 456))
                .build();

        byte[] encoded = encoder.encodeTimestampedNodes(timestampedLwM2mNodes, ContentFormat.SENML_CBOR, model);

        String expectedString = "83a321662f302f302f3022c482001a1dcd6504036c53616d706c65537472696e67a321662f302f302f3122c482001a1dcd650504f4a321662f302f302f3222c482001a1dcd6506021901c8";

        assertEquals(expectedString, Hex.encodeHexString(encoded));
    }

    @Test
    public void senml_json_encode_resources() {
        // Nodes to encode
        Map<LwM2mPath, LwM2mNode> nodes = new LinkedHashMap<>();
        nodes.put(new LwM2mPath("3/0/0"), LwM2mSingleResource.newStringResource(0, "Open Mobile Alliance"));
        nodes.put(new LwM2mPath("3/0/9"), LwM2mSingleResource.newIntegerResource(9, 95));
        nodes.put(new LwM2mPath("1/0/1"), LwM2mSingleResource.newIntegerResource(1, 86400));

        // Encode
        byte[] encoded = encoder.encodeNodes(nodes, ContentFormat.SENML_JSON, model);

        // Expected value
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/3/0/0\",\"vs\":\"Open Mobile Alliance\"},");
        b.append("{\"bn\":\"/3/0/9\",\"v\":95},");
        b.append("{\"bn\":\"/1/0/1\",\"v\":86400}]");
        String expected = b.toString();

        assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_json_encode_mixed_resource_and_instance() {
        // Nodes to encode
        Map<LwM2mPath, LwM2mNode> nodes = new LinkedHashMap<>();
        nodes.put(new LwM2mPath("4/0/0"), LwM2mSingleResource.newIntegerResource(0, 45));
        nodes.put(new LwM2mPath("4/0/1"), LwM2mSingleResource.newIntegerResource(1, 30));
        nodes.put(new LwM2mPath("4/0/2"), LwM2mSingleResource.newIntegerResource(2, 100));
        nodes.put(new LwM2mPath("6/0"), new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(0, 43.918998),
                LwM2mSingleResource.newFloatResource(1, 2.351149)));

        // Encode
        byte[] encoded = encoder.encodeNodes(nodes, ContentFormat.SENML_JSON, model);

        // Expected value
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/4/0/0\",\"v\":45},");
        b.append("{\"bn\":\"/4/0/1\",\"v\":30},");
        b.append("{\"bn\":\"/4/0/2\",\"v\":100},");
        b.append("{\"bn\":\"/6/0/\",\"n\":\"0\",\"v\":43.918998},");
        b.append("{\"n\":\"1\",\"v\":2.351149}]");
        String expected = b.toString();

        assertEquals(expected, new String(encoded));
    }

    @Test
    public void senml_json_encode_path_using_name() {
        // Prepare data to encode
        List<LwM2mPath> paths = Arrays.asList( //
                new LwM2mPath("4/0/0"), //
                new LwM2mPath("4/0/1"), //
                new LwM2mPath("4/0/2"));

        // Decode
        byte[] res = encoder.encodePaths(paths, ContentFormat.SENML_JSON);

        // Expected result
        StringBuilder b = new StringBuilder();
        b.append("[{\"n\":\"/4/0/0\"},");
        b.append("{\"n\":\"/4/0/1\"},");
        b.append("{\"n\":\"/4/0/2\"}]");
        assertEquals(b.toString(), new String(res));
    }

    @Test
    public void senml_json_encode_opaque_resource() {
        byte[] bytes = Hex.decodeHex("ABCDEF".toCharArray());
        LwM2mResource oResource = LwM2mSingleResource.newBinaryResource(3, bytes);
        byte[] json = encoder.encode(oResource, ContentFormat.SENML_JSON, new LwM2mPath("/0/0/3"), model);

        String expected = "[{\"bn\":\"/0/0/3\",\"vd\":\"q83v\"}]"; // q83v is base64 of ABCDE
        assertEquals(expected, new String(json));
    }

    @Test
    public void senml_cbor_encode_opaque_resource() {
        byte[] bytes = Hex.decodeHex("ABCDEF".toCharArray());
        LwM2mResource oResource = LwM2mSingleResource.newBinaryResource(3, bytes);
        byte[] cbor = encoder.encode(oResource, ContentFormat.SENML_CBOR, new LwM2mPath("/0/0/3"), model);
        // value : [{-2: "/0/0/3", 8: h'ABCDEF'}]
        String expected = "81a221662f302f302f330843abcdef";
        assertEquals(expected, Hex.encodeHexString(cbor));
    }
}

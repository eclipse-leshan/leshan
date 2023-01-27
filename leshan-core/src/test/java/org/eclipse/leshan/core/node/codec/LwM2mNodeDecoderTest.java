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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.json.LwM2mJsonException;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.tlv.Tlv;
import org.eclipse.leshan.core.tlv.Tlv.TlvType;
import org.eclipse.leshan.core.tlv.TlvEncoder;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.core.util.TestObjectLoader;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LwM2mDecoder}
 */
public class LwM2mNodeDecoderTest {

    private static LwM2mModel model;
    private static LwM2mDecoder decoder;

    @BeforeAll
    public static void loadModel() {
        // load default object
        List<ObjectModel> objects = TestObjectLoader.loadAllDefault();

        // add object 65 from the LWM2M v1.0.1 specification (figure 28)
        List<ResourceModel> resForObj65 = new ArrayList<>();
        resForObj65.add(new ResourceModel(0, "res0", Operations.R, true, false, Type.OBJLNK, null, null, null));
        resForObj65.add(new ResourceModel(1, "res1", Operations.R, false, false, Type.STRING, null, null, null));
        resForObj65.add(new ResourceModel(2, "res2", Operations.R, false, false, Type.INTEGER, null, null, null));
        objects.add(
                new ObjectModel(65, "object link tests 65", "", ObjectModel.DEFAULT_VERSION, true, false, resForObj65));

        // add object 66 from the LWM2M v1.0.1 specification (figure 28)
        List<ResourceModel> resForObj66 = new ArrayList<>();
        resForObj66.add(new ResourceModel(0, "res0", Operations.R, true, false, Type.STRING, null, null, null));
        resForObj66.add(new ResourceModel(1, "res1", Operations.R, false, false, Type.STRING, null, null, null));
        resForObj66.add(new ResourceModel(2, "res2", Operations.R, false, false, Type.OBJLNK, null, null, null));
        objects.add(
                new ObjectModel(66, "object link tests 66", "", ObjectModel.DEFAULT_VERSION, true, false, resForObj66));

        model = new StaticModel(objects);
        decoder = new DefaultLwM2mDecoder();
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

    // tlv content for multi instance ACL (encoded as an array of 2 Object instances TLV)
    // Example from LWM2M spec §4.3.2 B)
    private final static byte[] ENCODED_ACL = Hex
            .decodeHex("08000EC10001C101008302417F07C1037F080212C10003C101008702417F0761013601C1037F".toCharArray());

    // tlv content for multi instance SERVER (encoded as an array of 1 Object instance TLV)
    // Example from LWM2M spec §4.3.2 C)
    private final static byte[] ENCODED_SERVER = Hex.decodeHex("08000FC10001C40100015180C10601C10755".toCharArray());

    // tlv content for instance OBJ65 (encoded as an array of 3 resource TLV)
    // Example from LWM2M spec §4.3.3 1)
    private final static byte[] ENCODED_OBJ65 = Hex
            .decodeHex("88000C440000420000440100420001C8010D38363133383030373535353030C40212345678".toCharArray());

    // tlv content for multi instance OBJ66 (encoded as an array of 2 instance TLV)
    // Example from LWM2M spec §4.3.3 2)
    private final static byte[] ENCODED_OBJ66 = Hex.decodeHex(
            "080026C8000B6D79536572766963652031C8010F496E7465726E65742E31352E323334C40200430000080126C8000B6D79536572766963652032C8010F496E7465726E65742E31352E323335C402FFFFFFFF"
                    .toCharArray());

    // tlv content for object 3442 instance (contain core link value)
    private final static byte[] ENCODED_OBJ3442 = Hex.decodeHex(
            "080072c8b46f3c2f3e3b72743d226f6d612e6c776d326d223b63743d223630203131302031313220313534322031353433203131353432203131353433222c3c2f313e3b7665723d312e312c3c2f312f303e2c3c2f323e2c3c2f333e3b7665723d312e312c3c2f332f303e2c3c2f333434322f303e"
                    .toCharArray());

    private void assertDeviceInstance(LwM2mObjectInstance oInstance) {
        assertEquals(0, oInstance.getId());

        assertEquals("Open Mobile Alliance", oInstance.getResource(0).getValue());
        assertEquals("Lightweight M2M Client", oInstance.getResource(1).getValue());
        assertEquals("345000123", oInstance.getResource(2).getValue());
        assertEquals("1.0", oInstance.getResource(3).getValue());
        assertNull(oInstance.getResource(4));
        assertNull(oInstance.getResource(5));
        assertEquals(2, oInstance.getResource(6).getInstances().size());
        assertEquals(1L, oInstance.getResource(6).getValue(0));
        assertEquals(5L, oInstance.getResource(6).getValue(1));
        assertEquals(3800L, oInstance.getResource(7).getValue(0));
        assertEquals(5000L, oInstance.getResource(7).getValue(1));
        assertEquals(125L, oInstance.getResource(8).getValue(0));
        assertEquals(900L, oInstance.getResource(8).getValue(1));
        assertEquals(100L, oInstance.getResource(9).getValue());
        assertEquals(15L, oInstance.getResource(10).getValue());
        assertEquals(1, oInstance.getResource(11).getInstances().size());
        assertEquals(0L, oInstance.getResource(11).getValue(0));
        assertNull(oInstance.getResource(12));
        assertEquals(new Date(1367491215000L), oInstance.getResource(13).getValue());
        assertEquals("+02:00", oInstance.getResource(14).getValue());
        assertEquals("U", oInstance.getResource(16).getValue());

    }

    private void assertAclInstances(LwM2mObject oObject) {
        assertEquals(2, oObject.getId());

        assertEquals(2, oObject.getInstances().size());
        // instance 1
        LwM2mObjectInstance oInstance0 = oObject.getInstance(0);
        assertEquals(1L, oInstance0.getResource(0).getValue());
        assertEquals(0L, oInstance0.getResource(1).getValue());
        assertEquals(1, oInstance0.getResource(2).getInstances().size());
        assertEquals(7L, oInstance0.getResource(2).getValue(127));
        assertEquals(127L, oInstance0.getResource(3).getValue());

        // instance 2
        LwM2mObjectInstance oInstance2 = oObject.getInstance(2);
        assertEquals(3L, oInstance2.getResource(0).getValue());
        assertEquals(0L, oInstance2.getResource(1).getValue());
        assertEquals(2, oInstance2.getResource(2).getInstances().size());
        assertEquals(7L, oInstance2.getResource(2).getValue(127));
        assertEquals(1L, oInstance2.getResource(2).getValue(310));
        assertEquals(127L, oInstance2.getResource(3).getValue());
    }

    private void assertServerInstance(LwM2mObject oObject) {
        assertEquals(1, oObject.getId());

        assertEquals(1, oObject.getInstances().size());
        LwM2mObjectInstance oInstance0 = oObject.getInstance(0);
        assertEquals(1L, oInstance0.getResource(0).getValue());
        assertEquals(86400L, oInstance0.getResource(1).getValue());
        assertEquals(true, oInstance0.getResource(6).getValue());
        assertEquals("U", oInstance0.getResource(7).getValue());
    }

    private void assertObj65Instance(LwM2mObjectInstance instance) {
        assertEquals(0, instance.getId());

        // instance 1
        assertEquals(2, instance.getResource(0).getInstances().size());
        assertEquals(new ObjectLink(66, 0), instance.getResource(0).getValue(0));
        assertEquals(new ObjectLink(66, 1), instance.getResource(0).getValue(1));
        assertEquals("8613800755500", instance.getResource(1).getValue());
        assertEquals(305419896L, instance.getResource(2).getValue());
    }

    private void assertObj66Instance(LwM2mObject oObject) {
        assertEquals(66, oObject.getId());

        assertEquals(2, oObject.getInstances().size());
        // instance 1
        LwM2mObjectInstance oInstance0 = oObject.getInstance(0);
        assertEquals("myService 1", oInstance0.getResource(0).getValue());
        assertEquals("Internet.15.234", oInstance0.getResource(1).getValue());
        assertEquals(new ObjectLink(67, 0), oInstance0.getResource(2).getValue());

        // instance 2
        LwM2mObjectInstance oInstance2 = oObject.getInstance(1);
        assertEquals("myService 2", oInstance2.getResource(0).getValue());
    }

    private void assertObj3442Instance(LwM2mObjectInstance instance) {
        assertEquals(0, instance.getId());

        // instance 0
        assertEquals(1, instance.getResources().size());

        Link[] links = (Link[]) instance.getResource(TestLwM2mId.CORELNK_VALUE).getValue();

        assertEquals(7, links.length);

        assertEquals("/", links[0].getUriReference());
        assertEquals(2, links[0].getAttributes().asCollection().size());

        assertEquals("/1", links[1].getUriReference());
        assertEquals(1, links[1].getAttributes().asCollection().size());

        assertEquals("/1/0", links[2].getUriReference());
        assertEquals(0, links[2].getAttributes().asCollection().size());

        assertEquals("/2", links[3].getUriReference());
        assertEquals(0, links[3].getAttributes().asCollection().size());

        assertEquals("/3", links[4].getUriReference());
        assertEquals(1, links[4].getAttributes().asCollection().size());

        assertEquals("/3/0", links[5].getUriReference());
        assertEquals(0, links[5].getAttributes().asCollection().size());

        assertEquals("/3442/0", links[6].getUriReference());
        assertEquals(0, links[6].getAttributes().asCollection().size());
    }

    @Test
    public void text_manufacturer_resource() throws CodecException {
        String value = "MyManufacturer";
        LwM2mSingleResource resource = (LwM2mSingleResource) decoder.decode(value.getBytes(StandardCharsets.UTF_8),
                ContentFormat.TEXT, new LwM2mPath(3, 0, 0), model);

        assertEquals(0, resource.getId());
        assertFalse(resource.isMultiInstances());
        assertEquals(Type.STRING, resource.getType());
        assertEquals(value, resource.getValue());
    }

    @Test()
    public void content_format_is_mandatory() throws CodecException {
        assertThrowsExactly(CodecException.class, () -> {
            String value = "MyManufacturer";
            decoder.decode(value.getBytes(StandardCharsets.UTF_8), null, new LwM2mPath(666, 0, 0), model);
        });
    }

    @Test
    public void text_battery_resource() throws CodecException {
        LwM2mSingleResource resource = (LwM2mSingleResource) decoder.decode("100".getBytes(StandardCharsets.UTF_8),
                ContentFormat.TEXT, new LwM2mPath(3, 0, 9), model);

        assertEquals(9, resource.getId());
        assertFalse(resource.isMultiInstances());
        assertEquals(Type.INTEGER, resource.getType());
        assertEquals(100, ((Number) resource.getValue()).intValue());
    }

    @Test
    public void text_decode_opaque_from_base64_string() throws CodecException {
        // Using Firmware Update/Package
        LwM2mSingleResource resource = (LwM2mSingleResource) decoder.decode("AQIDBAU=".getBytes(StandardCharsets.UTF_8),
                ContentFormat.TEXT, new LwM2mPath(5, 0, 0), model);

        byte[] expectedValue = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5 };

        assertEquals(0, resource.getId());
        assertFalse(resource.isMultiInstances());
        assertEquals(Type.OPAQUE, resource.getType());
        assertArrayEquals(expectedValue, ((byte[]) resource.getValue()));
    }

    @Test()
    public void text_decode_should_throw_an_exception_for_invalid_base64() throws CodecException {
        assertThrowsExactly(CodecException.class, () -> {
            // Using Firmware Update/Package
            decoder.decode("!,-INVALID$_'".getBytes(StandardCharsets.UTF_8), ContentFormat.TEXT, new LwM2mPath(5, 0, 0),
                    model);
        });
    }

    @Test
    public void tlv_manufacturer_resource() throws CodecException {
        String value = "MyManufacturer";
        byte[] content = TlvEncoder.encode(new Tlv[] { new Tlv(TlvType.RESOURCE_VALUE, null, value.getBytes(), 0) })
                .array();
        LwM2mSingleResource resource = (LwM2mSingleResource) decoder.decode(content, ContentFormat.TLV,
                new LwM2mPath(3, 0, 0), model);

        assertEquals(0, resource.getId());
        assertFalse(resource.isMultiInstances());
        assertEquals(value, resource.getValue());
    }

    @Test
    public void tlv_device_object_instance0_from_resources_tlv() throws CodecException {

        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) decoder.decode(ENCODED_DEVICE_WITHOUT_INSTANCE,
                ContentFormat.TLV, new LwM2mPath(3, 0), model);
        assertDeviceInstance(oInstance);
    }

    @Test
    public void tlv_device_object_instance0_from_resources_tlv__instance_expected() throws CodecException {

        LwM2mObjectInstance oInstance = decoder.decode(ENCODED_DEVICE_WITHOUT_INSTANCE, ContentFormat.TLV,
                new LwM2mPath(3), model, LwM2mObjectInstance.class);
        assertDeviceInstance(oInstance);
    }

    @Test
    public void tlv_device_object_instance0_from_instance_tlv() throws CodecException {

        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) decoder.decode(ENCODED_DEVICE_WITH_INSTANCE,
                ContentFormat.TLV, new LwM2mPath(3, 0), model);
        assertDeviceInstance(oInstance);
    }

    @Test
    public void tlv_server_object_multi_instance_with_only_1_instance() throws Exception {
        LwM2mObject oObject = ((LwM2mObject) decoder.decode(ENCODED_SERVER, ContentFormat.TLV, new LwM2mPath(1),
                model));
        assertServerInstance(oObject);
    }

    @Test
    public void tlv_acl_object_multi_instance() throws Exception {
        LwM2mObject oObject = ((LwM2mObject) decoder.decode(ENCODED_ACL, ContentFormat.TLV, new LwM2mPath(2), model));
        assertAclInstances(oObject);
    }

    @Test()
    public void tlv_invalid_object_2_instances_with_the_same_id() {
        Tlv objInstance1 = new Tlv(TlvType.OBJECT_INSTANCE, new Tlv[0], null, 1);
        Tlv objInstance2 = new Tlv(TlvType.OBJECT_INSTANCE, new Tlv[0], null, 1);
        byte[] content = TlvEncoder.encode(new Tlv[] { objInstance1, objInstance2 }).array();

        assertThrowsExactly(CodecException.class, () -> {
            decoder.decode(content, ContentFormat.TLV, new LwM2mPath(2), model);
        });

    }

    @Test()
    public void tlv_invalid_object__instance_2_resources_with_the_same_id() {
        Tlv resource1 = new Tlv(TlvType.RESOURCE_VALUE, null, new byte[0], 1);
        Tlv resource2 = new Tlv(TlvType.RESOURCE_VALUE, null, new byte[0], 1);
        byte[] content = TlvEncoder.encode(new Tlv[] { resource1, resource2 }).array();

        assertThrowsExactly(CodecException.class, () -> {
            decoder.decode(content, ContentFormat.TLV, new LwM2mPath(3, 0), model);
        });

    }

    @Test
    public void tlv_single_instance_with_obj_link() throws Exception {
        LwM2mObjectInstance oInstance = ((LwM2mObjectInstance) decoder.decode(ENCODED_OBJ65, ContentFormat.TLV,
                new LwM2mPath(65, 0), model));
        assertObj65Instance(oInstance);
    }

    @Test
    public void tlv_multi_instance_with_obj_link() throws Exception {
        LwM2mObject oObject = ((LwM2mObject) decoder.decode(ENCODED_OBJ66, ContentFormat.TLV, new LwM2mPath(66),
                model));
        assertObj66Instance(oObject);
    }

    @Test
    public void tlv_power_source__array_values() throws CodecException {
        byte[] content = new byte[] { 65, 0, 1, 65, 1, 5 };

        LwM2mResource resource = (LwM2mResource) decoder.decode(content, ContentFormat.TLV, new LwM2mPath(3, 0, 6),
                model);

        assertEquals(6, resource.getId());
        assertEquals(2, resource.getInstances().size());
        assertEquals(1L, resource.getValue(0));
        assertEquals(5L, resource.getValue(1));
    }

    @Test
    public void tlv_power_source__multiple_resource() throws CodecException {
        // this content (a single TLV of type 'multiple_resource' containing the values)
        // is probably not compliant with the spec but it should be supported by the server
        byte[] content = new byte[] { -122, 6, 65, 0, 1, 65, 1, 5 };

        LwM2mResource resource = (LwM2mResource) decoder.decode(content, ContentFormat.TLV, new LwM2mPath(3, 0, 6),
                model);

        assertEquals(6, resource.getId());
        assertEquals(2, resource.getInstances().size());
        assertEquals(1L, resource.getValue(0));
        assertEquals(5L, resource.getValue(1));
    }

    @Test
    public void tlv_instance_without_id_tlv() throws CodecException {
        // this is "special" case where instance ID is not defined ...
        byte[] content = TlvEncoder
                .encode(new Tlv[] { new Tlv(TlvType.RESOURCE_VALUE, null, TlvEncoder.encodeInteger(11), 1),
                        new Tlv(TlvType.RESOURCE_VALUE, null, TlvEncoder.encodeInteger(10), 2) })
                .array();

        LwM2mObject object = (LwM2mObject) decoder.decode(content, ContentFormat.TLV, new LwM2mPath(2), model);
        assertEquals(object.getInstances().size(), 1);
        assertEquals(object.getInstances().values().iterator().next().getId(), LwM2mObjectInstance.UNDEFINED);
    }

    @Test
    public void tlv_unknown_object__missing_instance_tlv() throws CodecException {

        byte[] content = TlvEncoder.encode(new Tlv[] { new Tlv(TlvType.RESOURCE_VALUE, null, "value1".getBytes(), 1),
                new Tlv(TlvType.RESOURCE_VALUE, null, "value1".getBytes(), 2) }).array();

        LwM2mObject obj = (LwM2mObject) decoder.decode(content, ContentFormat.TLV, new LwM2mPath(10234), model);

        assertEquals(1, obj.getInstances().size());
        assertEquals(2, obj.getInstance(0).getResources().size());
    }

    @Test
    public void tlv_resource_with_undesired_object_instance() throws CodecException {

        Tlv resInstance1 = new Tlv(TlvType.RESOURCE_VALUE, null, "client".getBytes(), 1);
        Tlv objInstance = new Tlv(TlvType.OBJECT_INSTANCE, new Tlv[] { resInstance1 }, null, 0);
        byte[] content = TlvEncoder.encode(new Tlv[] { objInstance }).array();

        LwM2mSingleResource res = (LwM2mSingleResource) decoder.decode(content, ContentFormat.TLV,
                new LwM2mPath(3, 0, 1), model);

        assertEquals("client", res.getValue());
    }

    @Test()
    public void tlv_resource_with_undesired_invalid_object_instance() throws CodecException {

        Tlv resInstance1 = new Tlv(TlvType.RESOURCE_VALUE, null, "client".getBytes(), 1);
        Tlv objInstance = new Tlv(TlvType.OBJECT_INSTANCE, new Tlv[] { resInstance1 }, null, 1);
        byte[] content = TlvEncoder.encode(new Tlv[] { objInstance }).array();

        assertThrowsExactly(CodecException.class, () -> {
            decoder.decode(content, ContentFormat.TLV, new LwM2mPath(3, 0, 1), model);
        });
    }

    @Test
    public void tlv_empty_object() {
        byte[] content = TlvEncoder.encode(new Tlv[] {}).array();

        LwM2mObject obj = (LwM2mObject) decoder.decode(content, ContentFormat.TLV, new LwM2mPath(2), model);

        assertNotNull(obj);
        assertEquals(2, obj.getId());
        assertTrue(obj.getInstances().isEmpty());
    }

    @Test
    public void tlv_empty_instance() {
        byte[] content = TlvEncoder.encode(new Tlv[] {}).array();

        LwM2mObjectInstance instance = (LwM2mObjectInstance) decoder.decode(content, ContentFormat.TLV,
                new LwM2mPath(2, 0), model);

        assertNotNull(instance);
        assertEquals(0, instance.getId());
        assertTrue(instance.getResources().isEmpty());
    }

    @Test()
    public void tlv_empty_single_resource() {
        byte[] content = TlvEncoder.encode(new Tlv[] {}).array();

        assertThrowsExactly(CodecException.class, () -> {
            decoder.decode(content, ContentFormat.TLV, new LwM2mPath(2, 0, 0), model);
        });

    }

    @Test
    public void tlv_empty_multi_resource() {
        byte[] content = TlvEncoder.encode(new Tlv[] {}).array();

        LwM2mResource resource = (LwM2mResource) decoder.decode(content, ContentFormat.TLV, new LwM2mPath(3, 0, 6),
                model);

        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(6, resource.getId());
        assertTrue(resource.getInstances().isEmpty());
    }

    @Test()
    public void tlv_invalid_multi_resource_2_instance_with_the_same_id() {
        Tlv resInstance1 = new Tlv(TlvType.RESOURCE_INSTANCE, null, TlvEncoder.encodeObjlnk(new ObjectLink(100, 1)), 0);
        Tlv resInstance2 = new Tlv(TlvType.RESOURCE_INSTANCE, null, TlvEncoder.encodeObjlnk(new ObjectLink(101, 2)), 0);
        Tlv multiResource = new Tlv(TlvType.MULTIPLE_RESOURCE, new Tlv[] { resInstance1, resInstance2 }, null, 22);
        byte[] content = TlvEncoder.encode(new Tlv[] { multiResource }).array();

        assertThrowsExactly(CodecException.class, () -> {
            decoder.decode(content, ContentFormat.TLV, new LwM2mPath(3, 0, 22), model);
        });
    }

    @Test
    public void tlv_instance_with_core_link_value() {
        LwM2mObjectInstance instance = (LwM2mObjectInstance) decoder.decode(ENCODED_OBJ3442, ContentFormat.TLV,
                new LwM2mPath(3442, 0), model);
        assertObj3442Instance(instance);
    }

    @Test
    public void json_device_object_instance0() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/3/0/\",");
        b.append("\"e\":[");
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

        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) decoder.decode(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(3, 0), model);

        assertDeviceInstance(oInstance);
    }

    @Test
    public void json_device_object_instance0_with_empty_root_basename() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/\",");
        b.append("\"e\":[");
        b.append("{\"n\":\"3/0/0\",\"sv\":\"Open Mobile Alliance\"},");
        b.append("{\"n\":\"3/0/1\",\"sv\":\"Lightweight M2M Client\"},");
        b.append("{\"n\":\"3/0/2\",\"sv\":\"345000123\"},");
        b.append("{\"n\":\"3/0/3\",\"sv\":\"1.0\"},");
        b.append("{\"n\":\"3/0/6/0\",\"v\":1},");
        b.append("{\"n\":\"3/0/6/1\",\"v\":5},");
        b.append("{\"n\":\"3/0/7/0\",\"v\":3800},");
        b.append("{\"n\":\"3/0/7/1\",\"v\":5000},");
        b.append("{\"n\":\"3/0/8/0\",\"v\":125},");
        b.append("{\"n\":\"3/0/8/1\",\"v\":900},");
        b.append("{\"n\":\"3/0/9\",\"v\":100},");
        b.append("{\"n\":\"3/0/10\",\"v\":15},");
        b.append("{\"n\":\"3/0/11/0\",\"v\":0},");
        b.append("{\"n\":\"3/0/13\",\"v\":1367491215},");
        b.append("{\"n\":\"3/0/14\",\"sv\":\"+02:00\"},");
        b.append("{\"n\":\"3/0/16\",\"sv\":\"U\"}]}");

        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) decoder.decode(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(3, 0), model);

        assertDeviceInstance(oInstance);
    }

    @Test
    public void json_device_object_instance0_without_basename() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("{\"e\":[");
        b.append("{\"n\":\"/3/0/0\",\"sv\":\"Open Mobile Alliance\"},");
        b.append("{\"n\":\"/3/0/1\",\"sv\":\"Lightweight M2M Client\"},");
        b.append("{\"n\":\"/3/0/2\",\"sv\":\"345000123\"},");
        b.append("{\"n\":\"/3/0/3\",\"sv\":\"1.0\"},");
        b.append("{\"n\":\"/3/0/6/0\",\"v\":1},");
        b.append("{\"n\":\"/3/0/6/1\",\"v\":5},");
        b.append("{\"n\":\"/3/0/7/0\",\"v\":3800},");
        b.append("{\"n\":\"/3/0/7/1\",\"v\":5000},");
        b.append("{\"n\":\"/3/0/8/0\",\"v\":125},");
        b.append("{\"n\":\"/3/0/8/1\",\"v\":900},");
        b.append("{\"n\":\"/3/0/9\",\"v\":100},");
        b.append("{\"n\":\"/3/0/10\",\"v\":15},");
        b.append("{\"n\":\"/3/0/11/0\",\"v\":0},");
        b.append("{\"n\":\"/3/0/13\",\"v\":1367491215},");
        b.append("{\"n\":\"/3/0/14\",\"sv\":\"+02:00\"},");
        b.append("{\"n\":\"/3/0/16\",\"sv\":\"U\"}]}");

        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) decoder.decode(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(3, 0), model);

        assertDeviceInstance(oInstance);
    }

    @Test
    public void json_custom_object_instance() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"1024/0/\", \"e\":[");
        b.append("{\"n\":\"0\",\"sv\":\"a string\"},");
        b.append("{\"n\":\"1\",\"v\":10.5},");
        b.append("{\"n\":\"2\",\"bv\":true}]}");
        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) decoder.decode(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(1024, 0), model);

        assertEquals(0, oInstance.getId());

        assertEquals("a string", oInstance.getResource(0).getValue());
        assertEquals(10.5, oInstance.getResource(1).getValue());
        assertEquals(true, oInstance.getResource(2).getValue());
    }

    @Test
    public void json_timestamped_resources() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/0/1\",\"e\":[");
        b.append("{\"v\":22.9,\"t\":-30},");
        b.append("{\"v\":22.4,\"t\":-5},");
        b.append("{\"v\":24.1,\"t\":-50}],");
        b.append("\"bt\":25462634}");

        List<TimestampedLwM2mNode> timestampedResources = decoder.decodeTimestampedData(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(1024, 0, 1), model);

        assertEquals(3, timestampedResources.size());
        assertEquals(Instant.ofEpochSecond(25462634L - 5), timestampedResources.get(0).getTimestamp());
        assertEquals(22.4d, ((LwM2mResource) timestampedResources.get(0).getNode()).getValue());
        assertEquals(Instant.ofEpochSecond(25462634L - 30), timestampedResources.get(1).getTimestamp());
        assertEquals(22.9d, ((LwM2mResource) timestampedResources.get(1).getNode()).getValue());
        assertEquals(Instant.ofEpochSecond(25462634L - 50), timestampedResources.get(2).getTimestamp());
        assertEquals(24.1d, ((LwM2mResource) timestampedResources.get(2).getNode()).getValue());
    }

    @Test
    public void json_timestamped_instances() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/0/\",\"e\":[");
        b.append("{\"n\":\"1\",\"v\":22.9,\"t\":-30},");
        b.append("{\"n\":\"1\",\"v\":22.4,\"t\":-5},");
        b.append("{\"n\":\"0\",\"sv\":\"a string\",\"t\":-5},");
        b.append("{\"n\":\"1\",\"v\":24.1,\"t\":-50}],");
        b.append("\"bt\":25462634}");

        List<TimestampedLwM2mNode> timestampedResources = decoder.decodeTimestampedData(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(1024, 0), model);

        assertEquals(3, timestampedResources.size());
        assertEquals(Instant.ofEpochSecond(25462634L - 5), timestampedResources.get(0).getTimestamp());
        assertEquals("a string",
                ((LwM2mObjectInstance) timestampedResources.get(0).getNode()).getResource(0).getValue());
        assertEquals(22.4d, ((LwM2mObjectInstance) timestampedResources.get(0).getNode()).getResource(1).getValue());

        assertEquals(Instant.ofEpochSecond(25462634L - 30), timestampedResources.get(1).getTimestamp());
        assertEquals(22.9d, ((LwM2mObjectInstance) timestampedResources.get(1).getNode()).getResource(1).getValue());

        assertEquals(Instant.ofEpochSecond(25462634L - 50), timestampedResources.get(2).getTimestamp());
        assertEquals(24.1d, ((LwM2mObjectInstance) timestampedResources.get(2).getNode()).getResource(1).getValue());
    }

    @Test
    public void json_timestamped_Object() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/1024/\",\"e\":[");
        b.append("{\"n\":\"0/1\",\"v\":22.9,\"t\":-30},");
        b.append("{\"n\":\"0/1\",\"v\":22.4,\"t\":-5},");
        b.append("{\"n\":\"0/0\",\"sv\":\"a string\",\"t\":-5},");
        b.append("{\"n\":\"1/1\",\"v\":23,\"t\":-5},");
        b.append("{\"n\":\"0/1\",\"v\":24.1,\"t\":-50}],");
        b.append("\"bt\":25462634}");

        List<TimestampedLwM2mNode> timestampedResources = decoder.decodeTimestampedData(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(1024), model);

        assertEquals(3, timestampedResources.size());
        assertEquals(Instant.ofEpochSecond(25462634L - 5), timestampedResources.get(0).getTimestamp());
        assertEquals(22.4d,
                ((LwM2mObject) timestampedResources.get(0).getNode()).getInstance(0).getResource(1).getValue());
        assertEquals("a string",
                ((LwM2mObject) timestampedResources.get(0).getNode()).getInstance(0).getResource(0).getValue());
        assertEquals(23.0d,
                ((LwM2mObject) timestampedResources.get(0).getNode()).getInstance(1).getResource(1).getValue());
        assertEquals(Instant.ofEpochSecond(25462634L - 30), timestampedResources.get(1).getTimestamp());
        assertEquals(22.9d,
                ((LwM2mObject) timestampedResources.get(1).getNode()).getInstance(0).getResource(1).getValue());
        assertEquals(Instant.ofEpochSecond(25462634L - 50), timestampedResources.get(2).getTimestamp());
        assertEquals(24.1d,
                ((LwM2mObject) timestampedResources.get(2).getNode()).getInstance(0).getResource(1).getValue());
    }

    @Test
    public void json_empty_object() {
        // Completely empty
        LwM2mObject obj = null;
        StringBuilder b = new StringBuilder();
        b.append("{}");
        boolean failedWithCodecException = false;
        try {
            obj = (LwM2mObject) decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(2), model);
        } catch (CodecException e) {
            assertTrue(e.getCause() instanceof LwM2mJsonException);
            failedWithCodecException = true;
        }
        assertTrue(failedWithCodecException, "Should failed with codec exception");

        // with empty resource list
        b = new StringBuilder();
        b.append("{\"e\":[]}");
        obj = (LwM2mObject) decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(2), model);
        assertNotNull(obj);
        assertEquals(2, obj.getId());
        assertTrue(obj.getInstances().isEmpty());

        // with empty resources list and base name
        b = new StringBuilder();
        b.append("{\"bn\":\"2\", \"e\":[]}");
        obj = (LwM2mObject) decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(2), model);
        assertNotNull(obj);
        assertEquals(2, obj.getId());
        assertTrue(obj.getInstances().isEmpty());
    }

    @Test
    public void json_empty_instance() {
        // Completely empty
        LwM2mObjectInstance instance = null;
        StringBuilder b = new StringBuilder();
        b.append("{}");
        boolean failedWithCodecException = false;
        try {
            instance = (LwM2mObjectInstance) decoder.decode(b.toString().getBytes(), ContentFormat.JSON,
                    new LwM2mPath(2, 0), model);
        } catch (CodecException e) {
            assertTrue(e.getCause() instanceof LwM2mJsonException);
            failedWithCodecException = true;
        }
        assertTrue(failedWithCodecException, "Should failed with codec exception");

        // with empty resource list
        b = new StringBuilder();
        b.append("{\"e\":[]}");
        instance = (LwM2mObjectInstance) decoder.decode(b.toString().getBytes(), ContentFormat.JSON,
                new LwM2mPath(2, 0), model);
        assertNotNull(instance);
        assertEquals(0, instance.getId());
        assertTrue(instance.getResources().isEmpty());

        // with empty resources list and base name
        b = new StringBuilder();
        b.append("{\"bn\":\"2/0\", \"e\":[]}");
        instance = (LwM2mObjectInstance) decoder.decode(b.toString().getBytes(), ContentFormat.JSON,
                new LwM2mPath(2, 0), model);
        assertNotNull(instance);
        assertEquals(0, instance.getId());
        assertTrue(instance.getResources().isEmpty());
    }

    @Test()
    public void json_empty_single_resource() {
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"2/0/0\", \"e\":[]}");

        assertThrowsExactly(CodecException.class, () -> {
            decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(2, 0, 0), model);
        });
    }

    @Test
    public void json_empty_multi_resource() {
        // Completely empty
        LwM2mResource resource = null;
        StringBuilder b = new StringBuilder();
        b.append("{}");
        boolean failedWithCodecException = false;
        try {
            resource = (LwM2mResource) decoder.decode(b.toString().getBytes(), ContentFormat.JSON,
                    new LwM2mPath(3, 0, 6), model);
        } catch (CodecException e) {
            assertTrue(e.getCause() instanceof LwM2mJsonException);
            failedWithCodecException = true;
        }
        assertTrue(failedWithCodecException, "Should failed with codec exception");

        // with empty resource list
        b = new StringBuilder();
        b.append("{\"e\":[]}");
        resource = (LwM2mResource) decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(3, 0, 6),
                model);
        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(6, resource.getId());
        assertTrue(resource.getInstances().isEmpty());

        // with empty resources list and base name
        b = new StringBuilder();
        b.append("{\"bn\":\"3/0/6\", \"e\":[]}");
        resource = (LwM2mResource) decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(3, 0, 6),
                model);
        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(6, resource.getId());
        assertTrue(resource.getInstances().isEmpty());
    }

    @Test
    public void json_missing_value_for_resource() {

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"2/0/\",\"e\":[");
        b.append("{\"n\":\"0\"}");
        b.append("]}");
        try {
            decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(2, 0, 0), model);
            fail();
        } catch (CodecException e) {
            assertTrue(e.getCause() instanceof LwM2mJsonException);
        }
    }

    @Test()
    public void json_invalid_instance_2_resources_with_the_same_id() {
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"3/0/\",\"e\":[");
        b.append("{\"n\":\"1\",\"sv\":\"client1\"},");
        b.append("{\"n\":\"1\",\"sv\":\"client2\"}");
        b.append("]}");

        assertThrowsExactly(CodecException.class, () -> {
            decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(3, 0), model);
        });
    }

    @Test()
    public void json_invalid_multi_resource_2_instances_with_the_same_id() {
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"3/0/11/\",\"e\":[");
        b.append("{\"n\":\"1\",\"v\":2},");
        b.append("{\"n\":\"1\",\"v\":0}");
        b.append("]}");

        assertThrowsExactly(CodecException.class, () -> {
            decoder.decode(b.toString().getBytes(), ContentFormat.JSON, new LwM2mPath(3, 0, 11), model);
        });
    }

    @Test
    public void json_cut_basename_name_in_the_middle_of_an_id() {
        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"3/0/1\",\"e\":[");
        b.append("{\"n\":\"1/0\",\"v\":0},");
        b.append("{\"n\":\"1/1\",\"v\":0}");
        b.append("]}");

        LwM2mResource resource = (LwM2mResource) decoder.decode(b.toString().getBytes(), ContentFormat.JSON,
                new LwM2mPath(3, 0, 11), model);
        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(11, resource.getId());
        assertTrue(resource.getInstances().size() == 2);
    }

    @Test
    public void senml_json_decode_acl_object() {
        StringBuilder payload = new StringBuilder();
        payload.append("[{\"bn\":\"/2/3/\",\"n\":\"0\",\"v\":3},");
        payload.append("{\"n\":\"1\",\"v\":1},");
        payload.append("{\"n\":\"3\",\"v\":123},");
        payload.append("{\"bn\":\"/2/2/\",\"n\":\"0\",\"v\":4},");
        payload.append("{\"n\":\"1\",\"v\":2},");
        payload.append("{\"n\":\"3\",\"v\":124}]");

        LwM2mObject object = decoder.decode(payload.toString().getBytes(), ContentFormat.SENML_JSON,
                new LwM2mPath("/2"), model, LwM2mObject.class);

        assertNotNull(object);
        assertEquals(2, object.getId());
        assertEquals(2, object.getInstances().size());
        assertEquals(3l, object.getInstance(3).getResource(0).getValue());
        assertEquals(1l, object.getInstance(3).getResource(1).getValue());
        assertEquals(123l, object.getInstance(3).getResource(3).getValue());
        assertEquals(4l, object.getInstance(2).getResource(0).getValue());
        assertEquals(2l, object.getInstance(2).getResource(1).getValue());
        assertEquals(124l, object.getInstance(2).getResource(3).getValue());

    }

    @Test
    public void senml_json_decode_device_object_instance() {
        StringBuilder payload = new StringBuilder();
        payload.append("[{\"bn\":\"/3/0/\",\"n\":\"0\",\"vs\":\"Open Mobile Alliance\"},");
        payload.append("{\"n\":\"1\",\"vs\":\"Lightweight M2M Client\"},");
        payload.append("{\"n\":\"2\",\"vs\":\"345000123\"},");
        payload.append("{\"n\":\"3\",\"vs\":\"1.0\"},");
        payload.append("{\"n\":\"6/0\",\"v\":1},");
        payload.append("{\"n\":\"6/1\",\"v\":5},");
        payload.append("{\"n\":\"7/0\",\"v\":3800},");
        payload.append("{\"n\":\"7/1\",\"v\":5000},");
        payload.append("{\"n\":\"8/0\",\"v\":125},");
        payload.append("{\"n\":\"8/1\",\"v\":900},");
        payload.append("{\"n\":\"9\",\"v\":100},");
        payload.append("{\"n\":\"10\",\"v\":15},");
        payload.append("{\"n\":\"11/0\",\"v\":0},");
        payload.append("{\"n\":\"13\",\"v\":1367491215},");
        payload.append("{\"n\":\"14\",\"vs\":\"+02:00\"},");
        payload.append("{\"n\":\"16\",\"vs\":\"U\"}]");

        LwM2mObjectInstance instance = decoder.decode(payload.toString().getBytes(), ContentFormat.SENML_JSON,
                new LwM2mPath("/3/0"), model, LwM2mObjectInstance.class);

        assertNotNull(instance);
        assertEquals(0, instance.getId());
        assertEquals("Open Mobile Alliance", instance.getResource(0).getValue());
        assertEquals("Lightweight M2M Client", instance.getResource(1).getValue());
        assertEquals("345000123", instance.getResource(2).getValue());
        assertEquals("1.0", instance.getResource(3).getValue());

        assertEquals(1l, instance.getResource(6).getValue(0));
        assertEquals(5l, instance.getResource(6).getValue(1));
        assertEquals(3800l, instance.getResource(7).getValue(0));
        assertEquals(5000l, instance.getResource(7).getValue(1));
        assertEquals(125l, instance.getResource(8).getValue(0));
        assertEquals(900l, instance.getResource(8).getValue(1));

        assertEquals(100l, instance.getResource(9).getValue());
        assertEquals(15l, instance.getResource(10).getValue());

        assertEquals(0l, instance.getResource(11).getValue(0));

        Date time = (Date) instance.getResource(13).getValue();
        assertEquals(1367491215000l, time.getTime());

        assertEquals("+02:00", instance.getResource(14).getValue());
        assertEquals("U", instance.getResource(16).getValue());
    }

    @Test
    public void senml_json_decode_single_resource() {
        String payload = "[{\"bn\":\"/3/0/0\",\"vs\":\"Open Mobile Alliance\"}]";
        LwM2mResource resource = decoder.decode(payload.getBytes(), ContentFormat.SENML_JSON, new LwM2mPath("/3/0/0"),
                model, LwM2mResource.class);

        assertNotNull(resource);
        assertTrue(!resource.isMultiInstances());
        assertEquals(0, resource.getId());
        assertEquals("Open Mobile Alliance", resource.getValue());

        payload = "[{\"n\":\"/6/0/3\",\"v\":20.0}]";
        resource = decoder.decode(payload.getBytes(), ContentFormat.SENML_JSON, new LwM2mPath("/6/0/3"), model,
                LwM2mResource.class);

        assertNotNull(resource);
        assertTrue(!resource.isMultiInstances());
        assertEquals(3, resource.getId());
        assertEquals(20.0, resource.getValue());
    }

    @Test
    public void senml_json_decode_single_resource_using_exponantial_notation() {
        String payload = "[{\"bn\":\"/3/0/13\",\"v\":1.638435E9}]";
        LwM2mResource resource = decoder.decode(payload.getBytes(), ContentFormat.SENML_JSON, new LwM2mPath("/3/0/13"),
                model, LwM2mResource.class);

        assertNotNull(resource);
        assertTrue(!resource.isMultiInstances());
        assertEquals(13, resource.getId());
        assertEquals(new Date(1638435000000l), resource.getValue());
    }

    @Test
    public void senml_json_decode_multiple_resource() {
        StringBuilder payload = new StringBuilder();
        payload.append("[{\"bn\":\"/3/0/7/\",\"n\":\"0\",\"v\":3800},");
        payload.append("{\"n\":\"1\",\"v\":5000}]");
        LwM2mResource multipleResources = decoder.decode(payload.toString().getBytes(), ContentFormat.SENML_JSON,
                new LwM2mPath("/3/0/7"), model, LwM2mResource.class);

        assertNotNull(multipleResources);
        assertTrue(multipleResources.isMultiInstances());
        assertEquals(3800l, multipleResources.getValue(0));
        assertEquals(5000l, multipleResources.getValue(1));
    }

    @Test
    public void senml_json_decode_ulong() {
        // 18446744073709551615 can not hold in Long
        String payload = "[{\"n\":\"/0/0/16\",\"v\":18446744073709551615}]";
        LwM2mResource resource = decoder.decode(payload.getBytes(), ContentFormat.SENML_JSON, new LwM2mPath("/0/0/16"),
                model, LwM2mResource.class);

        assertNotNull(resource);
        assertTrue(!resource.isMultiInstances());
        assertEquals(16, resource.getId());
        assertEquals(ULong.valueOf("18446744073709551615"), resource.getValue());
    }

    @Test
    public void senml_json_decode_long() {
        // 9223372036854775800 long value can not hold in double (will be approximate to 9223372036854775808)
        String payload = "[{\"n\":\"/1/0/2\",\"v\":9223372036854775800}]";
        LwM2mResource resource = decoder.decode(payload.getBytes(), ContentFormat.SENML_JSON, new LwM2mPath("/1/0/2"),
                model, LwM2mResource.class);

        assertNotNull(resource);
        assertTrue(!resource.isMultiInstances());
        assertEquals(2, resource.getId());
        assertEquals(9223372036854775800l, resource.getValue());
    }

    @Test
    public void senml_json_empty_multi_resource() {
        // see : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/494

        // empty byte array
        LwM2mResource resource = null;
        resource = (LwM2mResource) decoder.decode(new byte[0], ContentFormat.SENML_JSON, new LwM2mPath(3, 0, 6), model);
        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(6, resource.getId());
        assertTrue(resource.getInstances().isEmpty());

        // empty string
        resource = null;
        StringBuilder b = new StringBuilder();
        b.append("");
        resource = (LwM2mResource) decoder.decode(b.toString().getBytes(), ContentFormat.SENML_JSON,
                new LwM2mPath(3, 0, 6), model);
        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(6, resource.getId());
        assertTrue(resource.getInstances().isEmpty());

        // empty Array
        b = new StringBuilder();
        b.append("[]");
        resource = (LwM2mResource) decoder.decode(b.toString().getBytes(), ContentFormat.SENML_JSON,
                new LwM2mPath(3, 0, 6), model);
        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(6, resource.getId());
        assertTrue(resource.getInstances().isEmpty());
    }

    @Test
    public void senml_timestamped_resources() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/1024/0/1\",\"v\":22.9,\"bt\":268500000},");
        b.append("{\"v\":22.4,\"t\":-5},");
        b.append("{\"v\":24.1,\"t\":-50}],");

        List<TimestampedLwM2mNode> timestampedResources = decoder.decodeTimestampedData(b.toString().getBytes(),
                ContentFormat.SENML_JSON, new LwM2mPath(1024, 0, 1), model);

        assertEquals(3, timestampedResources.size());
        assertEquals(Instant.ofEpochSecond(268500000), timestampedResources.get(0).getTimestamp());
        assertEquals(22.9d, ((LwM2mResource) timestampedResources.get(0).getNode()).getValue());
        assertEquals(Instant.ofEpochSecond(268500000 - 5), timestampedResources.get(1).getTimestamp());
        assertEquals(22.4d, ((LwM2mResource) timestampedResources.get(1).getNode()).getValue());
        assertEquals(Instant.ofEpochSecond(268500000 - 50), timestampedResources.get(2).getTimestamp());
        assertEquals(24.1d, ((LwM2mResource) timestampedResources.get(2).getNode()).getValue());
    }

    @Test
    public void senml_timestamped_instances() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/1024/0/\",\"bt\":268600000, \"n\":\"1\", \"v\":22.9},");
        b.append("{\"n\":\"1\",\"v\":24.1,\"t\":-50},");
        b.append("{\"bt\":268500000, \"n\":\"0\",\"vs\":\"a string\"},");
        b.append("{\"n\":\"1\",\"v\":22.4}]");

        List<TimestampedLwM2mNode> timestampedResources = decoder.decodeTimestampedData(b.toString().getBytes(),
                ContentFormat.SENML_JSON, new LwM2mPath(1024, 0), model);

        assertEquals(3, timestampedResources.size());
        assertEquals(Instant.ofEpochSecond(268600000), timestampedResources.get(0).getTimestamp());
        assertEquals(22.9d, ((LwM2mObjectInstance) timestampedResources.get(0).getNode()).getResource(1).getValue());

        assertEquals(Instant.ofEpochSecond(268600000 - 50), timestampedResources.get(1).getTimestamp());
        assertEquals(24.1d, ((LwM2mObjectInstance) timestampedResources.get(1).getNode()).getResource(1).getValue());

        assertEquals(Instant.ofEpochSecond(268500000), timestampedResources.get(2).getTimestamp());
        assertEquals(22.4d, ((LwM2mObjectInstance) timestampedResources.get(2).getNode()).getResource(1).getValue());
        assertEquals("a string",
                ((LwM2mObjectInstance) timestampedResources.get(2).getNode()).getResource(0).getValue());

    }

    @Test
    public void senml_timestamped_Object() throws CodecException {
        // json content for instance 0 of device object
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/1024/\",\"bt\":268600000,\"n\":\"0/1\",\"v\":22.9},");
        b.append("{\"n\":\"0/0\",\"vs\":\"a string\",\"t\":-50},");
        b.append("{\"n\":\"1/1\",\"v\":23,\"t\":-50},");
        b.append("{\"n\":\"0/1\",\"v\":24.1,\"t\":-5}]");

        List<TimestampedLwM2mNode> timestampedResources = decoder.decodeTimestampedData(b.toString().getBytes(),
                ContentFormat.SENML_JSON, new LwM2mPath(1024), model);

        assertEquals(3, timestampedResources.size());
        assertEquals(Instant.ofEpochSecond(268600000), timestampedResources.get(0).getTimestamp());
        assertEquals(22.9d,
                ((LwM2mObject) timestampedResources.get(0).getNode()).getInstance(0).getResource(1).getValue());

        assertEquals(Instant.ofEpochSecond(268600000 - 5), timestampedResources.get(1).getTimestamp());
        assertEquals(24.1d,
                ((LwM2mObject) timestampedResources.get(1).getNode()).getInstance(0).getResource(1).getValue());

        assertEquals(Instant.ofEpochSecond(268600000 - 50), timestampedResources.get(2).getTimestamp());
        assertEquals(23l,
                ((LwM2mObject) timestampedResources.get(2).getNode()).getInstance(1).getResource(1).getValue());
        assertEquals("a string",
                ((LwM2mObject) timestampedResources.get(2).getNode()).getInstance(0).getResource(0).getValue());
    }

    @Test
    public void senml_json_decode_resources() {
        // Prepare data to decode
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/3/0/0\",\"vs\":\"Open Mobile Alliance\"},");
        b.append("{\"bn\":\"/3/0/9\",\"v\":95},");
        b.append("{\"bn\":\"/1/0/1\",\"v\":86400}]");
        List<LwM2mPath> paths = Arrays.asList(new LwM2mPath("3/0/0"), new LwM2mPath("3/0/9"), new LwM2mPath("1/0/1"));

        // Decode
        Map<LwM2mPath, LwM2mNode> res = decoder.decodeNodes(b.toString().getBytes(), ContentFormat.SENML_JSON, paths,
                model);

        // Expected result
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("3/0/0"), LwM2mSingleResource.newStringResource(0, "Open Mobile Alliance"));
        nodes.put(new LwM2mPath("3/0/9"), LwM2mSingleResource.newIntegerResource(9, 95));
        nodes.put(new LwM2mPath("1/0/1"), LwM2mSingleResource.newIntegerResource(1, 86400));

        assertEquals(nodes, res);
    }

    @Test
    public void senml_json_decode_mixed_resource_and_instance() {
        // Prepare data to decode
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/4/0/0\",\"v\":45},");
        b.append("{\"bn\":\"/4/0/1\",\"v\":30},");
        b.append("{\"bn\":\"/4/0/2\",\"v\":100},");
        b.append("{\"bn\":\"/6/0/\",\"n\":\"0\",\"v\":43.918998},");
        b.append("{\"n\":\"1\",\"v\":2.351149},");
        b.append("{\"n\":\"5\",\"v\":1610029880}]");

        List<LwM2mPath> paths = Arrays.asList(new LwM2mPath("4/0/0"), new LwM2mPath("4/0/1"), new LwM2mPath("4/0/2"),
                new LwM2mPath("6/0"));

        // Decode
        Map<LwM2mPath, LwM2mNode> res = decoder.decodeNodes(b.toString().getBytes(), ContentFormat.SENML_JSON, paths,
                model);

        // Expected result
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("4/0/0"), LwM2mSingleResource.newIntegerResource(0, 45));
        nodes.put(new LwM2mPath("4/0/1"), LwM2mSingleResource.newIntegerResource(1, 30));
        nodes.put(new LwM2mPath("4/0/2"), LwM2mSingleResource.newIntegerResource(2, 100));
        nodes.put(new LwM2mPath("6/0"),
                new LwM2mObjectInstance(0, LwM2mSingleResource.newFloatResource(0, 43.918998),
                        LwM2mSingleResource.newFloatResource(1, 2.351149),
                        LwM2mSingleResource.newDateResource(5, new Date(1610029880000l))));

        assertEquals(nodes, res);
    }

    @Test
    public void senml_json_decode_mixed_resource_without_given_path() {
        // Prepare data to decode
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/4/0/0\",\"v\":45},");
        b.append("{\"bn\":\"/4/0/1\",\"v\":30},");
        b.append("{\"bn\":\"/4/0/2\",\"v\":100},");
        b.append("{\"bn\":\"/6/0/\",\"n\":\"0\",\"v\":43.918998},");
        b.append("{\"n\":\"1\",\"v\":2.351149},");
        b.append("{\"n\":\"5\",\"v\":1610029880}]");

        // Decode
        Map<LwM2mPath, LwM2mNode> res = decoder.decodeNodes(b.toString().getBytes(), ContentFormat.SENML_JSON, null,
                model);

        // Expected result
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("4/0/0"), LwM2mSingleResource.newIntegerResource(0, 45));
        nodes.put(new LwM2mPath("4/0/1"), LwM2mSingleResource.newIntegerResource(1, 30));
        nodes.put(new LwM2mPath("4/0/2"), LwM2mSingleResource.newIntegerResource(2, 100));
        nodes.put(new LwM2mPath("6/0/0"), LwM2mSingleResource.newFloatResource(0, 43.918998));
        nodes.put(new LwM2mPath("6/0/1"), LwM2mSingleResource.newFloatResource(1, 2.351149));
        nodes.put(new LwM2mPath("6/0/5"), LwM2mSingleResource.newDateResource(5, new Date(1610029880000l)));

        assertEquals(nodes, res);
    }

    @Test
    public void senml_json_decode_path_using_name() {
        // Prepare data to decode
        StringBuilder b = new StringBuilder();
        b.append("[{\"n\":\"/4/0/0\"},");
        b.append("{\"n\":\"/4/0/1\"},");
        b.append("{\"n\":\"/4/0/2\"}]");

        // Decode
        List<LwM2mPath> res = decoder.decodePaths(b.toString().getBytes(), ContentFormat.SENML_JSON);

        // Expected result
        List<LwM2mPath> paths = Arrays.asList( //
                new LwM2mPath("4/0/0"), //
                new LwM2mPath("4/0/1"), //
                new LwM2mPath("4/0/2"));

        assertEquals(paths, res);
    }

    @Test
    public void senml_json_decode_path_using_base_name() {
        // Prepare data to decode
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/4/0/\", \"n\":\"0\"},");
        b.append("{\"n\":\"1\"},");
        b.append("{\"n\":\"2\"}]");

        // Decode
        List<LwM2mPath> res = decoder.decodePaths(b.toString().getBytes(), ContentFormat.SENML_JSON);

        // Expected result
        List<LwM2mPath> paths = Arrays.asList( //
                new LwM2mPath("4/0/0"), //
                new LwM2mPath("4/0/1"), //
                new LwM2mPath("4/0/2"));

        assertEquals(paths, res);
    }

    @Test
    public void senml_json_decode_invalid_path_with_value() {
        // Prepare data to decode
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/4/0/\", \"n\":\"0\"},");
        b.append("{\"n\":\"1\", \"v\":200},");
        b.append("{\"n\":\"2\"}]");

        // Decode
        assertThrowsExactly(CodecException.class, () -> {
            decoder.decodePaths(b.toString().getBytes(), ContentFormat.SENML_JSON);
        });
    }

    @Test
    public void senml_json_decode_invalid_path_with_timestamp() {
        // Prepare data to decode
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/4/0/\", \"n\":\"0\"},");
        b.append("{\"n\":\"1\", \"t\":20000000},");
        b.append("{\"n\":\"2\"}]");

        // Decode
        assertThrowsExactly(CodecException.class, () -> {
            decoder.decodePaths(b.toString().getBytes(), ContentFormat.SENML_JSON);
        });
    }

    @Test
    public void senml_json_decode_opaque_resource() {
        byte[] json = "[{\"bn\":\"/0/0/3\",\"vd\":\"q83v\"}]".getBytes(); // q83v is base64 of ABCDE
        LwM2mResource oResource = (LwM2mResource) decoder.decode(json, ContentFormat.SENML_JSON,
                new LwM2mPath("/0/0/3"), model);

        byte[] bytes = Hex.decodeHex("ABCDEF".toCharArray());
        LwM2mResource expected = LwM2mSingleResource.newBinaryResource(3, bytes);

        assertEquals(expected, oResource);
    }

    @Test
    public void senml_cbor_decode_opaque_resource() {
        // value : [{-2: "/0/0/3", 8: h'ABCDEF'}]
        byte[] cbor = Hex.decodeHex("81a221662f302f302f330843abcdef".toCharArray());
        LwM2mResource oResource = (LwM2mResource) decoder.decode(cbor, ContentFormat.SENML_CBOR,
                new LwM2mPath("/0/0/3"), model);

        byte[] bytes = Hex.decodeHex("ABCDEF".toCharArray());
        LwM2mResource expected = LwM2mSingleResource.newBinaryResource(3, bytes);

        assertEquals(expected, oResource);
    }

    @Test
    public void senml_cbor_empty_multi_resource() {
        // see : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/494

        // empty byte array
        LwM2mResource resource = null;
        resource = (LwM2mResource) decoder.decode(new byte[0], ContentFormat.SENML_CBOR, new LwM2mPath(3, 0, 6), model);
        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(6, resource.getId());
        assertTrue(resource.getInstances().isEmpty());

        // empty Array
        // value : []
        byte[] cbor = Hex.decodeHex("80".toCharArray());
        resource = (LwM2mResource) decoder.decode(cbor, ContentFormat.SENML_CBOR, new LwM2mPath(3, 0, 6), model);
        assertNotNull(resource);
        assertTrue(resource instanceof LwM2mMultipleResource);
        assertEquals(6, resource.getId());
        assertTrue(resource.getInstances().isEmpty());
    }

    @Test
    public void senml_multiple_timestamped_nodes() throws CodecException {
        // given
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/4/0/\",\"bt\":268600000,\"n\":\"0\",\"v\":1,\"t\":1},");
        b.append("{\"n\":\"1\",\"v\":2,\"t\":2},");
        b.append("{\"n\":\"1\",\"v\":3,\"t\":3},");
        b.append("{\"bn\":\"/3/0/7/\",\"n\":\"0\",\"v\":3800}");
        b.append("]");

        // when
        TimestampedLwM2mNodes data = decoder.decodeTimestampedNodes(b.toString().getBytes(), ContentFormat.SENML_JSON,
                model);

        // then
        Instant timestamp = Instant.ofEpochSecond(268600000);
        TimestampedLwM2mNodes.Builder expectedResult = new TimestampedLwM2mNodes.Builder()
                .put(timestamp, new LwM2mPath("/3/0/7/0"), LwM2mResourceInstance.newIntegerInstance(0, 3800))
                .put(timestamp.plusSeconds(1), new LwM2mPath("/4/0/0"), LwM2mSingleResource.newIntegerResource(0, 1))
                .put(timestamp.plusSeconds(2), new LwM2mPath("/4/0/1"), LwM2mSingleResource.newIntegerResource(1, 2))
                .put(timestamp.plusSeconds(3), new LwM2mPath("/4/0/1"), LwM2mSingleResource.newIntegerResource(1, 3));

        assertEquals(expectedResult.build(), data);
    }

}

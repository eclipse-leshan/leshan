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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

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
import org.eclipse.leshan.core.node.Value.DataType;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.util.Charsets;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link LwM2mNodeDecoder}
 */
public class LwM2mNodeDecoderTest {

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
    public void text_manufacturer_resource() throws InvalidValueException {
        String value = "MyManufacturer";
        LwM2mResource resource = (LwM2mResource) LwM2mNodeDecoder.decode(value.getBytes(Charsets.UTF_8),
                ContentFormat.TEXT, new LwM2mPath(3, 0, 0), model);

        assertEquals(0, resource.getId());
        assertFalse(resource.isMultiInstances());
        assertEquals(DataType.STRING, resource.getValue().type);
        assertEquals(value, resource.getValue().value);
    }

    @Test
    public void no_model_and_no_content_type_then_fallback_to_text() throws InvalidValueException {
        String value = "MyManufacturer";
        LwM2mResource resource = (LwM2mResource) LwM2mNodeDecoder.decode(value.getBytes(Charsets.UTF_8), null,
                new LwM2mPath(666, 0, 0), model);

        assertEquals(0, resource.getId());
        assertFalse(resource.isMultiInstances());
        assertEquals(DataType.STRING, resource.getValue().type);
        assertEquals(value, resource.getValue().value);
    }

    @Test
    public void text_battery_resource() throws InvalidValueException {
        LwM2mResource resource = (LwM2mResource) LwM2mNodeDecoder.decode("100".getBytes(Charsets.UTF_8),
                ContentFormat.TEXT, new LwM2mPath(3, 0, 9), model);

        assertEquals(9, resource.getId());
        assertFalse(resource.isMultiInstances());
        assertEquals(DataType.INTEGER, resource.getValue().type);
        assertEquals(100, ((Number) resource.getValue().value).intValue());
    }

    @Test
    public void tlv_manufacturer_resource() throws InvalidValueException {
        String value = "MyManufacturer";
        byte[] content = TlvEncoder.encode(new Tlv[] { new Tlv(TlvType.RESOURCE_VALUE, null, value.getBytes(), 0) })
                .array();
        LwM2mResource resource = (LwM2mResource) LwM2mNodeDecoder.decode(content, ContentFormat.TLV, new LwM2mPath(3,
                0, 0), model);

        assertEquals(0, resource.getId());
        assertFalse(resource.isMultiInstances());
        assertEquals(value, resource.getValue().value);
    }

    // tlv content for instance 0 of device object
    private final static byte[] DEVICE_CONTENT = new byte[] { -56, 0, 20, 79, 112, 101, 110, 32, 77, 111, 98, 105, 108,
                            101, 32, 65, 108, 108, 105, 97, 110, 99, 101, -56, 1, 22, 76, 105, 103, 104, 116, 119, 101,
                            105, 103, 104, 116, 32, 77, 50, 77, 32, 67, 108, 105, 101, 110, 116, -56, 2, 9, 51, 52, 53,
                            48, 48, 48, 49, 50, 51, -61, 3, 49, 46, 48, -122, 6, 65, 0, 1, 65, 1, 5, -120, 7, 8, 66, 0,
                            14, -40, 66, 1, 19, -120, -121, 8, 65, 0, 125, 66, 1, 3, -124, -63, 9, 100, -63, 10, 15,
                            -63, 11, 0, -60, 13, 81, -126, 66, -113, -58, 14, 43, 48, 50, 58, 48, 48, -63, 15, 85 };

    @Test
    public void tlv_device_object_mono_instance() throws Exception {
        LwM2mObjectInstance oInstance = ((LwM2mObject) LwM2mNodeDecoder.decode(DEVICE_CONTENT, ContentFormat.TLV,
                new LwM2mPath(3), model)).getInstances().get(0);
        assertDeviceInstance(oInstance);
    }

    private void assertDeviceInstance(LwM2mObjectInstance oInstance) {
        assertEquals(0, oInstance.getId());

        assertEquals("Open Mobile Alliance", (String) oInstance.getResources().get(0).getValue().value);
        assertEquals("Lightweight M2M Client", (String) oInstance.getResources().get(1).getValue().value);
        assertEquals("345000123", (String) oInstance.getResources().get(2).getValue().value);
        assertEquals("1.0", (String) oInstance.getResources().get(3).getValue().value);
        assertNull(oInstance.getResources().get(4));
        assertNull(oInstance.getResources().get(5));
        assertEquals(2, oInstance.getResources().get(6).getValues().length);
        assertEquals(1, ((Number) oInstance.getResources().get(6).getValues()[0].value).intValue());
        assertEquals(5, ((Number) oInstance.getResources().get(6).getValues()[1].value).intValue());
        assertEquals(3800, ((Number) oInstance.getResources().get(7).getValues()[0].value).intValue());
        assertEquals(5000, ((Number) oInstance.getResources().get(7).getValues()[1].value).intValue());
        assertEquals(125, ((Number) oInstance.getResources().get(8).getValues()[0].value).intValue());
        assertEquals((int) 900, oInstance.getResources().get(8).getValues()[1].value);
        assertEquals(100, ((Number) oInstance.getResources().get(9).getValue().value).intValue());
        assertEquals(15, ((Number) oInstance.getResources().get(10).getValue().value).intValue());
        assertEquals(0, ((Number) oInstance.getResources().get(11).getValue().value).intValue());
        assertNull(oInstance.getResources().get(12));
        assertEquals(new Date(1367491215000L), (Date) oInstance.getResources().get(13).getValue().value);
        assertEquals("+02:00", (String) oInstance.getResources().get(14).getValue().value);
        assertEquals("U", (String) oInstance.getResources().get(15).getValue().value);

    }

    @Test
    public void tlv_device_object_instance0() throws InvalidValueException {

        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) LwM2mNodeDecoder.decode(DEVICE_CONTENT,
                ContentFormat.TLV, new LwM2mPath(3, 0), model);
        assertDeviceInstance(oInstance);
    }

    @Test
    public void tlv_power_source__array_values() throws InvalidValueException {
        byte[] content = new byte[] { 65, 0, 1, 65, 1, 5 };

        LwM2mResource resource = (LwM2mResource) LwM2mNodeDecoder.decode(content, ContentFormat.TLV, new LwM2mPath(3,
                0, 6), model);

        assertEquals(6, resource.getId());
        assertEquals(2, resource.getValues().length);
        assertEquals(1, ((Number) resource.getValues()[0].value).intValue());
        assertEquals(5, ((Number) resource.getValues()[1].value).intValue());
    }

    @Test
    public void tlv_power_source__multiple_resource() throws InvalidValueException {
        // this content (a single TLV of type 'multiple_resource' containing the values)
        // is probably not compliant with the spec but it should be supported by the server
        byte[] content = new byte[] { -122, 6, 65, 0, 1, 65, 1, 5 };

        LwM2mResource resource = (LwM2mResource) LwM2mNodeDecoder.decode(content, ContentFormat.TLV, new LwM2mPath(3,
                0, 6), model);

        assertEquals(6, resource.getId());
        assertEquals(2, resource.getValues().length);
        assertEquals(1, ((Number) resource.getValues()[0].value).intValue());
        assertEquals(5, ((Number) resource.getValues()[1].value).intValue());
    }

    @Test
    public void json_device_object_instance0() throws InvalidValueException {
        // json content for instance 0 of device object
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

        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) LwM2mNodeDecoder.decode(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(3, 0), model);

        assertEquals(0, oInstance.getId());

        assertEquals("Open Mobile Alliance", (String) oInstance.getResources().get(0).getValue().value);
        assertEquals("Lightweight M2M Client", (String) oInstance.getResources().get(1).getValue().value);
        assertEquals("345000123", (String) oInstance.getResources().get(2).getValue().value);
        assertEquals("1.0", (String) oInstance.getResources().get(3).getValue().value);
        assertNull(oInstance.getResources().get(4));
        assertNull(oInstance.getResources().get(5));
        assertEquals(2, oInstance.getResources().get(6).getValues().length);
        assertEquals(1, ((Number) oInstance.getResources().get(6).getValues()[0].value).intValue());
        assertEquals(5, ((Number) oInstance.getResources().get(6).getValues()[1].value).intValue());
        assertEquals(3800, ((Number) oInstance.getResources().get(7).getValues()[0].value).intValue());
        assertEquals(5000, ((Number) oInstance.getResources().get(7).getValues()[1].value).intValue());
        assertEquals(125, ((Number) oInstance.getResources().get(8).getValues()[0].value).intValue());
        assertEquals((int) 900, oInstance.getResources().get(8).getValues()[1].value);
        assertEquals(100, ((Number) oInstance.getResources().get(9).getValue().value).intValue());
        assertEquals(15, ((Number) oInstance.getResources().get(10).getValue().value).intValue());
        assertEquals(0, ((Number) oInstance.getResources().get(11).getValue().value).intValue());
        assertNull(oInstance.getResources().get(12));
        assertEquals(new Date(1367491215000L), (Date) oInstance.getResources().get(13).getValue().value);
        assertEquals("+02:00", (String) oInstance.getResources().get(14).getValue().value);
        assertEquals("U", (String) oInstance.getResources().get(15).getValue().value);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void json_device_object_instance0_with_root_basename() throws InvalidValueException {
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
        b.append("{\"n\":\"3/0/15\",\"sv\":\"U\"}]}");

        LwM2mObjectInstance oInstance = (LwM2mObjectInstance) LwM2mNodeDecoder.decode(b.toString().getBytes(),
                ContentFormat.JSON, new LwM2mPath(3, 0), model);

        assertEquals(0, oInstance.getId());

        assertEquals("Open Mobile Alliance", (String) oInstance.getResources().get(0).getValue().value);
        assertEquals("Lightweight M2M Client", (String) oInstance.getResources().get(1).getValue().value);
        assertEquals("345000123", (String) oInstance.getResources().get(2).getValue().value);
        assertEquals("1.0", (String) oInstance.getResources().get(3).getValue().value);
        assertNull(oInstance.getResources().get(4));
        assertNull(oInstance.getResources().get(5));
        assertEquals(2, oInstance.getResources().get(6).getValues().length);
        assertEquals(1, ((Number) oInstance.getResources().get(6).getValues()[0].value).intValue());
        assertEquals(5, ((Number) oInstance.getResources().get(6).getValues()[1].value).intValue());
        assertEquals(3800, ((Number) oInstance.getResources().get(7).getValues()[0].value).intValue());
        assertEquals(5000, ((Number) oInstance.getResources().get(7).getValues()[1].value).intValue());
        assertEquals(125, ((Number) oInstance.getResources().get(8).getValues()[0].value).intValue());
        assertEquals((int) 900, oInstance.getResources().get(8).getValues()[1].value);
        assertEquals(100, ((Number) oInstance.getResources().get(9).getValue().value).intValue());
        assertEquals(15, ((Number) oInstance.getResources().get(10).getValue().value).intValue());
        assertEquals(0, ((Number) oInstance.getResources().get(11).getValue().value).intValue());
        assertNull(oInstance.getResources().get(12));
        assertEquals(new Date(1367491215000L), (Date) oInstance.getResources().get(13).getValue().value);
        assertEquals("+02:00", (String) oInstance.getResources().get(14).getValue().value);
        assertEquals("U", (String) oInstance.getResources().get(15).getValue().value);
    }
}

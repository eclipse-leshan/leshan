/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.core.senml.cbor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.senml.SenMLDecoder;
import org.eclipse.leshan.senml.SenMLEncoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.eclipse.leshan.senml.cbor.upokecenter.SenMLCborUpokecenterEncoderDecoder;
import org.junit.jupiter.api.Test;

public class SenMLCborSerializerTest extends AbstractSenMLTest {

    private final SenMLEncoder encoder;
    private final SenMLDecoder decoder;

    public SenMLCborSerializerTest() {
        SenMLCborUpokecenterEncoderDecoder cborJava = new SenMLCborUpokecenterEncoderDecoder(true, false);
        this.encoder = cborJava;
        this.decoder = cborJava;
    }

    @Test
    public void serialize_then_deserialize_device_object() throws SenMLException {
        byte[] cbor = encoder.toSenML(givenDeviceObjectInstance());
        SenMLPack pack = decoder.fromSenML(cbor);
        SenMLTestUtil.assertSenMLPackEquals(givenDeviceObjectInstance(), pack);
    }

    @Test
    public void serialize_device_object() throws Exception {
        byte[] cbor = encoder.toSenML(givenDeviceObjectInstance());
        assertEquals(givenSenMLCborExample(), Hex.encodeHexString(cbor));
    }

    @Test
    public void deserialize_device_object_() throws Exception {
        SenMLPack pack = decoder.fromSenML(Hex.decodeHex(givenSenMLCborExample().toCharArray()));
        SenMLTestUtil.assertSenMLPackEquals(givenDeviceObjectInstance(), pack);
    }

    @Test
    public void deserialize_opaque_resource() throws Exception {
        // value : [{-2: "/0/0/3", 8: h'ABCDEF'}]
        byte[] cbor = Hex.decodeHex("81a221662f302f302f330843abcdef".toCharArray());
        SenMLPack pack = decoder.fromSenML(cbor);

        SenMLTestUtil.assertSenMLPackEquals(
                getPackWithSingleOpaqueValue("/0/0/3", Hex.decodeHex("ABCDEF".toCharArray())), pack);
    }

    @Test
    public void serialize_opaque_resource_() throws Exception {
        SenMLPack pack = getPackWithSingleOpaqueValue("/0/0/3", Hex.decodeHex("ABCDEF".toCharArray()));
        byte[] cbor = encoder.toSenML(pack);

        // value : [{-2: "/0/0/3", 8: h'ABCDEF'}]
        String expected = "81a221662f302f302f330843abcdef";
        assertEquals(expected, Hex.encodeHexString(cbor));
    }

    @Test
    public void deserialize_float_resource() throws Exception {
        // value : [{2: 300.0, -2: "/3442/0/130"}]
        byte[] cbor = Hex.decodeHex("81a202f95cb0216b2f333434322f302f313330".toCharArray());
        SenMLPack pack = decoder.fromSenML(cbor);

        assertEquals(pack.getRecords().size(), 1);
        SenMLRecord record = pack.getRecords().get(0);
        assertEquals("/3442/0/130", record.getBaseName());
        assertEquals(300.0d, record.getNumberValue());
    }
}

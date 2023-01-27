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
import org.eclipse.leshan.senml.json.jackson.SenMLJsonJacksonEncoderDecoder;
import org.junit.jupiter.api.Test;

public class SenMLJsonSerDesTest extends AbstractSenMLTest {

    private final SenMLEncoder encoder;
    private final SenMLDecoder decoder;

    public SenMLJsonSerDesTest() {
        SenMLJsonJacksonEncoderDecoder jackson = new SenMLJsonJacksonEncoderDecoder();
        this.encoder = jackson;
        this.decoder = jackson;
    }

    @Test
    public void serialize_then_deserialize_device_object() throws SenMLException {
        byte[] json = encoder.toSenML(givenDeviceObjectInstance());
        SenMLPack pack = decoder.fromSenML(json);
        SenMLTestUtil.assertSenMLPackEquals(givenDeviceObjectInstance(), pack);
    }

    @Test
    public void deserialize_device_object() throws SenMLException {
        String dataString = givenSenMLJsonExample();
        SenMLPack pack = decoder.fromSenML(dataString.getBytes());

        SenMLTestUtil.assertSenMLPackEquals(givenDeviceObjectInstance(), pack);
    }

    @Test
    public void serialize_device_object() throws SenMLException {
        SenMLPack pack = givenDeviceObjectInstance();
        byte[] json = encoder.toSenML(pack);
        assertEquals(givenSenMLJsonExample(), new String(json));
    }

    @Test
    public void deserialize_opaque_object_() throws Exception {
        byte[] json = "[{\"bn\":\"/0/0/3\",\"vd\":\"q83v\"}]".getBytes(); // q83v is base64 of ABCDE
        SenMLPack pack = decoder.fromSenML(json);

        SenMLTestUtil.assertSenMLPackEquals(
                getPackWithSingleOpaqueValue("/0/0/3", Hex.decodeHex("ABCDEF".toCharArray())), pack);
    }

    @Test
    public void serialize_opaque_object_() throws Exception {
        SenMLPack pack = getPackWithSingleOpaqueValue("/0/0/3", Hex.decodeHex("ABCDEF".toCharArray()));
        byte[] json = encoder.toSenML(pack);

        String expected = "[{\"bn\":\"/0/0/3\",\"vd\":\"q83v\"}]"; // q83v is base64 of ABCDE
        assertEquals(expected, new String(json));
    }

}

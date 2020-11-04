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

package org.eclipse.leshan.senml.cbor.jackson;

import java.io.IOException;

import org.eclipse.leshan.senml.SenMLDecoder;
import org.eclipse.leshan.senml.SenMLEncoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

/**
 * Helper for encoding/decoding SenML CBOR using Jackson
 */
public class SenMLCborJacksonEncoderDecoder implements SenMLDecoder, SenMLEncoder {
    private static final SenMLCborPackSerDes serDes = new SenMLCborPackSerDes();
    private static final ObjectMapper mapper = new CBORMapper();

    @Override
    public byte[] toSenML(SenMLPack pack) throws SenMLException {
        if (pack == null)
            return null;
        return serDes.serializeToCbor(pack);
    }

    @Override
    public SenMLPack fromSenML(byte[] jsonString) throws SenMLException {
        try {
            JsonNode node = mapper.readTree(jsonString);
            if (!node.isArray()) {
                throw new SenMLException("Unable to parse SenML CBOR: Array expected but was %s", node.getNodeType());
            }
            return serDes.deserializeFromCbor(node.iterator());
        } catch (IOException e) {
            throw new SenMLException("Unable to parse SenML CBOR.", e);
        }
    }
}

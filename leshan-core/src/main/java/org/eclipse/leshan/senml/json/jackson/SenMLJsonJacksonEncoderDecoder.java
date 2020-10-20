/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml.json.jackson;

import java.io.IOException;

import org.eclipse.leshan.core.json.LwM2mJsonException;
import org.eclipse.leshan.core.util.json.JsonException;
import org.eclipse.leshan.senml.SenMLPack;

import com.eclipsesource.json.ParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper for encoding/decoding SenML JSON using Jackson
 */
public class SenMLJsonJacksonEncoderDecoder {
    private static final SenMLJsonRecordSerDes serDes = new SenMLJsonRecordSerDes();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toSenMLJson(SenMLPack pack) throws LwM2mJsonException {
        if (pack == null)
            return null;
        try {
            return serDes.sSerialize(pack.getRecords());
        } catch (JsonException e) {
            throw new LwM2mJsonException("Unable to serialize SenML JSON.", e);
        }
    }

    public static SenMLPack fromSenMLJson(String jsonString) throws LwM2mJsonException {
        try {
            JsonNode node = mapper.readTree(jsonString);
            if (!node.isArray()) {
                throw new LwM2mJsonException("Unable to parse SenML JSON: JsonArray expected but was %s",
                        node.getNodeType());
            }
            return new SenMLPack(serDes.deserialize(node.iterator()));
        } catch (JsonException | ParseException | IOException e) {
            throw new LwM2mJsonException("Unable to parse SenML JSON.", e);
        }
    }
}

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

package org.eclipse.leshan.senml.json.jackson;

import java.io.IOException;

import org.eclipse.leshan.core.util.base64.Base64Decoder;
import org.eclipse.leshan.core.util.base64.Base64Encoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder.DecoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder.DecoderPadding;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderPadding;
import org.eclipse.leshan.core.util.json.JacksonJsonSerDes;
import org.eclipse.leshan.core.util.json.JsonException;
import org.eclipse.leshan.senml.SenMLDecoder;
import org.eclipse.leshan.senml.SenMLEncoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper for encoding/decoding SenML JSON using Jackson
 */
public class SenMLJsonJacksonEncoderDecoder implements SenMLDecoder, SenMLEncoder {
    private final JacksonJsonSerDes<SenMLRecord> serDes;
    private static final ObjectMapper mapper = new ObjectMapper();

    public SenMLJsonJacksonEncoderDecoder() {
        this(false);
    }

    /**
     * Create an Encoder/Decoder for SenML-JSON based on Jackson.
     *
     * SenML value is defined as mandatory in <a href="https://tools.ietf.org/html/rfc8428#section-4.2">rfc8428</a>, but
     * SenML records used with a Read-Composite operation do not contain any value field, so
     * <code>allowNoValue=true</code> can be used skip this validation.
     *
     * @param allowNoValue <code>True</code> to not check if there is a value for each SenML record.
     */
    public SenMLJsonJacksonEncoderDecoder(boolean allowNoValue) {
        this(allowNoValue, new DefaultBase64Decoder(DecoderAlphabet.BASE64URL, DecoderPadding.FORBIDEN),
                new DefaultBase64Encoder(EncoderAlphabet.BASE64URL, EncoderPadding.WITHOUT));
    }

    public SenMLJsonJacksonEncoderDecoder(boolean allowNoValue, Base64Decoder base64Decoder,
            Base64Encoder base64Encoder) {
        this(new SenMLJsonRecordSerDes(allowNoValue, base64Decoder, base64Encoder));
    }

    public SenMLJsonJacksonEncoderDecoder(JacksonJsonSerDes<SenMLRecord> senMLJSONSerializerDeserializer) {
        this.serDes = senMLJSONSerializerDeserializer;
    }

    @Override
    public byte[] toSenML(SenMLPack pack) throws SenMLException {
        if (pack == null)
            return null;
        try {
            return serDes.bSerialize(pack.getRecords());
        } catch (JsonException e) {
            throw new SenMLException("Unable to serialize SenML JSON.", e);
        }
    }

    @Override
    public SenMLPack fromSenML(byte[] jsonString) throws SenMLException {
        try {
            // handle empty payload
            if (jsonString == null || jsonString.length == 0) {
                return new SenMLPack();
            }

            JsonNode node = mapper.readTree(jsonString);
            if (!node.isArray()) {
                throw new SenMLException("Unable to parse SenML JSON: JsonArray expected but was %s",
                        node.getNodeType());
            }
            return new SenMLPack(serDes.deserialize(node.iterator()));

        } catch (JsonException | IOException e) {
            throw new SenMLException("Unable to parse SenML JSON.", e);
        }
    }
}

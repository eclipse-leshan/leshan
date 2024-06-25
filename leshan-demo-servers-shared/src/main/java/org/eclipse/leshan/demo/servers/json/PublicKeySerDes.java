/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.core.demo.json;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.base64.Base64Encoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderPadding;
import org.eclipse.leshan.core.util.json.JacksonJsonSerDes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PublicKeySerDes extends JacksonJsonSerDes<PublicKey> {

    private final Base64Encoder base64Encoder = new DefaultBase64Encoder(EncoderAlphabet.BASE64, EncoderPadding.WITH);

    @Override
    public JsonNode jSerialize(PublicKey publicKey) {
        if (!(publicKey instanceof ECPublicKey))
            throw new IllegalStateException("Unsupported Public Key Format (only ECPublicKey supported).");

        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        ObjectNode o = JsonNodeFactory.instance.objectNode();

        // Get x coordinate
        byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
        if (x[0] == 0)
            x = Arrays.copyOfRange(x, 1, x.length);
        o.put("x", Hex.encodeHexString(x));

        // Get Y coordinate
        byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
        if (y[0] == 0)
            y = Arrays.copyOfRange(y, 1, y.length);
        o.put("y", Hex.encodeHexString(y));

        // Get Curves params
        o.put("params", ecPublicKey.getParams().toString());

        // Get raw public key in format SubjectPublicKeyInfo (DER encoding)
        o.put("b64Der", base64Encoder.encode(ecPublicKey.getEncoded()));

        return o;
    }

    @Override
    public PublicKey deserialize(JsonNode o) {
        throw new UnsupportedOperationException("not implemented yet.");
    }
}

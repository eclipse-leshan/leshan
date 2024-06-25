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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.eclipse.leshan.core.util.base64.Base64Encoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderPadding;
import org.eclipse.leshan.core.util.json.JacksonJsonSerDes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

///!\ This class is a COPY of org.eclipse.leshan.server.bootstrap.demo.json.X509CertificateSerDes /!\
// TODO create a leshan-demo project ?
public class X509CertificateSerDes extends JacksonJsonSerDes<X509Certificate> {

    private final PublicKeySerDes publicKeySerDes = new PublicKeySerDes();
    private final Base64Encoder base64Encoder = new DefaultBase64Encoder(EncoderAlphabet.BASE64, EncoderPadding.WITH);

    @Override
    public JsonNode jSerialize(X509Certificate certificate) {
        final ObjectNode o = JsonNodeFactory.instance.objectNode();
        // add pubkey info
        o.set("pubkey", publicKeySerDes.jSerialize(certificate.getPublicKey()));

        // Get certificate (DER encoding)
        try {
            o.put("b64Der", base64Encoder.encode(certificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }

        return o;
    }

    @Override
    public X509Certificate deserialize(JsonNode o) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}

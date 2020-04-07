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
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet.json;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.eclipse.leshan.core.util.Base64;
import org.eclipse.leshan.core.util.json.JsonSerDes;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

///!\ This class is a COPY of org.eclipse.leshan.server.bootstrap.demo.json.X509CertificateSerDes /!\
// TODO create a leshan-demo project ?
public class X509CertificateSerDes extends JsonSerDes<X509Certificate> {

    private PublicKeySerDes publicKeySerDes = new PublicKeySerDes();

    @Override
    public JsonObject jSerialize(X509Certificate certificate) {
        final JsonObject o = Json.object();
        // add pubkey info
        o.add("pubkey", publicKeySerDes.jSerialize(certificate.getPublicKey()));

        // Get certificate (DER encoding)
        try {
            o.add("b64Der", Base64.encodeBase64String(certificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }

        return o;
    }

    @Override
    public X509Certificate deserialize(JsonObject o) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}

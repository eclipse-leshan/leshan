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

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import org.eclipse.leshan.core.util.Base64;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.json.JsonSerDes;

import com.eclipsesource.json.JsonObject;

///!\ This class is a COPY of org.eclipse.leshan.server.bootstrap.demo.json.PublicKeySerDes /!\
//TODO create a leshan-demo project ?
public class PublicKeySerDes extends JsonSerDes<PublicKey> {

    @Override
    public JsonObject jSerialize(PublicKey publicKey) {
        if (!(publicKey instanceof ECPublicKey))
            throw new IllegalStateException("Unsupported Public Key Format (only ECPublicKey supported).");

        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        JsonObject o = new JsonObject();

        // Get x coordinate
        byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
        if (x[0] == 0)
            x = Arrays.copyOfRange(x, 1, x.length);
        o.add("x", Hex.encodeHexString(x));

        // Get Y coordinate
        byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
        if (y[0] == 0)
            y = Arrays.copyOfRange(y, 1, y.length);
        o.add("y", Hex.encodeHexString(y));

        // Get Curves params
        o.add("params", ecPublicKey.getParams().toString());

        // Get raw public key in format SubjectPublicKeyInfo (DER encoding)
        o.add("b64Der", Base64.encodeBase64String(ecPublicKey.getEncoded()));

        return o;
    }

    @Override
    public PublicKey deserialize(JsonObject o) {
        throw new UnsupportedOperationException("not implemented yet.");
    }
}

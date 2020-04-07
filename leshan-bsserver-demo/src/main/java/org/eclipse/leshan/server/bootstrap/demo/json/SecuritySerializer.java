/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap.demo.json;

import java.lang.reflect.Type;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import org.eclipse.leshan.core.util.Base64;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

///!\ This class is a COPY of org.eclipse.leshan.server.demo.servlet.json.SecuritySerializer /!\
public class SecuritySerializer implements JsonSerializer<SecurityInfo> {

    @Override
    public JsonElement serialize(SecurityInfo src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("endpoint", src.getEndpoint());

        if (src.getIdentity() != null) {
            JsonObject psk = new JsonObject();
            psk.addProperty("identity", src.getIdentity());
            psk.addProperty("key", Hex.encodeHexString(src.getPreSharedKey()));
            element.add("psk", psk);
        }

        if (src.getRawPublicKey() != null) {
            JsonObject rpk = new JsonObject();
            PublicKey rawPublicKey = src.getRawPublicKey();
            if (rawPublicKey instanceof ECPublicKey) {
                ECPublicKey ecPublicKey = (ECPublicKey) rawPublicKey;
                // Get x coordinate
                byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
                if (x[0] == 0)
                    x = Arrays.copyOfRange(x, 1, x.length);
                rpk.addProperty("x", Hex.encodeHexString(x));

                // Get Y coordinate
                byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
                if (y[0] == 0)
                    y = Arrays.copyOfRange(y, 1, y.length);
                rpk.addProperty("y", Hex.encodeHexString(y));

                // Get Curves params
                rpk.addProperty("params", ecPublicKey.getParams().toString());

                // Get raw public key in format PKCS8 (DER encoding)
                rpk.addProperty("pkcs8", Base64.encodeBase64String(ecPublicKey.getEncoded()));
            } else {
                throw new JsonParseException("Unsupported Public Key Format (only ECPublicKey supported).");
            }
            element.add("rpk", rpk);
        }

        if (src.useX509Cert()) {
            element.addProperty("x509", true);
        }

        return element;
    }
}

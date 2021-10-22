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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.core.demo.json;

import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.util.Base64;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JacksonSecuritySerializer extends StdSerializer<SecurityInfo> {

    private static final long serialVersionUID = 1L;

    protected JacksonSecuritySerializer(Class<SecurityInfo> t) {
        super(t);
    }

    public JacksonSecuritySerializer() {
        this(null);
    }

    @Override
    public void serialize(SecurityInfo src, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Map<String, Object> element = new HashMap<>();

        element.put("endpoint", src.getEndpoint());

        if (src.getIdentity() != null) {
            Map<String, Object> psk = new HashMap<>();
            psk.put("identity", src.getIdentity());
            psk.put("key", Hex.encodeHexString(src.getPreSharedKey()));
            element.put("psk", psk);
        }

        if (src.getRawPublicKey() != null) {
            Map<String, Object> rpk = new HashMap<>();
            PublicKey rawPublicKey = src.getRawPublicKey();
            if (rawPublicKey instanceof ECPublicKey) {
                rpk.put("key", Hex.encodeHexString(rawPublicKey.getEncoded()));

                // TODO all the fields above is no more used should be removed it ?
                ECPublicKey ecPublicKey = (ECPublicKey) rawPublicKey;
                // Get x coordinate
                byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
                if (x[0] == 0)
                    x = Arrays.copyOfRange(x, 1, x.length);
                rpk.put("x", Hex.encodeHexString(x));

                // Get Y coordinate
                byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
                if (y[0] == 0)
                    y = Arrays.copyOfRange(y, 1, y.length);
                rpk.put("y", Hex.encodeHexString(y));

                // Get Curves params
                rpk.put("params", ecPublicKey.getParams().toString());

                // Get raw public key in format PKCS8 (DER encoding)
                rpk.put("pkcs8", Base64.encodeBase64String(ecPublicKey.getEncoded()));
            } else {
                throw new JsonGenerationException("Unsupported Public Key Format (only ECPublicKey supported).", gen);
            }
            element.put("rpk", rpk);
        }

        if (src.useX509Cert()) {
            element.put("x509", true);
        }

        gen.writeObject(element);
    }
}

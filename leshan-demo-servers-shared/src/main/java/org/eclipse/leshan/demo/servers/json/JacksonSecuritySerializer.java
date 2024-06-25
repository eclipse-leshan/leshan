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

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    public void serialize(SecurityInfo securityInfo, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        ObjectNode oSecInfo = JsonNodeFactory.instance.objectNode();
        oSecInfo.put("endpoint", securityInfo.getEndpoint());

        if (securityInfo.useSecureTransportLayer()) {
            // handle (D)TLS case :
            ObjectNode oTls = JsonNodeFactory.instance.objectNode();
            oSecInfo.set("tls", oTls);

            if (securityInfo.usePSK()) {
                // set detail for PSK
                ObjectNode oPsk = JsonNodeFactory.instance.objectNode();
                oPsk.put("identity", securityInfo.getPskIdentity());
                oPsk.put("key", Hex.encodeHexString(securityInfo.getPreSharedKey()));

                // set PSK field
                oTls.put("mode", "psk");
                oTls.set("details", oPsk);
            } else if (securityInfo.useRPK()) {
                // set details
                ObjectNode oRpk = JsonNodeFactory.instance.objectNode();
                PublicKey rawPublicKey = securityInfo.getRawPublicKey();
                oRpk.put("key", Hex.encodeHexString(rawPublicKey.getEncoded()));

                // set RPK field
                oTls.put("mode", "rpk");
                oTls.set("details", oRpk);
            } else if (securityInfo.useX509Cert()) {
                oTls.put("mode", "x509");
            }
        }
        if (securityInfo.useOSCORE()) {
            // handle OSCORE case :
            ObjectNode oOscore = JsonNodeFactory.instance.objectNode();
            oSecInfo.set("oscore", oOscore);
            oOscore.put("rid", Hex.encodeHexString(securityInfo.getOscoreSetting().getRecipientId()));
            oOscore.put("sid", Hex.encodeHexString(securityInfo.getOscoreSetting().getSenderId()));
            oOscore.put("msec", Hex.encodeHexString(securityInfo.getOscoreSetting().getMasterSecret()));
            // TODO OSCORE it should be possible to use an empty byte array for Master salf. (currently it failed)
            if (securityInfo.getOscoreSetting().getMasterSalt() != null) {
                oOscore.put("msalt", Hex.encodeHexString(securityInfo.getOscoreSetting().getMasterSalt()));
            }
            oOscore.put("aead", securityInfo.getOscoreSetting().getAeadAlgorithm().getValue());
            oOscore.put("hkdf", securityInfo.getOscoreSetting().getHkdfAlgorithm().getValue());
        }
        gen.writeTree(oSecInfo);
    }
}

/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
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
 *     Sierra Wireless - initial API and implementation
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.security;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;

import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.leshan.server.OscoreHandler;
import org.eclipse.leshan.util.Hex;
import org.eclipse.leshan.util.Validate;

/**
 * The security info for a client.
 * <p>
 * The following security modes are supported:
 * <ul>
 * <li>Pre-Shared Key: an identity and a key are needed</li>
 * <li>Raw Public Key Certificate: a public key is needed</li>
 * <li>X509 Certificate: an X509 certificate is needed</li>
 * <li>OSCORE: an OSCORE security context is needed</li>
 * </ul>
 */
public class SecurityInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    // the client end-point
    private final String endpoint;

    // PSK
    private final String identity;
    private final byte[] preSharedKey;

    private final PublicKey rawPublicKey;

    private final boolean useX509Cert;

    // OSCORE (FIXME: Save content properly information here. Must be serializable.)
    private final String oscoreIdentity;

    private SecurityInfo(String endpoint, String identity, byte[] preSharedKey, PublicKey rawPublicKey,
            boolean useX509Cert, OSCoreCtx oscoreCtx) {
        Validate.notEmpty(endpoint);
        this.endpoint = endpoint;
        this.identity = identity;
        this.preSharedKey = preSharedKey;
        this.rawPublicKey = rawPublicKey;
        this.useX509Cert = useX509Cert;
        this.oscoreIdentity = generateOscoreIdentity(oscoreCtx);
    }

    /**
     * Construct a {@link SecurityInfo} when using DTLS with Pre-Shared Keys.
     */
    public static SecurityInfo newPreSharedKeyInfo(String endpoint, String identity, byte[] preSharedKey) {
        Validate.notEmpty(identity);
        Validate.notNull(preSharedKey);
        return new SecurityInfo(endpoint, identity, preSharedKey, null, false, null);
    }

    /**
     * Construct a {@link SecurityInfo} when using DTLS with Raw Public Key (RPK).
     */
    public static SecurityInfo newRawPublicKeyInfo(String endpoint, PublicKey rawPublicKey) {
        Validate.notNull(rawPublicKey);
        return new SecurityInfo(endpoint, null, null, rawPublicKey, false, null);
    }

    /**
     * Construct a {@link SecurityInfo} when using DTLS with an X509 Certificate.
     */
    public static SecurityInfo newX509CertInfo(String endpoint) {
        return new SecurityInfo(endpoint, null, null, null, true, null);
    }

    /**
     * Construct a {@link SecurityInfo} when using OSCORE.
     */
    public static SecurityInfo newOSCoreInfo(String endpoint, OSCoreCtx oscoreCtx) {
        Validate.notNull(oscoreCtx);

        // Add the OSCORE Context to the context database
        HashMapCtxDB db = OscoreHandler.getContextDB();
        db.addContext(oscoreCtx);

        return new SecurityInfo(endpoint, null, null, null, false, oscoreCtx);
    }

    /**
     * Generates an OSCORE identity from an OSCORE context
     */
    private static String generateOscoreIdentity(OSCoreCtx oscoreCtx) {
        if (oscoreCtx == null) {
            return null;
        }

        String oscoreIdentity = "sid=" + Hex.encodeHexString(oscoreCtx.getSenderId()) + ",rid="
                + Hex.encodeHexString(oscoreCtx.getRecipientId());
        return oscoreIdentity;
    }

    public String getEndpoint() {
        return endpoint;
    }

    /**
     * The Pre-Shared-Key identity
     */
    public String getIdentity() {
        return identity;
    }

    public byte[] getPreSharedKey() {
        return preSharedKey;
    }

    /**
     * The OSCORE identity
     */
    public String getOscoreIdentity() {
        return oscoreIdentity;
    }

    public boolean usePSK() {
        return identity != null && preSharedKey != null;
    }

    public PublicKey getRawPublicKey() {
        return rawPublicKey;
    }

    public boolean useRPK() {
        return rawPublicKey != null;
    }

    public boolean useX509Cert() {
        return useX509Cert;
    }

    public boolean useOSCORE() {
        return oscoreIdentity != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
        result = prime * result + ((identity == null) ? 0 : identity.hashCode());
        result = prime * result + Arrays.hashCode(preSharedKey);
        result = prime * result + ((rawPublicKey == null) ? 0 : rawPublicKey.hashCode());
        result = prime * result + (useX509Cert ? 1231 : 1237);
        result = prime * result + ((oscoreIdentity == null) ? 0 : oscoreIdentity.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SecurityInfo other = (SecurityInfo) obj;
        if (endpoint == null) {
            if (other.endpoint != null)
                return false;
        } else if (!endpoint.equals(other.endpoint))
            return false;
        if (identity == null) {
            if (other.identity != null)
                return false;
        } else if (!identity.equals(other.identity))
            return false;
        if (!Arrays.equals(preSharedKey, other.preSharedKey))
            return false;
        if (rawPublicKey == null) {
            if (other.rawPublicKey != null)
                return false;
        } else if (!rawPublicKey.equals(other.rawPublicKey))
            return false;
        if (useX509Cert != other.useX509Cert)
            return false;
        if (oscoreIdentity == null) {
            if (other.oscoreIdentity != null)
                return false;
        } else if (!oscoreIdentity.equals(other.oscoreIdentity))
            return false;

        return true;
    }

    @Override
    public String toString() {
        // Note : preSharedKey is explicitly excluded from display for security purposes
        return String.format(
                "SecurityInfo [endpoint=%s, identity=%s, rawPublicKey=%s, useX509Cert=%s, oscoreIdentity=%s]", endpoint,
                identity, rawPublicKey, useX509Cert, oscoreIdentity);
    }

}

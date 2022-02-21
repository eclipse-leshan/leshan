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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.security;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.CoseException;
// TODO OSCORE leshan-server-core must not depends of Californium project
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.OscoreServerHandler;
import org.eclipse.leshan.server.security.oscore.OscoreSetting;

import com.upokecenter.cbor.CBORObject;

/**
 * The security info for a client.
 * <p>
 * A {@link SecurityInfo} contain data about how a client should authenticate itself.
 * <p>
 * The following security modes are supported:
 * <ul>
 * <li>Pre-Shared Key: the given identity and a key are needed</li>
 * <li>Raw Public Key Certificate: the given public key is needed</li>
 * <li>X509 Certificate: any trusted X509 certificate is needed</li>
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

    // RPK
    private final PublicKey rawPublicKey;

    // X.509
    private final boolean useX509Cert;

    // TODO OSCORE : Save content properly information here. Must be serializable.
    private final OscoreSetting oscoreSetting;

    private SecurityInfo(String endpoint, String identity, byte[] preSharedKey, PublicKey rawPublicKey,
            boolean useX509Cert, OscoreSetting oscoreSetting) {
        Validate.notEmpty(endpoint);
        this.endpoint = endpoint;
        this.identity = identity;
        this.preSharedKey = preSharedKey;
        this.rawPublicKey = rawPublicKey;
        this.useX509Cert = useX509Cert;
        this.oscoreSetting = oscoreSetting;
    }

    /**
     * Construct a {@link SecurityInfo} meaning that client with given endpoint name should authenticate itself using
     * PSK mode and the given PSK Identity and the given Pre-Shared Key.
     * 
     * @param endpoint the endpont name of the client.
     * @param identity the expected PSK Identity.
     * @param preSharedKey the expected Pre-Shared Key.
     * @return a PSK Security Info.
     */
    public static SecurityInfo newPreSharedKeyInfo(String endpoint, String identity, byte[] preSharedKey) {
        Validate.notEmpty(identity);
        Validate.notNull(preSharedKey);
        return new SecurityInfo(endpoint, identity, preSharedKey, null, false, null);
    }

    /**
     * Construct a {@link SecurityInfo} meaning that client with given endpoint name should authenticate itself using
     * RPK mode and the given Raw Public Key.
     * 
     * @param endpoint the endpont name of the client.
     * @param rawPublicKey the expected Raw Public Key.
     * @return a RPK Security Info.
     */
    public static SecurityInfo newRawPublicKeyInfo(String endpoint, PublicKey rawPublicKey) {
        Validate.notNull(rawPublicKey);
        return new SecurityInfo(endpoint, null, null, rawPublicKey, false, null);
    }

    /**
     * Construct a {@link SecurityInfo} meaning that client with given endpoint name should authenticate itself using
     * X.509 mode with any trusted X.509 Certificate.
     * <p>
     * By default, the certificate Common Name (CN) MUST match the endpoint name.
     * 
     * @param endpoint the endpont name of the client.
     * @return a X.509 Security Info.
     */
    public static SecurityInfo newX509CertInfo(String endpoint) {
        return new SecurityInfo(endpoint, null, null, null, true, null);
    }

    /**
     * Construct a {@link SecurityInfo} when using OSCORE.
     */
    // TODO OSCORE rename in newOscoreInfo
    public static SecurityInfo newOSCoreInfo(String endpoint, OscoreSetting oscoreSetting) {
        Validate.notNull(oscoreSetting);

        // TODO OSCORE remove access to context here.
        // Add the OSCORE Context to the context database
        HashMapCtxDB db = OscoreServerHandler.getContextDB();
        db.addContext(getContext(oscoreSetting));

        return new SecurityInfo(endpoint, null, null, null, false, oscoreSetting);
    }

    private static OSCoreCtx getContext(OscoreSetting oscoreSetting) {
        try {
            OSCoreCtx osCoreCtx = new OSCoreCtx(oscoreSetting.getMasterSecret(), true,
                    AlgorithmID.FromCBOR(CBORObject.FromObject(oscoreSetting.getAeadAlgorithm())),
                    oscoreSetting.getSenderId(), oscoreSetting.getRecipientId(),
                    AlgorithmID.FromCBOR(CBORObject.FromObject(oscoreSetting.getHmacAlgorithm())), 32,
                    oscoreSetting.getMasterSalt(), null, 1000);
            osCoreCtx.setContextRederivationEnabled(true);
            return osCoreCtx;
        } catch (OSException | CoseException e) {
            throw new IllegalStateException("Unable to create OSCoreContext", e);
        }
    }

    /**
     * Generates an OSCORE identity from an OSCORE context
     */
    private static String generateOscoreIdentity(OscoreSetting oscoreSetting) {
        if (oscoreSetting == null) {
            return null;
        }

        String oscoreIdentity = "sid=" + Hex.encodeHexString(oscoreSetting.getSenderId()) + ",rid="
                + Hex.encodeHexString(oscoreSetting.getRecipientId());
        return oscoreIdentity;
    }

    /**
     * @return the client endpoint name.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @return the Pre-Shared-Key identity or <code>null</code> if {@link #usePSK()} return <code>false</code>.
     * @see #getPreSharedKey()
     */
    public String getIdentity() {
        return identity;
    }

    /**
     * @return the Pre-Shared-Key or <code>null</code> if {@link #usePSK()} return <code>false</code>.
     * @see #getIdentity()
     */
    public byte[] getPreSharedKey() {
        return preSharedKey;
    }

    /**
     * @return the {@link OscoreSetting} or <code>null</code> if {@link #useOSCORE()} return <code>false</code>.
     */
    public OscoreSetting getOscoreSetting() {
        return oscoreSetting;
    }

    /**
     * @return <code>true</code> if this client should use PSK authentication.
     */
    public boolean usePSK() {
        return identity != null && preSharedKey != null;
    }

    /**
     * @return the {@link PublicKey} or <code>null</code> if {@link #useRPK()} returns <code>false</code>.
     */
    public PublicKey getRawPublicKey() {
        return rawPublicKey;
    }

    /**
     * @return <code>true</code> if this client should use RPK authentication.
     */
    public boolean useRPK() {
        return rawPublicKey != null;
    }

    /**
     * @return <code>true</code> if this client should use X.509 authentication.
     */
    public boolean useX509Cert() {
        return useX509Cert;
    }

    /**
     * @return The OSCORE identity
     */
    public String getOscoreIdentity() {
        // TODO OSCORE maybe we should use a dedicated class like :
        // https://github.com/eclipse/leshan/pull/1175/commits/a892ea34226fcf94bfe7956e4a392718ad5f768c#diff-fcd2ffa6ba28fbde0db07a2a8cfe5759ca5374ee732d9b3470eb98bae79b7ada
        return generateOscoreIdentity(oscoreSetting);
    }

    /**
     * @return <code>true</code> if this client should use OSCORE.
     */
    public boolean useOSCORE() {
        return oscoreSetting != null;
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
        result = prime * result + ((oscoreSetting == null) ? 0 : oscoreSetting.hashCode());
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
        if (oscoreSetting == null) {
            if (other.oscoreSetting != null)
                return false;
        } else if (!oscoreSetting.equals(other.oscoreSetting))
            return false;

        return true;
    }

    @Override
    public String toString() {
        // Note : preSharedKey is explicitly excluded from display for security purposes
        return String.format(
                "SecurityInfo [endpoint=%s, identity=%s, rawPublicKey=%s, useX509Cert=%s, oscoreIdentity=%s]", endpoint,
                identity, rawPublicKey, useX509Cert, getOscoreIdentity());
    }

}

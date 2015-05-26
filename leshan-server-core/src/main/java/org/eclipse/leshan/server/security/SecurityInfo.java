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
 *******************************************************************************/
package org.eclipse.leshan.server.security;

import java.io.Serializable;
import java.security.PublicKey;

import org.eclipse.leshan.util.Validate;

/**
 * The security info for a client.
 * <p>
 * The following security modes are supported:
 * <ul>
 * <li>Pre-Shared Key: an identity and a key are needed</li>
 * <li>Raw Public Key Certificate: a public key is needed</li>
 * <li>X509 Certificate: an X509 certificate is needed</li>
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

    private SecurityInfo(String endpoint, String identity, byte[] preSharedKey, PublicKey rawPublicKey,
            boolean useX509Cert) {
        Validate.notEmpty(endpoint);
        this.endpoint = endpoint;
        this.identity = identity;
        this.preSharedKey = preSharedKey;
        this.rawPublicKey = rawPublicKey;
        this.useX509Cert = useX509Cert;
    }

    /**
     * Construct a {@link SecurityInfo} when using DTLS with Pre-Shared Keys.
     */
    public static SecurityInfo newPreSharedKeyInfo(String endpoint, String identity, byte[] preSharedKey) {
        Validate.notEmpty(identity);
        Validate.notNull(preSharedKey);
        return new SecurityInfo(endpoint, identity, preSharedKey, null, false);
    }

    /**
     * Construct a {@link SecurityInfo} when using DTLS with Raw Public Key (RPK).
     */
    public static SecurityInfo newRawPublicKeyInfo(String endpoint, PublicKey rawPublicKey) {
        Validate.notNull(rawPublicKey);
        return new SecurityInfo(endpoint, null, null, rawPublicKey, false);
    }

    /**
     * Construct a {@link SecurityInfo} when using DTLS with an X509 Certificate.
     */
    public static SecurityInfo newX509CertInfo(String endpoint) {
        return new SecurityInfo(endpoint, null, null, null, true);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getIdentity() {
        return identity;
    }

    public byte[] getPreSharedKey() {
        return preSharedKey;
    }

    public PublicKey getRawPublicKey() {
        return rawPublicKey;
    }

    public boolean useX509Cert() {
        return useX509Cert;
    }
}

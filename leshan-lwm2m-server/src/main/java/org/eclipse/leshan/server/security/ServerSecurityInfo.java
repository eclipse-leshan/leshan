/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.server.security;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class ServerSecurityInfo {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private X509Certificate[] certificateChain;
    private Certificate[] trustedCertificates;

    public ServerSecurityInfo(PrivateKey privateKey, PublicKey publicKey, X509Certificate[] certificateChain,
            Certificate[] trustedCertificates) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.certificateChain = certificateChain;
        this.trustedCertificates = trustedCertificates;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }

    public Certificate[] getTrustedCertificates() {
        return trustedCertificates;
    }
}

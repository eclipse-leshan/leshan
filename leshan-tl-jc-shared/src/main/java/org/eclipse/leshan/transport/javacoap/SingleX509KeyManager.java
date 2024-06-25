/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.X509KeyManager;

public class SingleX509KeyManager implements X509KeyManager {

    private final PrivateKey privateKey;
    private final X509Certificate[] certChain;

    public SingleX509KeyManager(PrivateKey privateKey, X509Certificate[] certChain) {
        this.privateKey = privateKey;
        this.certChain = certChain;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return null;
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        if (Arrays.asList(keyType).contains(privateKey.getAlgorithm())) {
            return getAlias(certChain);
        }
        return null;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return null;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        if (privateKey.getAlgorithm().equals(keyType)) {
            return getAlias(certChain);
        }
        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return certChain;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return privateKey;
    }

    private String getAlias(X509Certificate[] certChain) {
        X509Certificate x509Certificate = certChain[0];
        String issuerName = x509Certificate.getIssuerX500Principal().getName();
        String algorithm = x509Certificate.getPublicKey().getAlgorithm();
        return algorithm + "_Certificate_" + issuerName;
    }
}

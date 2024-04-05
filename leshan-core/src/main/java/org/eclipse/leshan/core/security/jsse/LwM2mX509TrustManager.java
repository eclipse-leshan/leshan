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
package org.eclipse.leshan.core.security.jsse;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import org.eclipse.leshan.core.security.certificate.util.CertPathUtil;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier;
import org.eclipse.leshan.core.security.certificate.verifier.X509CertificateVerifier.Role;

public class LwM2mX509TrustManager extends X509ExtendedTrustManager {

    private final X509CertificateVerifier certificateVerifier;

    public LwM2mX509TrustManager(X509CertificateVerifier certificateVerifier) {
        this.certificateVerifier = certificateVerifier;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        // TODO not clear what this is about...
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        certificateVerifier.verifyCertificate(CertPathUtil.generateCertPath(Arrays.asList(chain)),
                getPeerAddress(socket), Role.CLIENT);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        certificateVerifier.verifyCertificate(CertPathUtil.generateCertPath(Arrays.asList(chain)),
                getPeerAddress(socket), Role.SERVER);
    }

    protected InetSocketAddress getPeerAddress(Socket socket) {
        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        certificateVerifier.verifyCertificate(CertPathUtil.generateCertPath(Arrays.asList(chain)),
                getPeerAddress(engine), Role.CLIENT);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        certificateVerifier.verifyCertificate(CertPathUtil.generateCertPath(Arrays.asList(chain)),
                getPeerAddress(engine), Role.SERVER);
    }

    protected InetSocketAddress getPeerAddress(SSLEngine engine) {
        if (engine.getPeerHost() == null)
            return null;

        return new InetSocketAddress(engine.getPeerHost(), engine.getPeerPort());
    }
}

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
package org.eclipse.leshan.transport.javacoap.server.coaps.bc.endpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.CertificateType;
import org.bouncycastle.tls.SecurityParameters;
import org.eclipse.leshan.core.security.util.SecurityUtil;
import org.eclipse.leshan.transport.javacoap.identity.PreSharedKeyPrincipal;
import org.eclipse.leshan.transport.javacoap.identity.RawPublicKeyPrincipal;
import org.eclipse.leshan.transport.javacoap.transport.context.keys.IpTransportContextKeys;
import org.eclipse.leshan.transport.javacoap.transport.context.keys.TlsTransportContextKeys;

import com.mbed.coap.transport.TransportContext;

public class CoapsBouncyCastleTransportResolver {

    public TransportContext getContext(SecurityParameters parameters, InetSocketAddress foreignPeerAddr) {

        TransportContext transport = TransportContext.of(IpTransportContextKeys.REMOTE_ADDRESS, foreignPeerAddr);
        Principal principal = getPrincipal(parameters);
        if (principal != null) {
            transport = transport.with(TlsTransportContextKeys.PRINCIPAL, principal); //
        }
        // TODO add more transport context ?
        // .with(TlsTransportContextKeys.TLS_SESSION_ID, new Opaque(parameters.getSessionID()).toHex()) //
        // .with(TlsTransportContextKeys.CIPHER_SUITE_ID, parameters.getCipherSuite()) //
        return transport;
    }

    private Principal getPrincipal(SecurityParameters parameters) {

        if (parameters.getPSKIdentity() != null) {
            return new PreSharedKeyPrincipal(new String(parameters.getPSKIdentity()));
        } else if (parameters.getClientCertificateType() == CertificateType.RawPublicKey
                && parameters.getPeerCertificate() != null && !parameters.getPeerCertificate().isEmpty()) {
            Certificate clientCertificate = parameters.getPeerCertificate();
            PublicKey publicKey;
            try {
                publicKey = SecurityUtil.publicKey.decode(clientCertificate.getCertificateAt(0).getEncoded());
            } catch (IOException | GeneralSecurityException e) {
                throw new IllegalStateException("unable to decode public key");
            }
            return new RawPublicKeyPrincipal(publicKey);
        } else if (parameters.getClientCertificateType() == CertificateType.X509
                && parameters.getPeerCertificate() != null && !parameters.getPeerCertificate().isEmpty()) {
            Certificate clientCertificate = parameters.getPeerCertificate();
            X509Certificate certificate;
            try {
                certificate = SecurityUtil.certificate.decode(clientCertificate.getCertificateAt(0).getEncoded());
            } catch (IOException | GeneralSecurityException e) {
                throw new IllegalStateException("unable to decode public key");
            }
            return certificate.getSubjectX500Principal();
        }

        return null;
    }
}

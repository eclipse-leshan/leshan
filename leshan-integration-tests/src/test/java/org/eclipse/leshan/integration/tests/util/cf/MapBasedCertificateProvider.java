/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util.cf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.scandium.dtls.CertificateIdentityResult;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.SignatureAndHashAlgorithm;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite.CertificateKeyAlgorithm;
import org.eclipse.californium.scandium.dtls.cipher.XECDHECryptography.SupportedGroup;
import org.eclipse.californium.scandium.dtls.x509.CertificateProvider;
import org.eclipse.californium.scandium.util.ServerName;
import org.eclipse.californium.scandium.util.ServerNames;

public class MapBasedCertificateProvider implements CertificateProvider {

    private final Map<String, CertPair> certPairs;
    private final List<CertificateType> supportedCertificateTypes;
    private final List<CertificateKeyAlgorithm> supportedCertificateKeyAlgorithms;

    public MapBasedCertificateProvider(Map<String, CertPair> certPairs) {
        this.certPairs = certPairs;
        this.supportedCertificateTypes = Collections.unmodifiableList(Arrays.asList(CertificateType.X_509));

        // extract supported algorithm
        Set<CertificateKeyAlgorithm> supportedAlgo = new HashSet<>();
        for (CertPair pair : certPairs.values()) {
            supportedAlgo.add(CertificateKeyAlgorithm.getAlgorithm(pair.getCertChain()[0].getPublicKey()));
        }
        this.supportedCertificateKeyAlgorithms = Collections.unmodifiableList(new ArrayList<>(supportedAlgo));
    }

    @Override
    public List<CertificateKeyAlgorithm> getSupportedCertificateKeyAlgorithms() {
        return supportedCertificateKeyAlgorithms;
    }

    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return supportedCertificateTypes;
    }

    @Override
    public CertificateIdentityResult requestCertificateIdentity(ConnectionId cid, boolean client,
            List<X500Principal> issuers, ServerNames serverNames,
            List<CertificateKeyAlgorithm> certificateKeyAlgorithms,
            List<SignatureAndHashAlgorithm> signatureAndHashAlgorithms, List<SupportedGroup> curves) {
        if (serverNames == null) {
            return null;
        } else {
            for (ServerName serverName : serverNames) {
                CertPair certPair = certPairs.get(serverName.getNameAsString());
                if (certPair != null) {
                    return new CertificateIdentityResult(cid, certPair.getPrivateKey(),
                            Arrays.asList(certPair.getCertChain()), null);
                }
            }
        }
        return null;
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
        // sync store
    }

}

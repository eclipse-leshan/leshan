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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class CertPair {

    private final X509Certificate[] certChain;
    private final PrivateKey privateKey;

    public CertPair(X509Certificate[] certChain, PrivateKey privateKey) {
        this.certChain = certChain;
        this.privateKey = privateKey;
    }

    public X509Certificate[] getCertChain() {
        return certChain;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}

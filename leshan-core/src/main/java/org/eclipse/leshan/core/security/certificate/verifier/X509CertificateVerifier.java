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
package org.eclipse.leshan.core.security.certificate.verifier;

import java.net.InetSocketAddress;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;

public interface X509CertificateVerifier {

    CertPath verifyCertificate(CertPath remotePeerCertChain, InetSocketAddress remotePeerAddress)
            throws CertificateException;
}

/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.impl;

import java.util.Map;

/**
 * Implement the different Public Key Infrastructure functions for enrolling and re-enrolling device. This can be seen
 * as the RA (Registration Authority) of the PKI.
 */
public interface PkiService {
    /**
     * Return the chain of CA certificates for this Registration Authority. The returned chain should be used by the
     * device for authenticating using the delivered certificate.
     */
    public byte[] getCaCertificates();

    /**
     * Enroll the provided CSR for the verified end-point and with the requested attributes.
     * 
     * @return the signed X.509 certificate (no base64 encoding)
     * 
     *         TODO: exception
     */
    public byte[] enroll(byte[] csr, String commonName, Map<String, Object> attributes);
}

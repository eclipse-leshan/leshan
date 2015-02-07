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
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.security.PublicKey;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.dtls.DTLSSession;

/**
 * A {@link CoAPEndpoint} for communications using DTLS security.
 */
public class SecureEndpoint extends CoAPEndpoint {

    private final DTLSConnector connector;

    public SecureEndpoint(DTLSConnector connector) {
        super(connector, NetworkConfig.getStandard());
        this.connector = connector;
    }

    /**
     * Returns the PSK identity from the DTLS session associated with the given request.
     * 
     * @param request the CoAP request
     * @return the PSK identity of the client or <code>null</code> if not found.
     */
    public String getPskIdentity(Request request) {
        return this.getSession(request).getPskIdentity();
    }

    /**
     * Returns the Raw Public Key (RPK) from the DTLS session associated with the given request.
     * 
     * @param request the CoAP request
     * @return the Raw Public Key of the client or <code>null</code> if not found.
     */
    public PublicKey getRawPublicKey(Request request) {
        return this.getSession(request).getPeerRawPublicKey();
    }

    public DTLSConnector getDTLSConnector() {
        return connector;
    }

    private DTLSSession getSession(Request request) {
        return connector.getSessionByAddress(new InetSocketAddress(request.getSource(), request.getSourcePort()));
    }
}

/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - initial implementation.
 ******************************************************************************/
package org.eclipse.leshan.client.californium.impl;

import java.net.InetSocketAddress;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.leshan.client.californium.SecureSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage session of secure endpoint.
 * 
 * Supports only {@link DTLSConnector} based endoints.
 */
public class SecureEndpointSessionManager implements SecureSessionManager {
    private static final Logger LOG = LoggerFactory.getLogger(SecureEndpointSessionManager.class);

    private final DTLSConnector connector;

    public SecureEndpointSessionManager(CoapEndpoint endpoint) {
        Connector connector = endpoint == null ? null : endpoint.getConnector();
        if (connector instanceof DTLSConnector) {
            this.connector = (DTLSConnector) connector;
        } else {
            this.connector = null;
        }
    }

    @Override
    public void forceResumeSessionFor(InetSocketAddress peer) {
        if (null != connector) {
            LOG.info("update secure session for {}", peer);
            connector.forceResumeSessionFor(peer);
        }
    }

    @Override
    public String getSessionIdFor(InetSocketAddress peer) {
        if (null != connector) {
            DTLSSession session = connector.getSessionByAddress(peer);
            if (null != session) {
                return session.getSessionIdentifier().toString();
            }
        }
        return null;
    }

}

/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity.SYSTEM
 *******************************************************************************/
package org.eclipse.leshan.client.californium.impl;

import static org.eclipse.leshan.LwM2mId.*;
import static org.eclipse.leshan.client.request.ServerIdentity.SYSTEM;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.request.ReadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a {@link PskStore} which search PSK credentials in Lwm2m Security object.
 */
public class SecurityObjectPskStore implements PskStore {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityObjectPskStore.class);

    private final LwM2mObjectEnabler securityEnabler;

    /**
     * Warning : The securityEnabler should not contains 2 or more entries with the same identity. This is not a LWM2M
     * specification constraint but an implementation limitation.
     */
    public SecurityObjectPskStore(LwM2mObjectEnabler securityEnabler) {
        this.securityEnabler = securityEnabler;
    }

    @Override
    public byte[] getKey(PskPublicInformation identity) {
        if (identity == null)
            return null;

        byte[] res = null;

        LwM2mObject securities = (LwM2mObject) securityEnabler.read(SYSTEM, new ReadRequest(SECURITY)).getContent();
        for (LwM2mObjectInstance security : securities.getInstances().values()) {
            long securityMode = (long) security.getResource(SEC_SECURITY_MODE).getValue();
            if (securityMode == SecurityMode.PSK.code) // psk
            {
                byte[] pskIdentity = (byte[]) security.getResource(SEC_PUBKEY_IDENTITY).getValue();
                if (Arrays.equals(identity.getBytes(), pskIdentity)) {
                    if (res == null) {
                        // we continue to check if the is duplication
                        res = (byte[]) security.getResource(SEC_SECRET_KEY).getValue();
                    } else {
                        LOG.warn("There is several security object instance with the same psk identity : '{}'",
                                identity);
                        // we find 1 duplication and warn for it no need to continue.
                        return res;
                    }
                }
            }
        }
        return res;
    }

    @Override
    public byte[] getKey(ServerNames serverNames, PskPublicInformation identity) {
        // serverNames is not supported
        return getKey(identity);
    }

    @Override
    public PskPublicInformation getIdentity(InetSocketAddress inetAddress) {
        if (inetAddress == null)
            return null;

        LwM2mObject securities = (LwM2mObject) securityEnabler.read(SYSTEM, new ReadRequest(SECURITY)).getContent();
        for (LwM2mObjectInstance security : securities.getInstances().values()) {
            long securityMode = (long) security.getResource(SEC_SECURITY_MODE).getValue();
            if (securityMode == SecurityMode.PSK.code) {
                try {
                    URI uri = new URI((String) security.getResource(SEC_SERVER_URI).getValue());
                    if (inetAddress.equals(ServerInfo.getAddress(uri))) {
                        byte[] pskIdentity = (byte[]) security.getResource(SEC_PUBKEY_IDENTITY).getValue();
                        return new PskPublicInformation(new String(pskIdentity));
                    }
                } catch (URISyntaxException e) {
                    LOG.error(String.format("Invalid URI %s", (String) security.getResource(SEC_SERVER_URI).getValue()),
                            e);
                }
            }
        }
        return null;
    }

    @Override
    public PskPublicInformation getIdentity(InetSocketAddress peerAddress, ServerNames virtualHost) {
        // TODO should we support SNI ?
        throw new UnsupportedOperationException();
    }
}

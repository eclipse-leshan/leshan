/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
package org.eclipse.leshan.client.object;

import static org.eclipse.leshan.LwM2mId.*;
import static org.eclipse.leshan.client.request.ServerIdentity.SYSTEM;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.request.ReadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityObjectUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityObjectUtil.class);

    public static LwM2mObjectInstance getSecurityInstance(LwM2mObjectEnabler securityEnabler, InetSocketAddress addr) {
        // Read security object
        LwM2mObject securities = (LwM2mObject) securityEnabler.read(SYSTEM, new ReadRequest(SECURITY)).getContent();

        // Search instance with the given addr
        for (LwM2mObjectInstance security : securities.getInstances().values()) {
            try {
                URI uri = new URI((String) security.getResource(SEC_SERVER_URI).getValue());
                if (addr.equals(ServerInfo.getAddress(uri))) {
                    return security;
                }
            } catch (URISyntaxException e) {
                LOG.warn(String.format("Invalid URI %s", (String) security.getResource(SEC_SERVER_URI).getValue()), e);
            }
        }
        return null;
    }

    public static SecurityMode getSecurityMode(LwM2mObjectInstance securityInstance) {
        return SecurityMode.fromCode((long) securityInstance.getResource(SEC_SECURITY_MODE).getValue());
    }

    public static String getPskIdentity(LwM2mObjectInstance securityInstance) {
        byte[] pubKey = (byte[]) securityInstance.getResource(SEC_PUBKEY_IDENTITY).getValue();
        return new String(pubKey);
    }

    public static byte[] getPskKey(LwM2mObjectInstance securityInstance) {
        return (byte[]) securityInstance.getResource(SEC_SECRET_KEY).getValue();
    }
}

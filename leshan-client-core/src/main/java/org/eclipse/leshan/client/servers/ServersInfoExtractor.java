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
package org.eclipse.leshan.client.servers;

import static org.eclipse.leshan.LwM2mId.SECURITY;
import static org.eclipse.leshan.LwM2mId.SEC_BOOTSTRAP;
import static org.eclipse.leshan.LwM2mId.SEC_SECURITY_MODE;
import static org.eclipse.leshan.LwM2mId.SEC_SERVER_ID;
import static org.eclipse.leshan.LwM2mId.SEC_SERVER_URI;
import static org.eclipse.leshan.LwM2mId.SERVER;
import static org.eclipse.leshan.LwM2mId.SRV_BINDING;
import static org.eclipse.leshan.LwM2mId.SRV_LIFETIME;
import static org.eclipse.leshan.LwM2mId.SRV_SERVER_ID;
import static org.eclipse.leshan.client.request.ServerIdentity.SYSTEM;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract from LwM2m tree servers information like server uri, security mode, ...
 */
public class ServersInfoExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(ServersInfoExtractor.class);

    public static ServersInfo getInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        LwM2mObjectEnabler securityEnabler = objectEnablers.get(SECURITY);
        LwM2mObjectEnabler serverEnabler = objectEnablers.get(SERVER);

        if (securityEnabler == null || serverEnabler == null)
            return null;

        ServersInfo infos = new ServersInfo();
        LwM2mObject securities = (LwM2mObject) securityEnabler.read(SYSTEM, new ReadRequest(SECURITY)).getContent();
        LwM2mObject servers = (LwM2mObject) serverEnabler.read(SYSTEM, new ReadRequest(SERVER)).getContent();

        for (LwM2mObjectInstance security : securities.getInstances().values()) {
            try {
                if ((boolean) security.getResource(SEC_BOOTSTRAP).getValue()) {
                    if (infos.bootstrap != null) {
                        LOG.warn("There is more than one bootstrap configuration in security object.");
                    } else {
                        // create bootstrap info
                        ServerInfo info = new ServerInfo();
                        info.serverId = (long) security.getResource(SEC_SERVER_ID).getValue();
                        info.serverUri = new URI((String) security.getResource(SEC_SERVER_URI).getValue());
                        info.secureMode = (long) security.getResource(SEC_SECURITY_MODE).getValue();
                        infos.bootstrap = info;
                    }
                } else {
                    // create device management info
                    DmServerInfo info = new DmServerInfo();
                    info.serverUri = new URI((String) security.getResource(SEC_SERVER_URI).getValue());
                    info.serverId = (long) security.getResource(SEC_SERVER_ID).getValue();
                    info.secureMode = (long) security.getResource(SEC_SECURITY_MODE).getValue();

                    // search corresponding device management server
                    for (LwM2mObjectInstance server : servers.getInstances().values()) {
                        if (info.serverId == (Long) server.getResource(SRV_SERVER_ID).getValue()) {
                            info.lifetime = (long) server.getResource(SRV_LIFETIME).getValue();
                            info.binding = BindingMode.valueOf((String) server.getResource(SRV_BINDING).getValue());

                            infos.deviceMangements.put(info.serverId, info);
                            break;
                        }
                    }

                }
            } catch (URISyntaxException e) {
                LOG.error(String.format("Invalid URI %s", (String) security.getResource(SEC_SERVER_URI).getValue()), e);
            }
        }
        return infos;
    }
}

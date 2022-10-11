/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.client.endpoint;

import java.security.cert.Certificate;
import java.util.Collection;
import java.util.List;

import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServerInfo;

public interface LwM2mClientEndpointsProvider {

    void init(LwM2mObjectTree objectTree, DownlinkRequestReceiver requestReceiver, ClientEndpointToolbox toolbox);

    ServerIdentity createEndpoint(ServerInfo serverInfo, boolean clientInitiatedOnly, List<Certificate> trustStore,
            ClientEndpointToolbox toolbox);

    Collection<ServerIdentity> createEndpoints(Collection<? extends ServerInfo> serverInfo, boolean clientInitiatedOnly,
            List<Certificate> trustStore, ClientEndpointToolbox toolbox);

    void destroyEndpoints();

    void start();

    List<LwM2mClientEndpoint> getEndpoints();

    LwM2mClientEndpoint getEndpoint(ServerIdentity server);

    void stop();

    void destroy();

}

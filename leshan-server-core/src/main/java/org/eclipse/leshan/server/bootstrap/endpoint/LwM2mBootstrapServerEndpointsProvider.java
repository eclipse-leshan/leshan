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
package org.eclipse.leshan.server.bootstrap.endpoint;

import java.net.URI;
import java.util.List;

import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.request.BootstrapUplinkRequestReceiver;
import org.eclipse.leshan.server.security.ServerSecurityInfo;

public interface LwM2mBootstrapServerEndpointsProvider {

    List<LwM2mBootstrapServerEndpoint> getEndpoints();

    LwM2mBootstrapServerEndpoint getEndpoint(URI uri);

    void createEndpoints(BootstrapUplinkRequestReceiver requestReceiver, BootstrapServerEndpointToolbox toolbox,
            ServerSecurityInfo serverSecurityInfo, LeshanBootstrapServer server);

    void start();

    void stop();

    void destroy();

}

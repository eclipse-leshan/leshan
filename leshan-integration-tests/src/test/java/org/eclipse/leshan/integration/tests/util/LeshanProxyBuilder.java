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
package org.eclipse.leshan.integration.tests.util;

import java.net.InetSocketAddress;

import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;

public class LeshanProxyBuilder {

    public static ReverseProxy givenReverseProxyFor(LeshanTestServer server, Protocol protocol) {
        EndpointUri serverEndpointUri = server.getEndpoint(protocol).getURI();
        return new ReverseProxy(new InetSocketAddress("localhost", 0),
                new InetSocketAddress(serverEndpointUri.getHost(), serverEndpointUri.getPort()));
    }
}

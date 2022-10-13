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
package org.eclipse.leshan.client.californium.endpoint.coap;

import java.net.InetSocketAddress;

import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointFactory;

public class CoapOscoreProtocolProvider extends CoapClientProtocolProvider {

    @Override
    public CaliforniumClientEndpointFactory createDefaultEndpointFactory(InetSocketAddress address) {
        return new CoapOscoreClientEndpointFactory(address);
    }
}

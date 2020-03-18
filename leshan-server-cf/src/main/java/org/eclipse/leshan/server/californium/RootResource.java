/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium;

import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * A default root resource.
 */
public class RootResource extends CoapResource {

    private CoapServer coapServer;

    public RootResource(CoapServer coapServer) {
        super("");
        setVisible(false);
        this.coapServer = coapServer;
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        exchange.respond(ResponseCode.NOT_FOUND);
    }

    @Override
    public List<Endpoint> getEndpoints() {
        return coapServer.getEndpoints();
    }
}
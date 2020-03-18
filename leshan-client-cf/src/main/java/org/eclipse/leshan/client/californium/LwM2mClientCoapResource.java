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
package org.eclipse.leshan.client.californium;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;
import org.eclipse.leshan.core.request.Identity;

/**
 * A Common {@link CoapResource} used to handle LWM2M request with some specific method for LWM2M client.
 */
public class LwM2mClientCoapResource extends LwM2mCoapResource {

    protected final BootstrapHandler bootstrapHandler;

    public LwM2mClientCoapResource(String name, BootstrapHandler bootstrapHandler) {
        super(name);
        this.bootstrapHandler = bootstrapHandler;
    }

    @Override
    protected Identity extractIdentity(EndpointContext context) {
        return extractServerIdentity(context);
    }

    /**
     * Create Leshan {@link ServerIdentity} from Californium {@link EndpointContext}.
     * 
     * @param context The Californium {@link EndpointContext} to convert.
     * @return The corresponding Leshan {@link ServerIdentity}.
     * @throws IllegalStateException if we are not able to extract {@link ServerIdentity}.
     */
    protected ServerIdentity extractServerIdentity(EndpointContext context) {
        Identity identity = EndpointContextUtil.extractIdentity(context);

        if (bootstrapHandler.isBootstrapServer(identity)) {
            return ServerIdentity.createLwm2mBootstrapServerIdentity(identity);
        }

        return ServerIdentity.createLwm2mServerIdentity(identity);
    }

    /**
     * Create Leshan {@link ServerIdentity} from Californium {@link Exchange}.
     * 
     * @param context The Californium {@link Exchange} containing the request for which we search sender identity.
     * @return The corresponding Leshan {@link ServerIdentity}.
     * @throws IllegalStateException if we are not able to extract {@link ServerIdentity}.
     */
    protected ServerIdentity extractServerIdentity(CoapExchange exchange) {
        return extractServerIdentity(exchange.advanced().getRequest().getSourceContext());
    }
}

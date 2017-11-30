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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *******************************************************************************/
package org.eclipse.leshan.client.californium.impl;

import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.servers.BootstrapHandler;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.request.Identity;

public class ResourceUtil {
    // TODO: validate addresses using the security object instances?
    // in case of multiple bootstrap instances of the security object,
    // using BootstrapHandler may be not the right choice, because it
    // handles the current bootstrap server.
    public static ServerIdentity extractServerIdentity(CoapExchange exchange, BootstrapHandler bootstrapHandler) {
        Identity identity = EndpointContextUtil.extractIdentity(exchange.advanced().getRequest().getSourceContext());

        if (bootstrapHandler.isBootstrapServer(identity)) {
            return ServerIdentity.createLwm2mBootstrapServerIdentity(identity);
        }

        return ServerIdentity.createLwm2mServerIdentity(identity);
    }
}

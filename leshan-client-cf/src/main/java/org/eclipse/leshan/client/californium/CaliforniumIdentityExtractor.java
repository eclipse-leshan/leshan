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
package org.eclipse.leshan.client.californium;

import org.eclipse.californium.core.network.Exchange.Origin;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.request.Identity;

public abstract class CaliforniumIdentityExtractor {

    protected EndpointContext getForeignPeerContext(CoapExchange exchange) {
        return exchange.advanced().getOrigin() == Origin.REMOTE ? exchange.advanced().getRequest().getSourceContext()
                : exchange.advanced().getRequest().getDestinationContext();
    }

    public abstract Identity getIdentity(CoapExchange exchange);

}

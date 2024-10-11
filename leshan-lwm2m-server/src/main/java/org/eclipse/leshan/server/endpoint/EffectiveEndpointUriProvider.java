/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.server.endpoint;

import org.eclipse.leshan.core.endpoint.EndpointUri;

/**
 * When creating LwM2M Server Endpoint, you can define its URI. (e.g. <code>coap://localhost:5683</code>)
 * <p>
 * For convenience this is possible to provide uri with port value at 0 meaning that system will pick up an ephemeral
 * port in a {@code bind} operation. The issue is that we will know effective endpoint URI only once the Server Endpoint
 * is started (once the bind operation is done and so port is attributed)
 * <p>
 * This class is responsible to provide the effective {@link EndpointUri} of a given {@link LwM2mServerEndpoint} . This
 * is sometime necessary when some sub-components of an endpoint need to know effective endpoint URI but we can not
 * provide a direct reference to {@link LwM2mServerEndpoint} at creation.
 * <p>
 * Note : When this class is needed that sounds like design is not so good (kind of cyclic dependencies ...) but for
 * now, we didn't find better alternative.
 */
public class EffectiveEndpointUriProvider {

    private LwM2mServerEndpoint endpoint;

    public void setEndpoint(LwM2mServerEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public EndpointUri getEndpointUri() {
        return endpoint.getURI();
    }
}

/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use Identity as destination
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.request;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpoint;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpointsProvider;

/**
 * The default implementation of {@link BootstrapDownlinkRequestSender}.
 */
public class DefaultBootstrapDownlinkRequestSender implements BootstrapDownlinkRequestSender {

    private final LwM2mBootstrapServerEndpointsProvider endpointsProvider;

    /**
     * @param endpointsProvider which provides available {@link LwM2mBootstrapServerEndpoint}
     */
    public DefaultBootstrapDownlinkRequestSender(LwM2mBootstrapServerEndpointsProvider endpointsProvider) {
        this.endpointsProvider = endpointsProvider;
    }

    @Override
    public <T extends LwM2mResponse> T send(BootstrapSession destination, BootstrapDownlinkRequest<T> request,
            long timeoutInMs) throws InterruptedException {

        // find endpoint to use
        LwM2mBootstrapServerEndpoint endpoint = endpointsProvider.getEndpoint(destination.getEndpointUsed());

        // Send requests synchronously
        T response = endpoint.send(destination, request, timeoutInMs);
        return response;
    }

    @Override
    public <T extends LwM2mResponse> void send(final BootstrapSession destination, BootstrapDownlinkRequest<T> request,
            long timeoutInMs, final ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {

        // find endpoint to use
        LwM2mBootstrapServerEndpoint endpoint = endpointsProvider.getEndpoint(destination.getEndpointUsed());

        // Send requests asynchronously
        endpoint.send(destination, request, new ResponseCallback<T>() {
            @Override
            public void onResponse(T response) {
                responseCallback.onResponse(response);
            }
        }, errorCallback, timeoutInMs);
    }

    @Override
    public void cancelOngoingRequests(BootstrapSession session) {
        for (LwM2mBootstrapServerEndpoint endpoint : endpointsProvider.getEndpoints()) {
            endpoint.cancelRequests(session.getId());
        }
    }
}

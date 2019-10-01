/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use Identity as destination
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.core.californium.CoapResponseCallback;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.californium.observation.ObservationServiceImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.util.Validate;

public class CaliforniumLwM2mRequestSender implements LwM2mRequestSender, CoapRequestSender {

    private final ObservationServiceImpl observationService;
    private final LwM2mModelProvider modelProvider;
    private final RequestSender sender;

    /**
     * @param observationService the service for keeping track of observed resources
     * @param modelProvider provides the supported objects definitions
     */
    public CaliforniumLwM2mRequestSender(Endpoint secureEndpoint, Endpoint nonSecureEndpoint,
            ObservationServiceImpl observationService, LwM2mModelProvider modelProvider, LwM2mNodeEncoder encoder,
            LwM2mNodeDecoder decoder) {
        Validate.notNull(observationService);
        Validate.notNull(modelProvider);
        this.observationService = observationService;
        this.modelProvider = modelProvider;
        this.sender = new RequestSender(secureEndpoint, nonSecureEndpoint, encoder, decoder);
    }

    @Override
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException {

        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Send requests synchronously
        T response = sender.sendLwm2mRequest(destination.getEndpoint(), destination.getIdentity(), destination.getId(),
                model, destination.getRootPath(), request, timeout);

        // Handle special observe case
        if (response != null && response.getClass() == ObserveResponse.class && response.isSuccess()) {
            observationService.addObservation(destination, ((ObserveResponse) response).getObservation());
        }
        return response;
    }

    @Override
    public <T extends LwM2mResponse> void send(final Registration destination, DownlinkRequest<T> request, long timeout,
            final ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        // Retrieve the objects definition
        final LwM2mModel model = modelProvider.getObjectModel(destination);

        // Send requests asynchronously
        sender.sendLwm2mRequest(destination.getEndpoint(), destination.getIdentity(), destination.getId(), model,
                destination.getRootPath(), request, timeout, new ResponseCallback<T>() {
                    @Override
                    public void onResponse(T response) {
                        if (response != null && response.getClass() == ObserveResponse.class && response.isSuccess()) {
                            observationService.addObservation(destination,
                                    ((ObserveResponse) response).getObservation());
                        }
                        responseCallback.onResponse(response);
                    }
                }, errorCallback);
    }

    @Override
    public Response sendCoapRequest(Registration destination, Request coapRequest, long timeout)
            throws InterruptedException {
        return sender.sendCoapRequest(destination.getIdentity(), destination.getId(), coapRequest, timeout);
    }

    @Override
    public void sendCoapRequest(Registration destination, Request coapRequest, long timeout,
            CoapResponseCallback responseCallback, ErrorCallback errorCallback) {
        sender.sendCoapRequest(destination.getIdentity(), destination.getId(), coapRequest, timeout, responseCallback,
                errorCallback);
    }

    @Override
    public void cancelPendingRequests(Registration registration) {
        Validate.notNull(registration);
        sender.cancelRequests(registration.getId());
    }
}

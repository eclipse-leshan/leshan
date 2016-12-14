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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.eclipse.leshan.server.client.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumLwM2mBootstrapRequestSender implements LwM2mBootstrapRequestSender {
    static final Logger LOG = LoggerFactory.getLogger(CaliforniumLwM2mBootstrapRequestSender.class);

    private final Endpoint nonSecureEndpoint;
    private final Endpoint secureEndpoint;
    private final LwM2mModel model;
    private final LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
    private final LwM2mNodeEncoder encoder = new DefaultLwM2mNodeEncoder();

    public CaliforniumLwM2mBootstrapRequestSender(final Endpoint secureEndpoint, final Endpoint nonSecureEndpoint,
            final LwM2mModel model) {
        this.secureEndpoint = secureEndpoint;
        this.nonSecureEndpoint = nonSecureEndpoint;
        this.model = model;
    }

    @Override
    public <T extends LwM2mResponse> T send(final String endpointName, final InetSocketAddress clientAddress,
            final boolean secure, final DownlinkRequest<T> request, Long timeout) throws InterruptedException {
        // Create the CoAP request from LwM2m request
        final CoapRequestBuilder coapClientRequestBuilder = new CoapRequestBuilder(clientAddress, model, encoder);
        request.accept(coapClientRequestBuilder);

        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Send CoAP request synchronously
        final SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeout) {
            @Override
            public T buildResponse(final Response coapResponse) {
                // TODO we need to fix that by removing the Client dependency from LwM2MResponseBuilder or by creating a
                // LwM2mBootstrapResponseBuilder
                Registration registration = new Registration.Builder("fakeregistrationid", endpointName, clientAddress.getAddress(),
                        clientAddress.getPort(), secure ? secureEndpoint.getAddress() : nonSecureEndpoint.getAddress())
                                .build();
                // Build LwM2m response
                final LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<T>(coapRequest,
                        coapResponse, registration, model, null, decoder);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        coapRequest.addMessageObserver(syncMessageObserver);

        // Send CoAP request asynchronously
        if (secure)
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    @Override
    public <T extends LwM2mResponse> void send(final String endpointName, final InetSocketAddress clientAddress,
            final boolean secure, final DownlinkRequest<T> request, final ResponseCallback<T> responseCallback,
            final ErrorCallback errorCallback) {
        // Create the CoAP request from LwM2m request
        final CoapRequestBuilder coapClientRequestBuilder = new CoapRequestBuilder(clientAddress, model, encoder);
        request.accept(coapClientRequestBuilder);
        final Request coapRequest = coapClientRequestBuilder.getRequest();

        // Add CoAP request callback
        coapRequest.addMessageObserver(new AsyncRequestObserver<T>(coapRequest, responseCallback, errorCallback) {

            @Override
            public T buildResponse(final Response coapResponse) {
                // TODO we need to fix that by removing the Client dependency from LwM2MResponseBuilder or by creating a
                // LwM2mBootstrapResponseBuilder
                Registration registration = new Registration.Builder("fakeregistrationid", endpointName, clientAddress.getAddress(),
                        clientAddress.getPort(), secure ? secureEndpoint.getAddress() : nonSecureEndpoint.getAddress())
                                .build();

                // Build LwM2m response
                final LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<T>(coapRequest,
                        coapResponse, registration, model, null, decoder);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        });

        // Send CoAP request asynchronously
        if (secure)
            secureEndpoint.sendRequest(coapRequest);
        else
            nonSecureEndpoint.sendRequest(coapRequest);
    }
}

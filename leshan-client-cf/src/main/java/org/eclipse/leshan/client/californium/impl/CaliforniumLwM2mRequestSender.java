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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.client.californium.impl;

import java.net.InetSocketAddress;

import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.client.request.LwM2mRequestSender;
import org.eclipse.leshan.core.californium.AsyncRequestObserver;
import org.eclipse.leshan.core.californium.SyncRequestObserver;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

public class CaliforniumLwM2mRequestSender implements LwM2mRequestSender {

    private final CaliforniumEndpointsManager endpointsManager;

    public CaliforniumLwM2mRequestSender(CaliforniumEndpointsManager endpointsManager) {
        this.endpointsManager = endpointsManager;
    }

    @Override
    public <T extends LwM2mResponse> T send(InetSocketAddress serverAddress, boolean secure,
            final UplinkRequest<T> request, long timeout) throws InterruptedException {
        // Create the CoAP request from LwM2m request
        CoapRequestBuilder coapClientRequestBuilder = new CoapRequestBuilder(serverAddress);
        request.accept(coapClientRequestBuilder);
        Request coapRequest = coapClientRequestBuilder.getRequest();

        // Toggle OSCORE use in the request if the target URI of the request has an OSCORE context registered
        HashMapCtxDB db = HashMapCtxDB.getInstance();
        try {
            if (db.getContext(coapRequest.getURI()) != null) {
                coapRequest.getOptions().setOscore(Bytes.EMPTY);
            }
        } catch (OSException e) {
            System.err.println("Failed to retrieve OSCORE Context for request");
            e.printStackTrace();
        }

        // Send CoAP request synchronously
        SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeout) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                LwM2mClientResponseBuilder<T> lwm2mResponseBuilder = new LwM2mClientResponseBuilder<>(coapResponse);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        coapRequest.addMessageObserver(syncMessageObserver);

        // Send CoAP request asynchronously
        endpointsManager.getEndpoint(null).sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    @Override
    public <T extends LwM2mResponse> void send(InetSocketAddress serverAddress, boolean secure,
            final UplinkRequest<T> request, long timeout, ResponseCallback<T> responseCallback,
            ErrorCallback errorCallback) {
        // Create the CoAP request from LwM2m request
        CoapRequestBuilder coapClientRequestBuilder = new CoapRequestBuilder(serverAddress);
        request.accept(coapClientRequestBuilder);
        Request coapRequest = coapClientRequestBuilder.getRequest();

        // Toggle OSCORE use in the request if the target URI of the request has an OSCORE context registered
        HashMapCtxDB db = HashMapCtxDB.getInstance();
        try {
            if (db.getContext(coapRequest.getURI()) != null) {
                coapRequest.getOptions().setOscore(Bytes.EMPTY);
            }
        } catch (OSException e) {
            System.err.println("Failed to retrieve OSCORE Context for request");
            e.printStackTrace();
        }

        // Add CoAP request callback
        MessageObserver obs = new AsyncRequestObserver<T>(coapRequest, responseCallback, errorCallback, timeout) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                LwM2mClientResponseBuilder<T> lwm2mResponseBuilder = new LwM2mClientResponseBuilder<>(coapResponse);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        coapRequest.addMessageObserver(obs);

        // Send CoAP request asynchronously
        endpointsManager.getEndpoint(null).sendRequest(coapRequest);
    }

}

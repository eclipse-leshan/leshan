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
package org.eclipse.leshan.client.californium.impl;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.request.AbstractLwM2mClientRequest;
import org.eclipse.leshan.client.request.AbstractRegisteredLwM2mClientRequest;
import org.eclipse.leshan.client.request.BootstrapRequest;
import org.eclipse.leshan.client.request.DeregisterRequest;
import org.eclipse.leshan.client.request.LwM2mClientRequestVisitor;
import org.eclipse.leshan.client.request.LwM2mContentRequest;
import org.eclipse.leshan.client.request.RegisterRequest;
import org.eclipse.leshan.client.request.UpdateRequest;
import org.eclipse.leshan.client.util.LinkFormatUtils;

public class CoapClientRequestBuilder implements LwM2mClientRequestVisitor {
    private Request coapRequest;

    private Endpoint coapEndpoint;

    private boolean parametersValid = false;

    private final InetSocketAddress serverAddress;

    private long timeout;

    private final LinkObject[] clientObjectModel;

    public CoapClientRequestBuilder(final InetSocketAddress serverAddress, final LinkObject... clientObjectModel) {
        this.serverAddress = serverAddress;
        this.clientObjectModel = clientObjectModel;
    }

    @Override
    public void visit(final BootstrapRequest request) {
        coapRequest = Request.newPost();
        buildRequestSettings(request);

        coapRequest.getOptions().addUriPath("bs");
        coapRequest.getOptions().addUriQuery("ep=" + request.getClientEndpointIdentifier());

        parametersValid = true;

    }

    @Override
    public void visit(final RegisterRequest request) {
        if (!areParametersValid(request.getClientParameters())) {
            return;
        }
        coapRequest = Request.newPost();
        buildRequestSettings(request);

        coapRequest.getOptions().addUriPath("rd");
        coapRequest.getOptions().addUriQuery("ep=" + request.getClientEndpointIdentifier());
        buildRequestContent(request);

        parametersValid = true;
    }

    @Override
    public void visit(final UpdateRequest request) {
        if (!areParametersValid(request.getClientParameters())) {
            return;
        }
        coapRequest = Request.newPut();
        buildRequestSettings(request);

        buildLocationPath(request);
        buildRequestContent(request);

        parametersValid = true;

    }

    @Override
    public void visit(final DeregisterRequest request) {
        coapRequest = Request.newDelete();
        buildRequestSettings(request);

        buildLocationPath(request);

        parametersValid = true;

    }

    public Request getRequest() {
        return coapRequest;
    }

    public Endpoint getEndpoint() {
        return coapEndpoint;
    }

    public boolean areParametersValid() {
        return parametersValid;
    }

    public long getTimeout() {
        return timeout;
    }

    private void buildLocationPath(final AbstractRegisteredLwM2mClientRequest request) {
        request.getClientIdentifier().accept(coapRequest);
    }

    private void buildRequestSettings(final AbstractLwM2mClientRequest request) {
        timeout = request.getTimeout();
        coapRequest.setDestination(serverAddress.getAddress());
        coapRequest.setDestinationPort(serverAddress.getPort());
    }

    private void buildRequestContent(final LwM2mContentRequest request) {
        for (final Entry<String, String> entry : request.getClientParameters().entrySet()) {
            coapRequest.getOptions().addUriQuery(entry.getKey() + "=" + entry.getValue());
        }

        final String payload = LinkFormatUtils.payloadize(clientObjectModel);
        coapRequest.setPayload(payload);
    }

    private boolean areParametersValid(final Map<String, String> parameters) {
        for (final Map.Entry<String, String> p : parameters.entrySet()) {
            switch (p.getKey()) {
            case "lt":
                break;
            case "lwm2m":
                break;
            case "sms":
                return false;
            case "b":
                if (!isBindingValid(p.getValue())) {
                    return false;
                }
                break;
            default:
                return false;
            }
        }

        return true;
    }

    private boolean isBindingValid(final String value) {
        return value.equals("U");
    }
}

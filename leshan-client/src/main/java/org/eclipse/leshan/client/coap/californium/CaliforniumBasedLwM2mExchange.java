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
package org.eclipse.leshan.client.coap.californium;

import java.util.List;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.response.CreateResponse;
import org.eclipse.leshan.client.response.LwM2mResponse;
import org.eclipse.leshan.client.util.ObserveSpecParser;

public class CaliforniumBasedLwM2mExchange implements LwM2mExchange {

    private final CoapExchange exchange;

    public CaliforniumBasedLwM2mExchange(final CoapExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void respond(final LwM2mResponse response) {
        if (response instanceof CreateResponse) {
            final String objectId = getObjectId();
            exchange.setLocationPath(objectId + "/" + ((CreateResponse) response).getLocation());
        }

        exchange.respond(leshanToCalifornium(response.getCode()), response.getResponsePayload());
    }

    private ResponseCode leshanToCalifornium(final org.eclipse.leshan.ResponseCode code) {
        switch (code) {
        case BAD_REQUEST:
            return ResponseCode.BAD_REQUEST;
        case CHANGED:
            return ResponseCode.CHANGED;
        case CONTENT:
            return ResponseCode.CONTENT;
        case CREATED:
            return ResponseCode.CREATED;
        case DELETED:
            return ResponseCode.DELETED;
        case METHOD_NOT_ALLOWED:
            return ResponseCode.METHOD_NOT_ALLOWED;
        case NOT_FOUND:
            return ResponseCode.NOT_FOUND;
        case UNAUTHORIZED:
            return ResponseCode.UNAUTHORIZED;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public byte[] getRequestPayload() {
        return exchange.getRequestPayload();
    }

    private String getObjectId() {
        return getUriPaths().get(0);
    }

    @Override
    public boolean hasObjectInstanceId() {
        return getUriPaths().size() > 1;
    }

    @Override
    public int getObjectInstanceId() {
        final List<String> paths = getUriPaths();
        return paths.size() >= 2 ? Integer.parseInt(paths.get(1)) : 0;
    }

    private List<String> getUriPaths() {
        return exchange.getRequestOptions().getUriPath();
    }

    @Override
    public boolean isObserve() {
        return exchange.getRequestOptions().hasObserve() && exchange.getRequestCode() == CoAP.Code.GET;
    }

    @Override
    public ObserveSpec getObserveSpec() {
        if (exchange.advanced().getRequest().getOptions().getURIQueryCount() == 0) {
            return null;
        }
        final List<String> uriQueries = exchange.advanced().getRequest().getOptions().getUriQuery();
        return ObserveSpecParser.parse(uriQueries);
    }

}

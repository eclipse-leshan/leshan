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
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.observation.Observation;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CaliforniumObservation extends MessageObserverAdapter implements Observation {
    private final Logger LOG = LoggerFactory.getLogger(CaliforniumObservation.class);

    private final Request coapRequest;
    private final List<ObservationListener> listeners = new CopyOnWriteArrayList<>();
    private final Client client;
    private final LwM2mPath path;

    public CaliforniumObservation(Request coapRequest, Client client, LwM2mPath path) {
        Validate.notNull(coapRequest);
        Validate.notNull(client);
        Validate.notNull(path);

        this.coapRequest = coapRequest;
        this.client = client;
        this.path = path;
    }

    public CaliforniumObservation(Request coapRequest, Client client, LwM2mPath path, ObservationListener listener) {
        this(coapRequest, client, path);
        this.listeners.add(listener);
    }

    public CaliforniumObservation(Request coapRequest, Client client, LwM2mPath path,
            List<ObservationListener> listeners) {
        this(coapRequest, client, path);
        this.listeners.addAll(listeners);
    }

    @Override
    public void cancel() {
        coapRequest.cancel();
    }

    @Override
    public void onResponse(Response coapResponse) {
        if (coapResponse.getCode() == CoAP.ResponseCode.CHANGED || coapResponse.getCode() == CoAP.ResponseCode.CONTENT) {
            try {
                LwM2mNode content = LwM2mNodeDecoder.decode(coapResponse.getPayload(),
                        ContentFormat.fromCode(coapResponse.getOptions().getContentFormat()), path);
                ValueResponse response = new ValueResponse(ResponseCode.CHANGED, content);

                for (ObservationListener listener : listeners) {
                    listener.newValue(this, response.getContent());
                }
            } catch (InvalidValueException e) {
                String msg = String.format("[%s] ([%s])", e.getMessage(), e.getPath().toString());
                LOG.debug(msg);
            }
        }
    }

    @Override
    public void onCancel() {
        for (ObservationListener listener : listeners) {
            listener.cancelled(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client getClient() {
        return client;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LwM2mPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return String.format("CaliforniumObservation [%s]", path);
    }

    @Override
    public void addListener(ObservationListener listener) {
        listeners.add(listener);

    }

    @Override
    public void removeListener(ObservationListener listener) {
        listeners.remove(listener);

    }
}

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
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationListener;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CaliforniumObservation extends MessageObserverAdapter implements Observation {
    private final Logger LOG = LoggerFactory.getLogger(CaliforniumObservation.class);

    private final Request coapRequest;
    private final List<ObservationListener> listeners = new CopyOnWriteArrayList<>();
    private final String registrationId;
    private final LwM2mPath path;
    private final LwM2mModel model;

    public CaliforniumObservation(Request coapRequest, String registrationId, LwM2mPath path, LwM2mModel model) {
        Validate.notNull(coapRequest);
        Validate.notNull(registrationId);
        Validate.notNull(path);
        Validate.notNull(model);

        this.coapRequest = coapRequest;
        this.registrationId = registrationId;
        this.path = path;
        this.model = model;
    }

    @Override
    public void cancel() {
        coapRequest.cancel();
    }

    @Override
    public void onResponse(Response coapResponse) {
        // TODO remove the CHANGED test case, the spec say now a successful notify should be a 2.05 content
        if (coapResponse.getCode() == CoAP.ResponseCode.CHANGED || coapResponse.getCode() == CoAP.ResponseCode.CONTENT) {
            try {
                LwM2mNode content = LwM2mNodeDecoder.decode(coapResponse.getPayload(),
                        ContentFormat.fromCode(coapResponse.getOptions().getContentFormat()), path, model);
                for (ObservationListener listener : listeners) {
                    listener.newValue(this, content);
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
    public String getRegistrationId() {
        return registrationId;
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

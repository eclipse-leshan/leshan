/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein (Bosch Software Innovations GmbH) 
 *                     - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mClientRequestSender;
import org.eclipse.leshan.client.californium.impl.ObjectResource;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.util.Validate;

/**
 * This client has a special ability to have its GET handler instrumented by a test. It is very useful especially if a
 * client is not assumed to be always online, i.e. to sleep and wake up sometimes, so that different communications
 * scenarios could be tested.
 */
public class QueuedModeLeshanClient implements LwM2mClient {

    private boolean running;
    private CoapServer clientSideServer;
    private final Object startMonitor = new Object();
    private final CaliforniumLwM2mClientRequestSender requestSender;
    private final List<LwM2mObjectEnabler> objectEnablers;
    private OnGetCallback onGetCallback = null;

    private class CustomObjectResource extends ObjectResource {

        private LwM2mObjectEnabler nodeEnabler;

        public CustomObjectResource(final LwM2mObjectEnabler nodeEnabler) {
            super(nodeEnabler);
            this.nodeEnabler = nodeEnabler;
            this.nodeEnabler.setNotifySender(this);
            setObservable(true);
        }

        @Override
        public void handleGET(final CoapExchange exchange) {
            if (onGetCallback != null) {
                if (onGetCallback.handleGet(exchange)) {
                    super.handleGET(exchange);
                }
            } else {
                super.handleGET(exchange);
            }
        }
    }

    public interface OnGetCallback {

        /**
         * @return true, if the super implementation should be called, otherwise false.
         */
        boolean handleGet(CoapExchange coapExchange);
    }

    public QueuedModeLeshanClient(final InetSocketAddress clientAddress, final InetSocketAddress serverAddress,
            final List<LwM2mObjectEnabler> objectEnablers) {
        Validate.notNull(clientAddress);
        Validate.notNull(serverAddress);
        Validate.notNull(objectEnablers);
        Validate.notEmpty(objectEnablers);

        final Endpoint endpoint = new CoAPEndpoint(clientAddress);

        clientSideServer = new CoapServer();
        clientSideServer.addEndpoint(endpoint);

        this.objectEnablers = new ArrayList<>(objectEnablers);
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            if (clientSideServer.getRoot().getChild(Integer.toString(enabler.getId())) != null) {
                throw new IllegalArgumentException("Trying to load Client Object of name '" + enabler.getId()
                        + "' when one was already added.");
            }

            final ObjectResource clientObject = new CustomObjectResource(enabler);
            clientSideServer.add(clientObject);
        }

        requestSender = new CaliforniumLwM2mClientRequestSender(clientSideServer.getEndpoint(clientAddress),
                serverAddress, this);
    }

    @Override
    public void start() {
        synchronized (startMonitor) {
            if (!running) {
                clientSideServer.start();
                running = true;
            }
        }
    }

    @Override
    public void stop() {
        synchronized (startMonitor) {
            if (running) {
                clientSideServer.stop();
                running = false;
            }
        }
    }

    @Override
    public <T extends LwM2mResponse> T send(final UplinkRequest<T> request) {
        return requestSender.send(request, null);
    }

    @Override
    public <T extends LwM2mResponse> T send(final UplinkRequest<T> request, final long timeout) {
        return requestSender.send(request, timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(final UplinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
        requestSender.send(request, responseCallback, errorCallback);
    }

    @Override
    public List<LwM2mObjectEnabler> getObjectEnablers() {
        return objectEnablers;
    }

    public void setOnGetCallback(final OnGetCallback onGetCallback) {
        this.onGetCallback = onGetCallback;
    }
}

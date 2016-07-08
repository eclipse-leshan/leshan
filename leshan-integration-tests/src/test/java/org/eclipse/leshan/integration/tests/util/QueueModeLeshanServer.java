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

import org.eclipse.californium.core.CoapServer;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.response.ResponseListener;
import org.eclipse.leshan.server.security.SecurityRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueModeLeshanServer implements LwM2mServer {
    private static final Logger LOG = LoggerFactory.getLogger(QueueModeLeshanServer.class);
    private final CoapServer coapServer;
    private final ClientRegistry clientRegistry;
    private final ObservationRegistry observationRegistry;
    private final SecurityRegistry securityRegistry;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mRequestSender lwM2mRequestSender;
    private final MessageStore messageStore;

    public QueueModeLeshanServer(CoapServer coapServer, ClientRegistry clientRegistry,
            ObservationRegistry observationRegistry, SecurityRegistry securityRegistry,
            LwM2mModelProvider modelProvider, LwM2mRequestSender lwM2mRequestSender,
            MessageStore inMemoryMessageStore) {

        this.coapServer = coapServer;
        this.clientRegistry = clientRegistry;
        this.observationRegistry = observationRegistry;
        this.securityRegistry = securityRegistry;
        this.modelProvider = modelProvider;
        this.lwM2mRequestSender = lwM2mRequestSender;
        this.messageStore = inMemoryMessageStore;

        // Cancel observations on client unregistering
        this.clientRegistry.addListener(new ClientRegistryListener() {

            @Override
            public void updated(ClientUpdate update, Client clientUpdated) {
            }

            @Override
            public void unregistered(Client client) {
                QueueModeLeshanServer.this.observationRegistry.cancelObservations(client);
                QueueModeLeshanServer.this.lwM2mRequestSender.cancelPendingRequests(client);
            }

            @Override
            public void registered(Client client) {
            }
        });

    }

    @Override
    public void start() {
        // Start registries
        if (clientRegistry instanceof Startable) {
            ((Startable) clientRegistry).start();
        }
        if (securityRegistry instanceof Startable) {
            ((Startable) securityRegistry).start();
        }
        if (observationRegistry instanceof Startable) {
            ((Startable) observationRegistry).start();
        }

        // Start server
        coapServer.start();

        LOG.info("LW-M2M server started");
    }

    @Override
    public void stop() {
        // Stop server
        coapServer.stop();

        // Start registries
        if (clientRegistry instanceof Stoppable) {
            ((Stoppable) clientRegistry).stop();
        }
        if (securityRegistry instanceof Stoppable) {
            ((Stoppable) securityRegistry).stop();
        }
        if (observationRegistry instanceof Stoppable) {
            ((Stoppable) observationRegistry).stop();
        }
        if (lwM2mRequestSender instanceof Stoppable) {
            ((Stoppable) lwM2mRequestSender).stop();
        }

        LOG.info("LW-M2M server stopped");
    }

    @Override
    public void destroy() {
        // Destroy server
        coapServer.destroy();

        // Destroy registries
        if (clientRegistry instanceof Destroyable) {
            ((Destroyable) clientRegistry).destroy();
        }
        if (securityRegistry instanceof Destroyable) {
            ((Destroyable) securityRegistry).destroy();
        }
        if (observationRegistry instanceof Destroyable) {
            ((Destroyable) observationRegistry).destroy();
        }

        LOG.info("LW-M2M server destroyed");
    }

    @Override
    public <T extends LwM2mResponse> T send(Client destination, DownlinkRequest<T> request)
            throws InterruptedException {
        throw new UnsupportedOperationException("Server doesn't support synchronous sending of messages");
    }

    @Override
    public <T extends LwM2mResponse> T send(Client destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException {
        throw new UnsupportedOperationException("Server doesn't support synchronous sending of messages");
    }

    @Override
    public <T extends LwM2mResponse> void send(Client destination, DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        // Noop.
    }

    @Override
    public <T extends LwM2mResponse> void send(Client destination, String requestTicket, DownlinkRequest<T> request) {
        lwM2mRequestSender.send(destination, requestTicket, request);
    }

    @Override
    public void addResponseListener(ResponseListener listener) {
        lwM2mRequestSender.addResponseListener(listener);
    }

    @Override
    public void removeResponseListener(ResponseListener listener) {
        lwM2mRequestSender.removeResponseListener(listener);
    }

    @Override
    public ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    @Override
    public ObservationRegistry getObservationRegistry() {
        return observationRegistry;
    }

    @Override
    public SecurityRegistry getSecurityRegistry() {
        return securityRegistry;
    }

    @Override
    public LwM2mModelProvider getModelProvider() {
        return modelProvider;
    }

    public MessageStore getMessageStore() {
        return messageStore;
    }

    public LwM2mRequestSender getLwM2mRequestSender() {
        return lwM2mRequestSender;
    }
}

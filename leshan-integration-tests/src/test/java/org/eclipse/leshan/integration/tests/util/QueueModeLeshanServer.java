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
import org.eclipse.leshan.server.security.SecurityRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class QueueModeLeshanServer implements LwM2mServer {
    private static final Logger LOG = LoggerFactory.getLogger(QueueModeLeshanServer.class);
    private final CoapServer coapServer;
    private final ClientRegistry clientRegistry;
    private final ObservationRegistry observationRegistry;
    private final SecurityRegistry securityRegistry;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mRequestSender queueRequestSender;
    private final MessageStore messageStore;
    private final long sendTimeout;

    public QueueModeLeshanServer(final CoapServer coapServer, final ClientRegistry clientRegistry,
            final ObservationRegistry observationRegistry, final SecurityRegistry securityRegistry,
            final LwM2mModelProvider modelProvider, final LwM2mRequestSender queueRequestSender,
            final MessageStore inMemoryMessageStore, long sendTimeout) {

        this.coapServer = coapServer;
        this.clientRegistry = clientRegistry;
        this.observationRegistry = observationRegistry;
        this.securityRegistry = securityRegistry;
        this.modelProvider = modelProvider;
        this.queueRequestSender = queueRequestSender;
        this.messageStore = inMemoryMessageStore;
        this.sendTimeout = sendTimeout;

        // Cancel observations on client unregistering
        clientRegistry.addListener(new ClientRegistryListener() {

            @Override
            public void updated(ClientUpdate update, Client clientUpdated) {
            }

            @Override
            public void unregistered(final Client client) {
                observationRegistry.cancelObservations(client);
            }

            @Override
            public void registered(final Client client) {
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
        if (queueRequestSender instanceof Stoppable) {
            ((Stoppable) queueRequestSender).stop();
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
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request)
            throws InterruptedException {
        return queueRequestSender.send(destination, request, sendTimeout);
    }

    @Override
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request,
            final long timeout) throws InterruptedException {
        return queueRequestSender.send(destination, request, timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(final Client destination, final DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
        queueRequestSender.send(destination, request, responseCallback, errorCallback);
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
}

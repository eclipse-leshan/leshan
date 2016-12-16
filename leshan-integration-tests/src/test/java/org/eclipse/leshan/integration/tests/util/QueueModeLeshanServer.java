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
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationService;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.response.ResponseListener;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueModeLeshanServer implements LwM2mServer {
    private static final Logger LOG = LoggerFactory.getLogger(QueueModeLeshanServer.class);
    private final CoapServer coapServer;
    private final RegistrationService registrationService;
    private final ObservationService observationService;
    private final EditableSecurityStore securityStore;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mRequestSender lwM2mRequestSender;
    private final MessageStore messageStore;

    public QueueModeLeshanServer(CoapServer coapServer, RegistrationService registrationService,
            ObservationService observationService, EditableSecurityStore securityStore,
            LwM2mModelProvider modelProvider, LwM2mRequestSender lwM2mRequestSender,
            MessageStore inMemoryMessageStore) {

        this.coapServer = coapServer;
        this.registrationService = registrationService;
        this.observationService = observationService;
        this.securityStore = securityStore;
        this.modelProvider = modelProvider;
        this.lwM2mRequestSender = lwM2mRequestSender;
        this.messageStore = inMemoryMessageStore;

        // Cancel observations on client unregistering
        this.registrationService.addListener(new RegistrationListener() {

            @Override
            public void updated(RegistrationUpdate update, Registration updatedRegistration) {
            }

            @Override
            public void unregistered(Registration registration) {
                QueueModeLeshanServer.this.observationService.cancelObservations(registration);
                QueueModeLeshanServer.this.lwM2mRequestSender.cancelPendingRequests(registration);
            }

            @Override
            public void registered(Registration registration) {
            }
        });

    }

    @Override
    public void start() {
        // Start registries
        if (registrationService instanceof Startable) {
            ((Startable) registrationService).start();
        }
        if (securityStore instanceof Startable) {
            ((Startable) securityStore).start();
        }
        if (observationService instanceof Startable) {
            ((Startable) observationService).start();
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
        if (registrationService instanceof Stoppable) {
            ((Stoppable) registrationService).stop();
        }
        if (securityStore instanceof Stoppable) {
            ((Stoppable) securityStore).stop();
        }
        if (observationService instanceof Stoppable) {
            ((Stoppable) observationService).stop();
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
        if (registrationService instanceof Destroyable) {
            ((Destroyable) registrationService).destroy();
        }
        if (securityStore instanceof Destroyable) {
            ((Destroyable) securityStore).destroy();
        }
        if (observationService instanceof Destroyable) {
            ((Destroyable) observationService).destroy();
        }

        LOG.info("LW-M2M server destroyed");
    }

    @Override
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request)
            throws InterruptedException {
        throw new UnsupportedOperationException("Server doesn't support synchronous sending of messages");
    }

    @Override
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException {
        throw new UnsupportedOperationException("Server doesn't support synchronous sending of messages");
    }

    @Override
    public <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        // Noop.
    }

    @Override
    public <T extends LwM2mResponse> void send(Registration destination, String requestTicket, DownlinkRequest<T> request) {
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
    public RegistrationService getRegistrationService() {
        return registrationService;
    }

    @Override
    public ObservationService getObservationService() {
        return observationService;
    }

    @Override
    public EditableSecurityStore getSecurityStore() {
        return securityStore;
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

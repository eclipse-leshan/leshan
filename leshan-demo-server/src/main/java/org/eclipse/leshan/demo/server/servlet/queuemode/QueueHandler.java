/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.demo.server.servlet.queuemode;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkDeviceManagementRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;

/**
 * This is a very simple in memory way to store request when device is sleeping.
 */
public class QueueHandler {

    private final LeshanServer server;
    private final ConcurrentMap<String, QueueRequestData> requestsToSend;

    private class QueueRequestData {
        private long timeout;
        private DownlinkDeviceManagementRequest<? extends LwM2mResponse> request;
        private CompletableFuture<LwM2mResponse> responseFuture;
    }

    public QueueHandler(LeshanServer server) {
        this.server = server;
        this.requestsToSend = new ConcurrentHashMap<>();

        // Handle Presence Service Event
        server.getPresenceService().addListener(new PresenceListener() {
            @Override
            public void onSleeping(IRegistration registration) {
            }

            @Override
            public void onAwake(IRegistration registration) {
                // try to send store request
                QueueRequestData data = requestsToSend.remove(registration.getId());
                if (data != null) {
                    try {
                        server.send(registration, data.request, data.timeout, //
                                r -> {
                                    data.responseFuture.complete(r);
                                }, //
                                err -> {
                                    data.responseFuture.completeExceptionally(err);
                                });
                    } catch (RuntimeException e) {
                        data.responseFuture.completeExceptionally(e);
                    }
                }
            }
        });

        // Handle Registration Service Event
        server.getRegistrationService().addListener(new RegistrationListener() {
            @Override
            public void updated(RegistrationUpdate update, IRegistration updatedReg, IRegistration previousReg) {
            }

            @Override
            public void unregistered(IRegistration registration, Collection<Observation> observations, boolean expired,
                    IRegistration newReg) {
                QueueRequestData data = requestsToSend.remove(registration.getId());
                if (data != null) {
                    data.responseFuture.cancel(false);
                }
            }

            @Override
            public void registered(IRegistration registration, IRegistration previousReg,
                    Collection<Observation> previousObservations) {
            }
        });

    }

    public CompletableFuture<LwM2mResponse> send(IRegistration destination, DownlinkDeviceManagementRequest<?> request,
            long timeoutInMs) throws InterruptedException {

        // is client awake ?
        boolean useQueueMode = destination.usesQueueMode();
        boolean clientAwake = server.getPresenceService().isClientAwake(destination);

        // client is awake we try to send request now
        CompletableFuture<LwM2mResponse> future = new CompletableFuture<>();
        if (clientAwake || !useQueueMode) {
            try {
                // TODO ideally we should use async way to send request
                LwM2mResponse response = server.send(destination, request, timeoutInMs);
                future.complete(response);
            } catch (ClientSleepingException e) {
                clientAwake = false;
            }
        }

        // client is not awake we queue the request for later.
        if (useQueueMode && !clientAwake) {
            QueueRequestData data = new QueueRequestData();
            data.request = request;
            data.responseFuture = future;
            data.timeout = timeoutInMs;
            QueueRequestData previous = requestsToSend.put(destination.getId(), data);
            // Cancel previous future as we store only last request
            if (previous != null) {
                previous.responseFuture.cancel(false);
            }

            // We want to be sure there still a registration for this ID
            // The idea of this code is to avoid race condition which could lead to memory leak,
            // In case where Registration is removed before we push QueueRequestData.
            // Any better idea to handle this is welcomed.
            requestsToSend.compute(destination.getId(), (id, currentData) -> {
                if (server.getRegistrationService().getById(id) == null) {
                    // registration was removed, so we don't want to keep data for it
                    currentData.responseFuture.cancel(false);
                    return null;
                } else {
                    // registration is still there, we can add it
                    return currentData;
                }
            });
        }
        return future;
    }
}

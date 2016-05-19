/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multiple requests extensions. Send multiple request during registration without waiting for responses. Violates
 * N-START 1 (depends on congestion control in COAP layer). Used for robustness test of clients. The extension toggles
 * between normal registrations and registrations with multiple requests to see, if the client is able to work under
 * "normal" condition when it was "stressed before.
 */
public class MultiRequestExtension implements ClientRegistryListener, LeshanServerExtension {
    /**
     * Configuration key for number of multiple requests.
     * 
     * @see #multiRequests
     */
    public static final String CONFIG_MULTI_REQUESTS = "MULTI_REQUESTS";
    /**
     * Configuration key for number of registrations processed without multiple requests.
     * 
     * @see #normalOperation
     */
    public static final String CONFIG_NORMAL_OPERATION = "NORMAL_OPERATION";

    private static final Logger LOG = LoggerFactory.getLogger(MultiRequestExtension.class);

    /**
     * Default value for number of multiple requests.
     * 
     * @see #multiRequests
     */
    private static final int DEFAULT_MULTI_REQUESTS = 3;
    /**
     * Default value for number of registrations processed without multiple requests.
     * 
     * @see #normalOperation
     */
    private static final int DEFAULT_NORMAL_OPERATION = 4;

    /**
     * Counter of registrations.
     */
    private final AtomicInteger counter = new AtomicInteger();
    private volatile boolean enabled;
    private volatile LeshanServer server;
    /**
     * Number of multiple request send during registration.
     */
    private volatile int multiRequests = DEFAULT_MULTI_REQUESTS;
    /**
     * Number of normal processed registrations.
     */
    private volatile int normalOperation = DEFAULT_NORMAL_OPERATION;

    public MultiRequestExtension() {
    }

    @Override
    public void setup(LeshanServer lwServer, ExtensionConfig configuration, LeshanServerExtensionsManager manager) {
        server = lwServer;
        multiRequests = configuration.get(CONFIG_MULTI_REQUESTS, DEFAULT_MULTI_REQUESTS);
        normalOperation = configuration.get(CONFIG_NORMAL_OPERATION, DEFAULT_NORMAL_OPERATION);
        LOG.info("Multi requests: " + multiRequests + ", normal operation: " + normalOperation);
    }

    @Override
    public void start() {
        enabled = true;
        LOG.info("Extension multi requests enabled");
    }

    @Override
    public void stop() {
        enabled = false;
        LOG.info("Extension multi requests disabled");
    }

    @Override
    public void registered(final Client client) {
        multipleRequests(client);
    }

    @Override
    public void updated(ClientUpdate update, Client clientUpdated) {
        multipleRequests(clientUpdated);
    }

    @Override
    public void unregistered(Client client) {
    }

    /**
     * Send multiple request. Send multiple request, if enabled and not processing a normal registration.
     * 
     * @param client registering client
     */
    private void multipleRequests(final Client client) {
        if (!enabled)
            return;
        int count = counter.incrementAndGet();
        if (1 == count) {
            try {
                LOG.info("double request for " + client.getEndpoint());
                server.send(client, new ReadRequest(3, 0), responseCallback, errorCallback);
                if (1 < multiRequests) {
                    for (int i = 1; i < multiRequests; ++i) {
                        server.send(client, new ReadRequest(3, 0, 13), responseCallback, errorCallback);
                    }
                }
                Thread.sleep(500);
            } catch (Throwable ex) {
                LOG.error("error during double request for " + client.getEndpoint(), ex);
            }
        } else if (normalOperation == count) {
            counter.set(0);
        }
    }

    private final ResponseCallback<ReadResponse> responseCallback = new ResponseCallback<ReadResponse>() {
        @Override
        public void onResponse(ReadResponse response) {
        }
    };

    private final ErrorCallback errorCallback = new ErrorCallback() {
        @Override
        public void onError(Exception e) {
        }
    };

}

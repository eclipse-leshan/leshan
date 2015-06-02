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
 *     Bosch Software Innovations GmbH - separated from client registry
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The registry cleaner checks the client registry periodically for outdated registrations and cleans them, if
 * necessary.
 */
public class RegistrationCleaner implements Startable, Stoppable {

    private final ScheduledExecutorService schedExecutor = Executors.newScheduledThreadPool(1);
    private final ClientRegistry clientRegistry;
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationCleaner.class);

    /**
     * Creates a new periodic registry cleaner.
     * 
     * @param clientRegistry client registry to check
     */
    public RegistrationCleaner(final ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
    }

    @Override
    public void start() {
        // every 2 seconds clean the registration list
        // TODO re-consider clean-up interval: wouldn't 5 minutes do as well?
        schedExecutor.scheduleAtFixedRate(new Cleaner(), 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Registration cleaner was interrupted.", e);
        }
    }

    private class Cleaner implements Runnable {
        @Override
        public void run() {
            for (Client client : clientRegistry.allClients()) {
                synchronized (client) {
                    if (!client.isAlive()) {
                        // force de-registration
                        clientRegistry.deregisterClient(client.getRegistrationId());
                    }
                }
            }
        }
    }

}

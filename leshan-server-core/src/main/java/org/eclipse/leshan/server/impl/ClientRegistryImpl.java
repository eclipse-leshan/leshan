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
package org.eclipse.leshan.server.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In memory client registry
 */
public class ClientRegistryImpl implements ClientRegistry, Startable, Stoppable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientRegistryImpl.class);

    private final Map<String /* end-point */, Client> clientsByEp = new ConcurrentHashMap<>();

    private final List<ClientRegistryListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(ClientRegistryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ClientRegistryListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<Client> allClients() {
        return Collections.unmodifiableCollection(clientsByEp.values());
    }

    @Override
    public Client get(String endpoint) {
        return clientsByEp.get(endpoint);
    }

    @Override
    public boolean registerClient(Client client) {
        Validate.notNull(client);

        LOG.debug("Registering new client: {}", client);

        Client previous = clientsByEp.put(client.getEndpoint(), client);
        if (previous != null) {
            for (ClientRegistryListener l : listeners) {
                l.unregistered(previous);
            }
        }
        for (ClientRegistryListener l : listeners) {
            l.registered(client);
        }

        return true;
    }

    @Override
    public Client updateClient(ClientUpdate update) {
        Validate.notNull(update);

        LOG.debug("Updating registration for client: {}", update);
        Client client = findByRegistrationId(update.getRegistrationId());
        if (client == null) {
            return null;
        } else {
            Client clientUpdated = update.updateClient(client);
            clientsByEp.put(clientUpdated.getEndpoint(), clientUpdated);

            // notify listener
            for (ClientRegistryListener l : listeners) {
                l.updated(clientUpdated);
            }
            return clientUpdated;
        }
    }

    @Override
    public Client deregisterClient(String registrationId) {
        Validate.notNull(registrationId);

        LOG.debug("Deregistering client with registrationId: {}", registrationId);

        Client toBeUnregistered = findByRegistrationId(registrationId);
        if (toBeUnregistered == null) {
            return null;
        } else {
            Client unregistered = clientsByEp.remove(toBeUnregistered.getEndpoint());
            for (ClientRegistryListener l : listeners) {
                l.unregistered(unregistered);
            }
            LOG.debug("Deregistered client: {}", unregistered);
            return unregistered;
        }
    }

    @Override
    public Client findByRegistrationId(String id) {
        Client result = null;
        if (id != null) {
            for (Client client : clientsByEp.values()) {
                if (id.equals(client.getRegistrationId())) {
                    result = client;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * start the registration manager, will start regular cleanup of dead registrations.
     */
    @Override
    public void start() {
        // every 2 seconds clean the registration list
        // TODO re-consider clean-up interval: wouldn't 5 minutes do as well?
        schedExecutor.scheduleAtFixedRate(new Cleaner(), 2, 2, TimeUnit.SECONDS);
    }

    /**
     * Stop the underlying cleanup of the registrations.
     */
    @Override
    public void stop() {
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Clean up registration thread was interrupted.", e);
        }
    }

    private final ScheduledExecutorService schedExecutor = Executors.newScheduledThreadPool(1);

    private class Cleaner implements Runnable {

        @Override
        public void run() {
            for (Client client : clientsByEp.values()) {
                synchronized (client) {
                    if (!client.isAlive()) {
                        // force de-registration
                        deregisterClient(client.getRegistrationId());
                    }
                }
            }
        }
    }
}

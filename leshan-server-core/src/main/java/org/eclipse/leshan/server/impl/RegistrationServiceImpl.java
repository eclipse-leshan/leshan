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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationService;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link RegistrationService}
 */
public class RegistrationServiceImpl implements RegistrationService, Startable, Stoppable, ExpirationListener {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final List<RegistrationListener> listeners = new CopyOnWriteArrayList<>();

    private RegistrationStore store;

    public RegistrationServiceImpl(RegistrationStore store) {
        this.store = store;
        store.setExpirationListener(this);
    }

    @Override
    public void addListener(RegistrationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(RegistrationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<Client> getAllRegistrations() {
        return store.getAllRegistration();
    }

    @Override
    public Client getByEndpoint(String endpoint) {
        return store.getRegistrationByEndpoint(endpoint);
    }

    public boolean registerClient(Client client) {
        Validate.notNull(client);

        LOG.debug("Registering new client: {}", client);

        Deregistration previous = store.addRegistration(client);
        if (previous != null) {
            for (RegistrationListener l : listeners) {
                l.unregistered(previous.getRegistration());
            }
        }
        for (RegistrationListener l : listeners) {
            l.registered(client);
        }

        return true;
    }

    public Client updateClient(ClientUpdate update) {
        Validate.notNull(update);

        LOG.debug("Updating registration for client: {}", update);
        Client clientUpdated = store.updateRegistration(update);
        if (clientUpdated != null) {
            // notify listener
            for (RegistrationListener l : listeners) {
                l.updated(update, clientUpdated);
            }
            return clientUpdated;
        }
        return null;
    }

    public Client deregisterClient(String registrationId) {
        Validate.notNull(registrationId);

        LOG.debug("Deregistering client with registrationId: {}", registrationId);

        Deregistration unregistered = store.removeRegistration(registrationId);
        for (RegistrationListener l : listeners) {
            l.unregistered(unregistered.getRegistration());
        }
        LOG.debug("Deregistered client: {}", unregistered);
        return unregistered.getRegistration();
    }

    public RegistrationStore getStore() {
        return store;
    }

    @Override
    public Client getById(String id) {
        return store.getRegistration(id);
    }

    /**
     * start the registration manager, will start regular cleanup of dead registrations.
     */
    @Override
    public void start() {
        if (store instanceof Startable)
            ((Startable) store).start();
    }

    /**
     * Stop the underlying cleanup of the registrations.
     */
    @Override
    public void stop() {
        if (store instanceof Stoppable)
            ((Stoppable) store).stop();
    }

    @Override
    public void registrationExpired(Client registration, Collection<Observation> observation) {
        for (RegistrationListener l : listeners) {
            l.unregistered(registration);
        }
    }
}

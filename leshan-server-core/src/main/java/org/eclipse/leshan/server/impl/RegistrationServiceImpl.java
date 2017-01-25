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
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationService;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link RegistrationService}
 */
public class RegistrationServiceImpl implements RegistrationService, ExpirationListener {

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
    public Collection<Registration> getAllRegistrations() {
        return store.getAllRegistration();
    }

    @Override
    public Registration getByEndpoint(String endpoint) {
        return store.getRegistrationByEndpoint(endpoint);
    }

    @Override
    public Registration getById(String id) {
        return store.getRegistration(id);
    }

    @Override
    public void registrationExpired(Registration registration, Collection<Observation> observation) {
        for (RegistrationListener l : listeners) {
            l.unregistered(registration);
        }
    }

    public void fireRegistred(Registration registration) {
        for (RegistrationListener l : listeners) {
            l.registered(registration);
        }
    }

    public void fireUnregistered(Registration registration) {
        for (RegistrationListener l : listeners) {
            l.unregistered(registration);
        }
    }

    public void fireUpdated(RegistrationUpdate update, Registration registration) {
        for (RegistrationListener l : listeners) {
            l.updated(update, registration);
        }
    }

    public RegistrationStore getStore() {
        return store;
    }
}

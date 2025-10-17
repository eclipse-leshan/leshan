/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.registration;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.observation.Observation;

/**
 * An implementation of {@link RegistrationService}
 */
public class RegistrationServiceImpl implements RegistrationService, ExpirationListener {

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
    public Iterator<Registration> getAllRegistrations() {
        return store.getAllRegistrations();
    }

    @Override
    public Registration getByEndpoint(String endpoint) {
        return store.getRegistrationByEndpoint(endpoint);
    }

    @Override
    public IRegistration getById(String id) {
        return store.getRegistration(id);
    }

    @Override
    public void registrationExpired(IRegistration registration, Collection<Observation> observations) {
        for (RegistrationListener l : listeners) {
            l.unregistered(registration, observations, true, null);
        }
    }

    public void fireRegistered(IRegistration registration, IRegistration previousReg,
            Collection<Observation> previousObservations) {
        for (RegistrationListener l : listeners) {
            l.registered(registration, previousReg, previousObservations);
        }
    }

    public void fireUnregistered(IRegistration registration, Collection<Observation> observations, IRegistration newReg) {
        for (RegistrationListener l : listeners) {
            l.unregistered(registration, observations, false, newReg);
        }
    }

    public void fireUpdated(RegistrationUpdate update, IRegistration updatedRegistration,
            IRegistration previousRegistration) {
        for (RegistrationListener l : listeners) {
            l.updated(update, updatedRegistration, previousRegistration);
        }
    }

    public RegistrationStore getStore() {
        return store;
    }
}

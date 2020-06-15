/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.Collection;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;

/**
 * Listener that controls the state of the client (awake/sleeping) It is in charge of sending all the queued messages
 * when the client is awake (has sent an update message), and controlling the time the client is awake before going to
 * sleep.
 */

public class PresenceStateListener implements RegistrationListener, ObservationListener {

    PresenceServiceImpl presenceService;

    public PresenceStateListener(PresenceServiceImpl presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    public void registered(Registration reg, Registration previousReg, Collection<Observation> previousObsersations) {
        if (reg.usesQueueMode()) {
            presenceService.setAwake(reg);
        }
    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedRegistration,
            Registration previousRegistration) {
        if (updatedRegistration.usesQueueMode()) {
            presenceService.setAwake(updatedRegistration);
        }

    }

    @Override
    public void unregistered(Registration reg, Collection<Observation> observations, boolean expired,
            Registration newReg) {
        presenceService.stopPresenceTracking(reg);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 1.1
     */
    @Override
    public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
        presenceService.setAwake(registration);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 1.1
     */
    @Override
    public void newObservation(Observation observation, Registration registration) {
    }

    /**
     * {@inheritDoc}
     * 
     * @since 1.1
     */
    @Override
    public void cancelled(Observation observation) {
    }

    /**
     * {@inheritDoc}
     * 
     * @since 1.1
     */
    @Override
    public void onError(Observation observation, Registration registration, Exception error) {
    }
}

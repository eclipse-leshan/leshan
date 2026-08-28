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

import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationAddition;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.UpdatedRegistration;

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
    public void registered(RegistrationAddition registrationAddition) {
        Registration reg = registrationAddition.getNewRegistration();
        if (reg.usesQueueMode()) {
            presenceService.setAwake(reg);
        }

    }

    @Override
    public void updated(UpdatedRegistration modification) {
        Registration updatedRegistration = modification.getUpdatedRegistration();
        if (updatedRegistration.usesQueueMode()) {
            presenceService.setAwake(updatedRegistration);
        }

    }

    @Override
    public void unregistered(Deregistration deregistration, boolean expired, Registration newReg) {
        presenceService.stopPresenceTracking(deregistration.getRegistration());

    }

    /**
     * {@inheritDoc}
     *
     * @since 1.1
     */
    @Override
    public void onResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
        presenceService.setAwake(registration);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public void onResponse(CompositeObservation observation, Registration registration,
            ObserveCompositeResponse response) {
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

/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.observation;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.registration.RegistrationStore;

public class LwM2mObservationStore implements ObservationStore {

    private final RegistrationStore registrationStore;
    private final LwM2mNotificationReceiver notificationListener;
    private final ObservationSerDes observationSerDes;

    public LwM2mObservationStore(RegistrationStore registrationStore, LwM2mNotificationReceiver notificationListener,
            ObservationSerDes observationSerDes) {
        this.registrationStore = registrationStore;
        this.notificationListener = notificationListener;
        this.observationSerDes = observationSerDes;
    }

    @Override
    public Observation putIfAbsent(Token token, Observation obs) {
        org.eclipse.leshan.core.observation.Observation lwm2mObservation = buildLwM2mObservation(obs);
        Collection<org.eclipse.leshan.core.observation.Observation> removed = registrationStore
                .addObservation(lwm2mObservation.getRegistrationId(), lwm2mObservation, true);

        Observation previousObservation = null;
        if (removed != null && !removed.isEmpty()) {
            for (org.eclipse.leshan.core.observation.Observation observation : removed) {
                if (Arrays.equals(observation.getId().getBytes(), token.getBytes())) {
                    previousObservation = buildCoapObservation(observation);
                    break;
                }
            }
        }
        for (org.eclipse.leshan.core.observation.Observation observation : removed) {
            notificationListener.cancelled(observation);
        }
        return previousObservation;
    }

    @Override
    public Observation put(Token token, Observation obs) {
        org.eclipse.leshan.core.observation.Observation lwm2mObservation = buildLwM2mObservation(obs);
        Collection<org.eclipse.leshan.core.observation.Observation> removed = registrationStore
                .addObservation(lwm2mObservation.getRegistrationId(), buildLwM2mObservation(obs), false);

        Observation previousObservation = null;
        if (removed != null && !removed.isEmpty()) {
            for (org.eclipse.leshan.core.observation.Observation observation : removed) {
                if (Arrays.equals(observation.getId().getBytes(), token.getBytes())) {
                    previousObservation = buildCoapObservation(observation);
                    break;
                }
            }
        }
        for (org.eclipse.leshan.core.observation.Observation observation : removed) {
            notificationListener.cancelled(observation);
        }
        return previousObservation;
    }

    @Override
    public void remove(Token token) {
        org.eclipse.leshan.core.observation.Observation removedObservation = registrationStore.removeObservation(null,
                new ObservationIdentifier(token.getBytes()));
        notificationListener.cancelled(removedObservation);
    }

    @Override
    public Observation get(Token token) {
        org.eclipse.leshan.core.observation.Observation observation = registrationStore.getObservation(null,
                new ObservationIdentifier(token.getBytes()));
        if (observation == null) {
            return null;
        } else {
            return buildCoapObservation(observation);
        }
    }

    @Override
    public void setContext(Token token, EndpointContext endpointContext) {
        // In Leshan we always set context when we send the request, so this should not be needed to implement this.
    }

    @Override
    public void setExecutor(ScheduledExecutorService executor) {
        // registrationStore has its own executor.
    }

    @Override
    public void start() {
        // Internal RegistrationStore is started by Leshan.
    }

    @Override
    public void stop() {
        // Internal RegistrationStore is stopped by Leshan.
    }

    private org.eclipse.leshan.core.observation.Observation buildLwM2mObservation(Observation observation) {
        String obs = observationSerDes.serialize(observation);
        return ObserveUtil.createLwM2mObservation(observation, obs);
    }

    private Observation buildCoapObservation(org.eclipse.leshan.core.observation.Observation observation) {
        String serializedObservation = ObserveUtil.extractSerializedObservation(observation);
        if (serializedObservation == null)
            return null;

        return observationSerDes.deserialize(serializedObservation);
    }
}

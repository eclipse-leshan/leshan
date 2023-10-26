/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.observation;

import java.util.Collection;
import java.util.Optional;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.peer.LwM2mIdentity;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.observe.ObservationsStore;

public class LwM2mObservationsStore implements ObservationsStore {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mObservationsStore.class);

    private final RegistrationStore store;
    private final LwM2mNotificationReceiver notificationReceiver;
    private final IdentityHandler identityHandler;

    public LwM2mObservationsStore(RegistrationStore store, LwM2mNotificationReceiver notificationReceiver,
            IdentityHandler identityHandler) {
        this.store = store;
        this.notificationReceiver = notificationReceiver;
        this.identityHandler = identityHandler;
    }

    @Override
    public void add(CoapRequest obsReq) {
        Observation observation = obsReq.getTransContext(LwM2mKeys.LESHAN_OBSERVATION);
        if (observation == null) {
            String errMessage = "missing LESHAN_OBSERVATION key in coap request transport context";
            LOG.warn(errMessage);
            throw new IllegalStateException(errMessage);
        }

        Registration registration = obsReq.getTransContext(LwM2mKeys.LESHAN_REGISTRATION);
        if (registration == null) {
            String errMessage = "missing LESHAN_REGISTRATION key in coap request transport context";
            LOG.warn(errMessage);
            throw new IllegalStateException(errMessage);
        }

        Collection<Observation> removed = null;
        try {
            LOG.debug("Add new Observation for registration {} : {} ", registration.getId(), observation);
            removed = store.addObservation(registration.getId(), observation, false);
        } catch (Exception e) {
            LOG.warn("Unable to add observation {}", observation, e);
            throw e;
        }

        // Manage cancellation
        if (removed != null && !removed.isEmpty()) {
            for (Observation obsRemoved : removed) {
                notificationReceiver.cancelled(obsRemoved);
            }
        }
        notificationReceiver.newObservation(observation, registration);
    }

    @Override
    public Optional<String> resolveUriPath(SeparateResponse obs) {

        // Try to find observation for given token
        ObservationIdentifier observationIdentifier = new ObservationIdentifier(obs.getToken().getBytes());
        LOG.debug("Search observation for  identifier {} ", observationIdentifier);
        Observation observation = store.getObservation(observationIdentifier);
        // TODO should we use PeerIdentity in ObservationIdentifier.
        if (observation == null) {
            LOG.debug("Observation not found");
            return Optional.empty();
        }
        Registration registration = store.getRegistration(observation.getRegistrationId());
        if (registration == null) {
            LOG.debug("No registration for observation {}, {}", observationIdentifier, observation);
            return Optional.empty();
        }
        // extract identity from notification
        LwM2mPeer notificationIdentity = identityHandler.getIdentity(obs);
        LwM2mIdentity registrationIdentity = registration.getClientTransportData().getIdentity();

        if (!(registrationIdentity.equals(notificationIdentity.getIdentity()))) {
            LOG.debug("Notification reveiced from peer with unexpected identity : should be {} but was {}",
                    registrationIdentity, notificationIdentity.getIdentity());
            return Optional.empty();
        }

        return ObservationUtil.getPath(observation);
    }

    @Override
    public void remove(SeparateResponse obs) {
        // Try to find observation for given token
        ObservationIdentifier observationIdentifier = new ObservationIdentifier(obs.getToken().getBytes());
        Observation observation = store.getObservation(observationIdentifier);

        if (observation != null) {
            // try to remove observation
            Observation removedObservation = store.removeObservation(observation.getRegistrationId(),
                    observationIdentifier);
            if (removedObservation != null) {
                notificationReceiver.cancelled(removedObservation);
            }
        }
    }
}

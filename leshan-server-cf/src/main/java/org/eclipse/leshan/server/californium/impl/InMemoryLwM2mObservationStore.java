/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange.KeyToken;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.core.observe.InMemoryObservationStore;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.elements.CorrelationContext;
import org.eclipse.californium.elements.RawData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in-memory {@link LwM2mObservationStore} implementation.
 * 
 * Mainly inspired by the Californium {@link InMemoryObservationStore}.
 */
public class InMemoryLwM2mObservationStore implements LwM2mObservationStore {

    private final Logger LOG = LoggerFactory.getLogger(InMemoryLwM2mObservationStore.class);

    private static final DataSerializer serializer = new UdpDataSerializer();

    private Map<KeyToken, Observation> byToken = new HashMap<>();
    private Map<String, List<KeyToken>> byRegId = new HashMap<>();

    /* lock for udpate */
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(Observation obs) {
        if (obs != null) {
            try {
                lock.writeLock().lock();

                validateObservation(obs);

                String registrationId = getRegistrationId(obs);
                KeyToken token = new KeyToken(obs.getRequest().getToken());
                Observation previousObservation = byToken.put(token, obs);
                if (!byRegId.containsKey(registrationId)) {
                    byRegId.put(registrationId, new ArrayList<KeyToken>());
                }
                byRegId.get(registrationId).add(token);

                // log any collisions
                if (previousObservation != null) {
                    LOG.warn(
                            "Token collision ? observation from request [{}] will be replaced by observation from request [{}] ",
                            previousObservation.getRequest(),
                            obs.getRequest());
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public Observation get(byte[] token) {
        return get(new KeyToken(token));
    }

    private Observation get(KeyToken token) {
        try {
            lock.readLock().lock();

            Observation obs = byToken.get(token);
            if (obs != null) {
                RawData serialize = serializer.serializeRequest(obs.getRequest(), null);
                DataParser parser = new UdpDataParser();
                Request newRequest = (Request) parser.parseMessage(serialize);
                newRequest.setUserContext(obs.getRequest().getUserContext());
                return new Observation(newRequest, obs.getContext());
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Observation> getByRegistrationId(String registrationId) {
        try {
            lock.readLock().lock();

            Collection<Observation> result = new ArrayList<>();
            List<KeyToken> tokens = byRegId.get(registrationId);
            if (tokens != null) {
                for (KeyToken token : tokens) {
                    Observation obs = get(token);
                    if (obs != null) {
                        result.add(obs);
                    }
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void remove(byte[] token) {
        try {
            lock.writeLock().lock();

            KeyToken kToken = new KeyToken(token);
            Observation removed = byToken.remove(kToken);

            if (removed != null) {
                String registrationId = getRegistrationId(removed);
                List<KeyToken> tokens = byRegId.get(registrationId);
                tokens.remove(kToken);
                if (tokens.isEmpty()) {
                    byRegId.remove(registrationId);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Collection<Observation> removeAll(String registrationId) {
        try {
            lock.writeLock().lock();

            Collection<Observation> removed = new ArrayList<>();
            List<KeyToken> tokens = byRegId.get(registrationId);
            if (tokens != null) {
                for (KeyToken token : tokens) {
                    Observation observationRemoved = byToken.remove(token);
                    if (observationRemoved != null) {
                        removed.add(observationRemoved);
                    }
                }
            }
            byRegId.remove(registrationId);
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void setContext(byte[] token, CorrelationContext ctx) {
        try {
            lock.writeLock().lock();

            Observation obs = byToken.get(new KeyToken(token));
            if (obs != null) {
                byToken.put(new KeyToken(token), new Observation(obs.getRequest(), ctx));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* Retrieve the registrationId from the request context */
    private String getRegistrationId(Observation observation) {
        return observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID);
    }

    private void validateObservation(Observation observation) {
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_REGID))
            throw new IllegalStateException("missing registrationId info in the request context");
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_LWM2M_PATH))
            throw new IllegalStateException("missing lwm2m path info in the request context");
    }

}

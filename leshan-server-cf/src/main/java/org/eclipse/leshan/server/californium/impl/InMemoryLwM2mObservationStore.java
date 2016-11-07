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
 *     Bosch Software Innovations - add TCP support, retry on MessageFormatException
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.MessageFormatException;
import org.eclipse.californium.core.coap.Request;
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

    private Map<Key, Observation> byToken = new HashMap<>();
    private Map<String, List<Key>> byRegId = new HashMap<>();

    /* lock for udpate */
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(Observation obs) {
        if (obs != null) {
            try {
                lock.writeLock().lock();

                validateObservation(obs);

                String registrationId = getRegistrationId(obs);
                Key token = Key.fromToken(obs.getRequest().getToken());
                Observation previousObservation = byToken.put(token, obs);
                if (!byRegId.containsKey(registrationId)) {
                    byRegId.put(registrationId, new ArrayList<Key>());
                }
                byRegId.get(registrationId).add(token);

                // log any collisions
                if (previousObservation != null) {
                    LOG.warn(
                            "Token collision ? observation from request [{}] will be replaced by observation from request [{}] ",
                            previousObservation.getRequest(), obs.getRequest());
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public Observation get(byte[] token) {
        return get(Key.fromToken(token));
    }

    private Observation get(Key token) {
        try {
            lock.readLock().lock();

            Observation obs = byToken.get(token);
            if (obs != null) {
                Request request = obs.getRequest();
                RawData serialize = serializer.serializeRequest(request, null);
                DataParser parser = new UdpDataParser();
                Request newRequest;
                try {
                    newRequest = (Request) parser.parseMessage(serialize);
                } catch (MessageFormatException ex) {
                    /** second chance, may be TCP serializer was used for this message */
                    request.setBytes(null);
                    serialize = serializer.serializeRequest(request, null);
                    newRequest = (Request) parser.parseMessage(serialize);
                }
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
            List<Key> tokens = byRegId.get(registrationId);
            if (tokens != null) {
                for (Key token : tokens) {
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

            Key kToken = Key.fromToken(token);
            Observation removed = byToken.remove(kToken);

            if (removed != null) {
                String registrationId = getRegistrationId(removed);
                List<Key> tokens = byRegId.get(registrationId);
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
            List<Key> tokens = byRegId.get(registrationId);
            if (tokens != null) {
                for (Key token : tokens) {
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

            Observation obs = byToken.get(Key.fromToken(token));
            if (obs != null) {
                byToken.put(Key.fromToken(token), new Observation(obs.getRequest(), ctx));
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

    private static class Key {
        private final byte[] token;

        private Key(final byte[] token) {
            this.token = token;
        }

        private static Key fromToken(byte[] token) {
            return new Key(token);
        }

        @Override
        public String toString() {
            return Utils.toHexString(token);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(token);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (!Arrays.equals(token, other.token))
                return false;
            return true;
        }
    }

}

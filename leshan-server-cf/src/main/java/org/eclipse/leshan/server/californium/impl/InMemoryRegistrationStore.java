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

import static org.eclipse.leshan.server.californium.impl.CoapRequestBuilder.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange.KeyToken;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.elements.CorrelationContext;
import org.eclipse.californium.elements.RawData;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryRegistrationStore implements CaliforniumRegistrationStore {
    private final Logger LOG = LoggerFactory.getLogger(InMemoryRegistrationStore.class);

    private final Map<String /* end-point */, Client> clientsByEp = new ConcurrentHashMap<>();

    private static final DataSerializer serializer = new UdpDataSerializer();
    private Map<KeyToken, org.eclipse.californium.core.observe.Observation> byToken = new HashMap<>();
    private Map<String, List<KeyToken>> byRegId = new HashMap<>();
    /* lock for udpate */
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public Client addRegistration(Client registration) {
        Client registrationRemoved = clientsByEp.put(registration.getEndpoint(), registration);
        if (registrationRemoved != null)
            removeAll(registrationRemoved.getRegistrationId());
        return registrationRemoved;
    }

    @Override
    public Client updateRegistration(ClientUpdate update) {
        Client client = getRegistration(update.getRegistrationId());
        if (client == null) {
            return null;
        } else {
            Client registrationUpdated = update.updateClient(client);
            clientsByEp.put(registrationUpdated.getEndpoint(), registrationUpdated);
            return registrationUpdated;
        }
    }

    @Override
    public Client getRegistration(String registrationId) {
        Client result = null;
        if (registrationId != null) {
            for (Client client : clientsByEp.values()) {
                if (registrationId.equals(client.getRegistrationId())) {
                    result = client;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public Client getRegistrationByEndpoint(String endpoint) {
        return clientsByEp.get(endpoint);
    }

    @Override
    public Collection<Client> getRegistrationByAdress(InetSocketAddress address) {
        // TODO needed to remove getAllRegistration()
        return null;
    }

    @Override
    public Collection<Client> getAllRegistration() {
        return clientsByEp.values();
    }

    @Override
    public Client removeRegistration(String registrationId) {
        Client registration = getRegistration(registrationId);
        if (registration != null) {
            removeAll(registration.getRegistrationId());
            return clientsByEp.remove(registration.getEndpoint());
        }
        return null;
    }

    @Override
    public Observation addObservation(String registrationId, Observation observation) {
        // not used observation was added by Californium
        return null;
    }

    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
        Observation observation = build(get(observationId));
        if (observation != null && registrationId.equals(observation.getRegistrationId())) {
            remove(observationId);
            return observation;
        }
        return null;
    }

    @Override
    public Observation getObservation(String registrationId, byte[] ObservationId) {
        return build(get(ObservationId));
    }

    @Override
    public Collection<Observation> getObservations(String registrationId) {
        return build(getByRegistrationId(registrationId));
    }

    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        return build(removeAll(registrationId));
    }

    private Collection<Observation> build(Collection<org.eclipse.californium.core.observe.Observation> cfObss) {
        List<Observation> obs = new ArrayList<>();

        for (org.eclipse.californium.core.observe.Observation cfObs : cfObss) {
            obs.add(build(cfObs));
        }
        return obs;
    }

    private Observation build(org.eclipse.californium.core.observe.Observation cfObs) {
        if (cfObs == null)
            return null;

        String regId = null;
        String lwm2mPath = null;
        Map<String, String> context = null;

        for (Entry<String, String> ctx : cfObs.getRequest().getUserContext().entrySet()) {
            switch (ctx.getKey()) {
            case CTX_REGID:
                regId = ctx.getValue();
                break;
            case CTX_LWM2M_PATH:
                lwm2mPath = ctx.getValue();
                break;
            default:
                if (context == null) {
                    context = new HashMap<>();
                }
                context.put(ctx.getKey(), ctx.getValue());
            }
        }
        return new Observation(cfObs.getRequest().getToken(), regId, new LwM2mPath(lwm2mPath), context);
    }

    @Override
    public void add(org.eclipse.californium.core.observe.Observation obs) {
        if (obs != null) {
            try {
                lock.writeLock().lock();

                validateObservation(obs);

                String registrationId = getRegistrationId(obs);
                KeyToken token = new KeyToken(obs.getRequest().getToken());
                org.eclipse.californium.core.observe.Observation previousObservation = byToken.put(token, obs);
                if (!byRegId.containsKey(registrationId)) {
                    byRegId.put(registrationId, new ArrayList<KeyToken>());
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
    public org.eclipse.californium.core.observe.Observation get(byte[] token) {
        return get(new KeyToken(token));
    }

    private org.eclipse.californium.core.observe.Observation get(KeyToken token) {
        try {
            lock.readLock().lock();

            org.eclipse.californium.core.observe.Observation obs = byToken.get(token);
            if (obs != null) {
                RawData serialize = serializer.serializeRequest(obs.getRequest(), null);
                DataParser parser = new UdpDataParser();
                Request newRequest = (Request) parser.parseMessage(serialize);
                newRequest.setUserContext(obs.getRequest().getUserContext());
                return new org.eclipse.californium.core.observe.Observation(newRequest, obs.getContext());
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<org.eclipse.californium.core.observe.Observation> getByRegistrationId(String registrationId) {
        try {
            lock.readLock().lock();

            Collection<org.eclipse.californium.core.observe.Observation> result = new ArrayList<>();
            List<KeyToken> tokens = byRegId.get(registrationId);
            if (tokens != null) {
                for (KeyToken token : tokens) {
                    org.eclipse.californium.core.observe.Observation obs = get(token);
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
            org.eclipse.californium.core.observe.Observation removed = byToken.remove(kToken);

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

    public Collection<org.eclipse.californium.core.observe.Observation> removeAll(String registrationId) {
        try {
            lock.writeLock().lock();

            Collection<org.eclipse.californium.core.observe.Observation> removed = new ArrayList<>();
            List<KeyToken> tokens = byRegId.get(registrationId);
            if (tokens != null) {
                for (KeyToken token : tokens) {
                    org.eclipse.californium.core.observe.Observation observationRemoved = byToken.remove(token);
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

            org.eclipse.californium.core.observe.Observation obs = byToken.get(new KeyToken(token));
            if (obs != null) {
                byToken.put(new KeyToken(token),
                        new org.eclipse.californium.core.observe.Observation(obs.getRequest(), ctx));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* Retrieve the registrationId from the request context */
    private String getRegistrationId(org.eclipse.californium.core.observe.Observation observation) {
        return observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID);
    }

    private void validateObservation(org.eclipse.californium.core.observe.Observation observation) {
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_REGID))
            throw new IllegalStateException("missing registrationId info in the request context");
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_LWM2M_PATH))
            throw new IllegalStateException("missing lwm2m path info in the request context");
        if (getRegistration(observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID)) == null) {
            throw new IllegalStateException("no registration for this Id");
        }
    }
}

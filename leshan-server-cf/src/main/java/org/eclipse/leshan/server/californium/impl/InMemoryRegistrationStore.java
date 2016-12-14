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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in memory store for registration and observation.
 */
public class InMemoryRegistrationStore implements CaliforniumRegistrationStore, Startable, Stoppable {
    private final Logger LOG = LoggerFactory.getLogger(InMemoryRegistrationStore.class);

    // Data structure
    private final Map<String /* end-point */, Registration> regsByEp = new HashMap<>();
    private Map<KeyToken, org.eclipse.californium.core.observe.Observation> obsByToken = new HashMap<>();
    private Map<String, List<KeyToken>> tokensByRegId = new HashMap<>();

    // Request serializer to persist observation
    private static final DataSerializer serializer = new UdpDataSerializer();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    /* *************** Leshan Registration API **************** */

    @Override
    public Deregistration addRegistration(Registration registration) {
        try {
            lock.writeLock().lock();

            Registration registrationRemoved = regsByEp.put(registration.getEndpoint(), registration);
            if (registrationRemoved != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registrationRemoved.getId());
                return new Deregistration(registrationRemoved, observationsRemoved);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    @Override
    public Registration updateRegistration(RegistrationUpdate update) {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistration(update.getRegistrationId());
            if (registration == null) {
                return null;
            } else {
                Registration updatedRegistration = update.update(registration);
                regsByEp.put(updatedRegistration.getEndpoint(), updatedRegistration);
                return updatedRegistration;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Registration getRegistration(String registrationId) {
        try {
            lock.readLock().lock();

            if (registrationId != null) {
                for (Registration registration : regsByEp.values()) {
                    if (registrationId.equals(registration.getId())) {
                        return registration;
                    }
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistrationByEndpoint(String endpoint) {
        try {
            lock.readLock().lock();
            return regsByEp.get(endpoint);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Registration> getRegistrationByAdress(InetSocketAddress address) {
        // TODO needed to remove getAllRegistration()
        return null;
    }

    @Override
    public Collection<Registration> getAllRegistration() {
        try {
            lock.readLock().lock();
            return Collections.unmodifiableCollection(regsByEp.values());
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public Deregistration removeRegistration(String registrationId) {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistration(registrationId);
            if (registration != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registration.getId());
                regsByEp.remove(registration.getEndpoint());
                return new Deregistration(registration, observationsRemoved);
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Leshan Observation API **************** */

    @Override
    public Observation addObservation(String registrationId, Observation observation) {
        // not used observation was added by Californium
        return null;
    }

    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
        try {
            lock.writeLock().lock();

            Observation observation = build(unsafeGetObservation(new KeyToken(observationId)));
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                // TODO remove API should returns the observation removed
                unsafeRemoveObservation(observationId);
                return observation;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Observation getObservation(String registrationId, byte[] observationId) {
        try {
            lock.readLock().lock();
            Observation observation = build(unsafeGetObservation(new KeyToken(observationId)));
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                return observation;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Observation> getObservations(String registrationId) {
        try {
            lock.readLock().lock();

            Collection<Observation> result = new ArrayList<>();
            List<KeyToken> tokens = tokensByRegId.get(registrationId);
            if (tokens != null) {
                for (KeyToken token : tokens) {
                    Observation obs = build(unsafeGetObservation(token));
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
    public Collection<Observation> removeObservations(String registrationId) {
        try {
            lock.writeLock().lock();
            return unsafeRemoveAllObservations(registrationId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Californium ObservationStore API **************** */

    @Override
    public void add(org.eclipse.californium.core.observe.Observation obs) {
        if (obs != null) {
            try {
                lock.writeLock().lock();

                validateObservation(obs);

                String registrationId = extractRegistrationId(obs);
                KeyToken token = new KeyToken(obs.getRequest().getToken());
                org.eclipse.californium.core.observe.Observation previousObservation = obsByToken.put(token, obs);
                if (!tokensByRegId.containsKey(registrationId)) {
                    tokensByRegId.put(registrationId, new ArrayList<KeyToken>());
                }
                tokensByRegId.get(registrationId).add(token);

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
        try {
            lock.readLock().lock();
            return unsafeGetObservation(new KeyToken(token));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setContext(byte[] token, CorrelationContext ctx) {
        try {
            lock.writeLock().lock();

            org.eclipse.californium.core.observe.Observation obs = obsByToken.get(new KeyToken(token));
            if (obs != null) {
                obsByToken.put(new KeyToken(token),
                        new org.eclipse.californium.core.observe.Observation(obs.getRequest(), ctx));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(byte[] token) {
        try {
            lock.writeLock().lock();
            unsafeRemoveObservation(token);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Observation utility functions **************** */

    private org.eclipse.californium.core.observe.Observation unsafeGetObservation(KeyToken token) {
        org.eclipse.californium.core.observe.Observation obs = obsByToken.get(token);
            if (obs != null) {
                RawData serialize = serializer.serializeRequest(obs.getRequest(), null);
                DataParser parser = new UdpDataParser();
                Request newRequest = (Request) parser.parseMessage(serialize);
                newRequest.setUserContext(obs.getRequest().getUserContext());
                return new org.eclipse.californium.core.observe.Observation(newRequest, obs.getContext());
            }
            return null;
    }

    private void unsafeRemoveObservation(byte[] observationId) {
        KeyToken kToken = new KeyToken(observationId);
        org.eclipse.californium.core.observe.Observation removed = obsByToken.remove(kToken);

        if (removed != null) {
            String registrationId = extractRegistrationId(removed);
            List<KeyToken> tokens = tokensByRegId.get(registrationId);
            tokens.remove(kToken);
            if (tokens.isEmpty()) {
                tokensByRegId.remove(registrationId);
            }
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        List<KeyToken> tokens = tokensByRegId.get(registrationId);
        if (tokens != null) {
            for (KeyToken token : tokens) {
                Observation observationRemoved = build(obsByToken.remove(token));
                if (observationRemoved != null) {
                    removed.add(observationRemoved);
                }
            }
        }
        tokensByRegId.remove(registrationId);
        return removed;
    }

    /* Retrieve the registrationId from the request context */
    private String extractRegistrationId(org.eclipse.californium.core.observe.Observation observation) {
        return observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID);
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

    private void validateObservation(org.eclipse.californium.core.observe.Observation observation) {
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_REGID))
            throw new IllegalStateException("missing registrationId info in the request context");
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_LWM2M_PATH))
            throw new IllegalStateException("missing lwm2m path info in the request context");
        if (getRegistration(observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID)) == null) {
            throw new IllegalStateException("no registration for this Id");
        }
    }

    /* *************** Expiration handling **************** */

    @Override
    public void setExpirationListener(ExpirationListener listener) {
        this.expirationListener = listener;
    }

    /**
     * start the registration store, will start regular cleanup of dead registrations.
     */
    @Override
    public void start() {
        // every 2 seconds clean the registration list
        // TODO re-consider clean-up interval: wouldn't 5 minutes do as well?
        schedExecutor.scheduleAtFixedRate(new Cleaner(), 2, 2, TimeUnit.SECONDS);
    }

    /**
     * Stop the underlying cleanup of the registrations.
     */
    @Override
    public void stop() {
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Clean up registration thread was interrupted.", e);
        }
    }

    private final ScheduledExecutorService schedExecutor = Executors.newScheduledThreadPool(1);

    private class Cleaner implements Runnable {

        @Override
        public void run() {
            try {
                for (Registration reg : new ArrayList<Registration>(getAllRegistration())) {
                    if (!reg.isAlive()) {
                        // force de-registration
                        Deregistration removedRegistration = removeRegistration(reg.getId());
                        expirationListener.registrationExpired(removedRegistration.getRegistration(),
                                removedRegistration.getObservations());
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unexcepted Exception while registration cleaning", e);
            }
        }
    }
}

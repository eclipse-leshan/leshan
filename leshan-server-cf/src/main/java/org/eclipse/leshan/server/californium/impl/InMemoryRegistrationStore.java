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
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace serialize/parse in
 *                                                     unsafeGetObservation() with
 *                                                     ObservationUtil.shallowClone.
 *                                                     Reuse already created Key in
 *                                                     setContext().
 *     Achim Kraus (Bosch Software Innovations GmbH) - rename CorrelationContext to
 *                                                     EndpointContext
 *     Achim Kraus (Bosch Software Innovations GmbH) - update to modified 
 *                                                     ObservationStore API
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.ObservationUtil;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.ObserveUtil;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in memory store for registration and observation.
 */
public class InMemoryRegistrationStore implements CaliforniumRegistrationStore, Startable, Stoppable {
    private final Logger LOG = LoggerFactory.getLogger(InMemoryRegistrationStore.class);

    // Data structure
    private final Map<String /* end-point */, Registration> regsByEp = new HashMap<>();
    private Map<Token, org.eclipse.californium.core.observe.Observation> obsByToken = new HashMap<>();
    private Map<String, List<Token>> tokensByRegId = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    private final ScheduledExecutorService schedExecutor;
    private final long cleanPeriod; // in seconds

    public InMemoryRegistrationStore() {
        this(2); // default clean period : 2s
    }

    public InMemoryRegistrationStore(long cleanPeriodInSec) {
        this(Executors.newScheduledThreadPool(1,
                new NamedThreadFactory(String.format("InMemoryRegistrationStore Cleaner (%ds)", cleanPeriodInSec))),
                cleanPeriodInSec);
    }

    public InMemoryRegistrationStore(ScheduledExecutorService schedExecutor, long cleanPeriodInSec) {
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
    }

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
    public UpdatedRegistration updateRegistration(RegistrationUpdate update) {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistration(update.getRegistrationId());
            if (registration == null) {
                return null;
            } else {
                Registration updatedRegistration = update.update(registration);
                regsByEp.put(updatedRegistration.getEndpoint(), updatedRegistration);
                return new UpdatedRegistration(registration, updatedRegistration);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Registration getRegistration(String registrationId) {
        try {
            lock.readLock().lock();
            // TODO we should create an index instead of iterate all over the collection
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
    public Registration getRegistrationByAdress(InetSocketAddress address) {
        try {
            lock.readLock().lock();
            // TODO we should create an index instead of iterate all over the collection
            if (address != null) {
                for (Registration r : regsByEp.values()) {
                    if (address.getPort() == r.getPort() && address.getAddress().equals(r.getAddress())) {
                        return r;
                    }
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Iterator<Registration> getAllRegistrations() {
        try {
            lock.readLock().lock();
            return new ArrayList<>(regsByEp.values()).iterator();
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

    /*
     * The observation is not persisted here, it is done by the Californium layer (in the implementation of the
     * org.eclipse.californium.core.observe.ObservationStore#add method)
     */
    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation) {

        List<Observation> removed = new ArrayList<>();

        try {
            lock.writeLock().lock();
            // cancel existing observations for the same path and registration id.
            for (Observation obs : unsafeGetObservations(registrationId)) {
                if (observation.getPath().equals(obs.getPath()) && !Arrays.equals(observation.getId(), obs.getId())) {
                    unsafeRemoveObservation(new Token(obs.getId()));
                    removed.add(obs);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        return removed;
    }

    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
        try {
            lock.writeLock().lock();
            Token token = new Token(observationId);
            Observation observation = build(unsafeGetObservation(token));
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                unsafeRemoveObservation(token);
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
            Observation observation = build(unsafeGetObservation(new Token(observationId)));
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
            return unsafeGetObservations(registrationId);
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
    public org.eclipse.californium.core.observe.Observation putIfAbsent(Token token, org.eclipse.californium.core.observe.Observation obs) {
        return add(token, obs, true);
    }

    @Override
    public org.eclipse.californium.core.observe.Observation put(Token token, org.eclipse.californium.core.observe.Observation obs) {
        return add(token, obs, false);
    }

    private org.eclipse.californium.core.observe.Observation add(Token token, org.eclipse.californium.core.observe.Observation obs, boolean ifAbsent) {
        org.eclipse.californium.core.observe.Observation previousObservation = null;
        if (obs != null) {
            try {
                lock.writeLock().lock();

                validateObservation(obs);

                String registrationId = ObserveUtil.extractRegistrationId(obs);
                if (ifAbsent) {
                    previousObservation = obsByToken.putIfAbsent(token, obs);
                } else {
                    previousObservation = obsByToken.put(token, obs);
                }
                if (!tokensByRegId.containsKey(registrationId)) {
                    tokensByRegId.put(registrationId, new ArrayList<Token>());
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
        return previousObservation;
    }

    @Override
    public org.eclipse.californium.core.observe.Observation get(Token token) {
        try {
            lock.readLock().lock();
            return unsafeGetObservation(token);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setContext(Token token, EndpointContext ctx) {
        try {
            lock.writeLock().lock();
            org.eclipse.californium.core.observe.Observation obs = obsByToken.get(token);
            if (obs != null) {
                obsByToken.put(token, new org.eclipse.californium.core.observe.Observation(obs.getRequest(), ctx));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(Token token) {
        try {
            lock.writeLock().lock();
            unsafeRemoveObservation(token);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Observation utility functions **************** */

    private org.eclipse.californium.core.observe.Observation unsafeGetObservation(Token token) {
        org.eclipse.californium.core.observe.Observation obs = obsByToken.get(token);
        return ObservationUtil.shallowClone(obs);
    }

    private void unsafeRemoveObservation(Token observationId) {
        org.eclipse.californium.core.observe.Observation removed = obsByToken.remove(observationId);

        if (removed != null) {
            String registrationId = ObserveUtil.extractRegistrationId(removed);
            List<Token> tokens = tokensByRegId.get(registrationId);
            tokens.remove(observationId);
            if (tokens.isEmpty()) {
                tokensByRegId.remove(registrationId);
            }
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        List<Token> tokens = tokensByRegId.get(registrationId);
        if (tokens != null) {
            for (Token token : tokens) {
                Observation observationRemoved = build(obsByToken.remove(token));
                if (observationRemoved != null) {
                    removed.add(observationRemoved);
                }
            }
        }
        tokensByRegId.remove(registrationId);
        return removed;
    }

    private Collection<Observation> unsafeGetObservations(String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        List<Token> tokens = tokensByRegId.get(registrationId);
        if (tokens != null) {
            for (Token token : tokens) {
                Observation obs = build(unsafeGetObservation(token));
                if (obs != null) {
                    result.add(obs);
                }
            }
        }
        return result;
    }

    private Observation build(org.eclipse.californium.core.observe.Observation cfObs) {
        if (cfObs == null)
            return null;

        return ObserveUtil.createLwM2mObservation(cfObs.getRequest());
    }

    private String validateObservation(org.eclipse.californium.core.observe.Observation observation) {
        String endpoint = ObserveUtil.validateCoapObservation(observation);
        if (getRegistration(ObserveUtil.extractRegistrationId(observation)) == null) {
            throw new IllegalStateException("no registration for this Id");
        }

        return endpoint;
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
        schedExecutor.scheduleAtFixedRate(new Cleaner(), cleanPeriod, cleanPeriod, TimeUnit.SECONDS);
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

    private class Cleaner implements Runnable {

        @Override
        public void run() {
            try {
                Collection<Registration> allRegs = new ArrayList<>();
                try {
                    lock.readLock().lock();
                    allRegs.addAll(regsByEp.values());
                } finally {
                    lock.readLock().unlock();
                }

                for (Registration reg : allRegs) {
                    if (!reg.isAlive()) {
                        // force de-registration
                        Deregistration removedRegistration = removeRegistration(reg.getId());
                        expirationListener.registrationExpired(removedRegistration.getRegistration(),
                                removedRegistration.getObservations());
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unexpected Exception while registration cleaning", e);
            }
        }
    }
}

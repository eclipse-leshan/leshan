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
package org.eclipse.leshan.server.registration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in memory store for registration and observation.
 */
public class InMemoryRegistrationStore implements RegistrationStore, Startable, Stoppable, Destroyable {
    private final Logger LOG = LoggerFactory.getLogger(InMemoryRegistrationStore.class);

    // Data structure
    private final Map<String /* end-point */, Registration> regsByEp = new HashMap<>();
    private final Map<InetSocketAddress, Registration> regsByAddr = new HashMap<>();
    private final Map<String /* reg-id */, Registration> regsByRegId = new HashMap<>();
    private final Map<Identity, Registration> regsByIdentity = new HashMap<>();
    private final Map<ObservationIdentifier, Observation> obsByToken = new HashMap<>();
    private final Map<String, Set<ObservationIdentifier>> tokensByRegId = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    private final ScheduledExecutorService schedExecutor;
    private ScheduledFuture<?> cleanerTask;
    private boolean started = false;
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
            regsByRegId.put(registration.getId(), registration);
            regsByIdentity.put(registration.getIdentity(), registration);
            // If a registration is already associated to this address we don't care as we only want to keep the most
            // recent binding.
            regsByAddr.put(registration.getSocketAddress(), registration);
            if (registrationRemoved != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registrationRemoved.getId());
                if (!registrationRemoved.getSocketAddress().equals(registration.getSocketAddress())) {
                    removeFromMap(regsByAddr, registrationRemoved.getSocketAddress(), registrationRemoved);
                }
                if (!registrationRemoved.getId().equals(registration.getId())) {
                    removeFromMap(regsByRegId, registrationRemoved.getId(), registrationRemoved);
                }
                if (!registrationRemoved.getIdentity().equals(registration.getIdentity())) {
                    removeFromMap(regsByIdentity, registrationRemoved.getIdentity(), registrationRemoved);
                }
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
                // If registration is already associated to this address we don't care as we only want to keep the most
                // recent binding.
                regsByAddr.put(updatedRegistration.getSocketAddress(), updatedRegistration);
                if (!registration.getSocketAddress().equals(updatedRegistration.getSocketAddress())) {
                    removeFromMap(regsByAddr, registration.getSocketAddress(), registration);
                }
                regsByIdentity.put(updatedRegistration.getIdentity(), updatedRegistration);
                if (!registration.getIdentity().equals(updatedRegistration.getIdentity())) {
                    removeFromMap(regsByIdentity, registration.getIdentity(), registration);
                }

                regsByRegId.put(updatedRegistration.getId(), updatedRegistration);

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
            return regsByRegId.get(registrationId);
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
            return regsByAddr.get(address);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistrationByIdentity(Identity identity) {
        try {
            lock.readLock().lock();
            return regsByIdentity.get(identity);
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
                removeFromMap(regsByAddr, registration.getSocketAddress(), registration);
                removeFromMap(regsByRegId, registration.getId(), registration);
                removeFromMap(regsByIdentity, registration.getIdentity(), registration);
                return new Deregistration(registration, observationsRemoved);
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Leshan Observation API **************** */

    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation, boolean addIfAbsent) {
        List<Observation> removed = new ArrayList<>();
        try {
            lock.writeLock().lock();

            if (!regsByRegId.containsKey(registrationId)) {
                throw new IllegalStateException(String.format(
                        "can not add observation %s there is no registration with id %s", observation, registrationId));
            }

            Observation previousObservation;
            ObservationIdentifier id = observation.getId();

            if (addIfAbsent) {
                if (!obsByToken.containsKey(id))
                    previousObservation = obsByToken.put(id, observation);
                else
                    previousObservation = obsByToken.get(id);
            } else {
                previousObservation = obsByToken.put(id, observation);
            }
            if (!tokensByRegId.containsKey(registrationId)) {
                tokensByRegId.put(registrationId, new HashSet<ObservationIdentifier>());
            }
            tokensByRegId.get(registrationId).add(id);

            // log any collisions
            if (previousObservation != null) {
                removed.add(previousObservation);
                LOG.warn("Token collision ? observation [{}] will be replaced by observation [{}] ",
                        previousObservation, observation);
            }

            // cancel existing observations for the same path and registration id.
            for (Observation obs : unsafeGetObservations(registrationId)) {
                if (areTheSamePaths(observation, obs) && !observation.getId().equals(obs.getId())) {
                    unsafeRemoveObservation(obs.getId());
                    removed.add(obs);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        return removed;
    }

    private boolean areTheSamePaths(Observation observation, Observation obs) {
        if (observation instanceof SingleObservation && obs instanceof SingleObservation) {
            return ((SingleObservation) observation).getPath().equals(((SingleObservation) obs).getPath());
        }
        if (observation instanceof CompositeObservation && obs instanceof CompositeObservation) {
            return ((CompositeObservation) observation).getPaths().equals(((CompositeObservation) obs).getPaths());
        }
        return false;
    }

    @Override
    public Observation removeObservation(String registrationId, ObservationIdentifier observationId) {
        try {
            lock.writeLock().lock();
            Observation observation = unsafeGetObservation(observationId);
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                unsafeRemoveObservation(observationId);
                return observation;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Observation getObservation(String registrationId, ObservationIdentifier observationId) {
        try {
            lock.readLock().lock();
            Observation observation = unsafeGetObservation(observationId);
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                return observation;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Observation getObservation(ObservationIdentifier observationId) {
        try {
            lock.readLock().lock();
            Observation observation = unsafeGetObservation(observationId);
            if (observation != null) {
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

    /* *************** Observation utility functions **************** */

    private Observation unsafeGetObservation(ObservationIdentifier token) {
        Observation obs = obsByToken.get(token);
        return obs;
    }

    private void unsafeRemoveObservation(ObservationIdentifier observationId) {
        Observation removed = obsByToken.remove(observationId);

        if (removed != null) {
            String registrationId = removed.getRegistrationId();
            Set<ObservationIdentifier> tokens = tokensByRegId.get(registrationId);
            tokens.remove(observationId);
            if (tokens.isEmpty()) {
                tokensByRegId.remove(registrationId);
            }
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        Set<ObservationIdentifier> ids = tokensByRegId.get(registrationId);
        if (ids != null) {
            for (ObservationIdentifier id : ids) {
                Observation observationRemoved = obsByToken.remove(id);
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
        Set<ObservationIdentifier> ids = tokensByRegId.get(registrationId);
        if (ids != null) {
            for (ObservationIdentifier id : ids) {
                Observation obs = unsafeGetObservation(id);
                if (obs != null) {
                    result.add(obs);
                }
            }
        }
        return result;
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
    public synchronized void start() {
        if (!started) {
            started = true;
            cleanerTask = schedExecutor.scheduleAtFixedRate(new Cleaner(), cleanPeriod, cleanPeriod, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop the underlying cleanup of the registrations.
     */
    @Override
    public synchronized void stop() {
        if (started) {
            started = false;
            if (cleanerTask != null) {
                cleanerTask.cancel(false);
                cleanerTask = null;
            }
        }
    }

    /**
     * Destroy "cleanup" scheduler.
     */
    @Override
    public synchronized void destroy() {
        started = false;
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Destroying InMemoryRegistrationStore was interrupted.", e);
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

    // boolean remove(Object key, Object value) exist only since java8
    // So this method is here only while we want to support java 7
    protected <K, V> boolean removeFromMap(Map<K, V> map, K key, V value) {
        if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
            map.remove(key);
            return true;
        } else
            return false;
    }
}

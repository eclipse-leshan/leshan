/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - rename CorrelationContext to
 *                                                     EndpointContext
 *     Achim Kraus (Bosch Software Innovations GmbH) - update to modified
 *                                                     ObservationStore API
 *     Michał Wadowski (Orange)                      - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.server.redis;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.redis.serialization.IdentitySerDes;
import org.eclipse.leshan.server.redis.serialization.ObservationSerDes;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.Pool;

/**
 * A RegistrationStore which stores registrations and observations in Redis.
 */
public class RedisRegistrationStore implements RegistrationStore, Startable, Stoppable, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(RedisRegistrationStore.class);

    // Redis key prefixes
    private final String registrationByEndpointPrefix; // (Endpoint => Registration)
    private final String endpointByRegistrationIdPrefix; // secondary index key (Registration ID => Endpoint)
    private final String endpointBySocketAddressPrefix; // secondary index key (Socket Address => Endpoint)
    private final String endpointByIdentityPrefix; // secondary index key (Identity => Endpoint)
    private final String endpointLockPrefix;
    private final byte[] observationTokenPrefix;
    private final String observationTokensByRegistrationIdPrefix; // secondary index (Registration => Token list)
    private final byte[] endpointExpirationKey; // a sorted set used for registration expiration (expiration date,
                                                // Endpoint)

    private final Pool<Jedis> pool;

    // Listener used to notify about a registration expiration
    private ExpirationListener expirationListener;

    private final ScheduledExecutorService schedExecutor;
    private ScheduledFuture<?> cleanerTask;
    private boolean started = false;

    private final long cleanPeriod; // in seconds
    private final int cleanLimit; // maximum number to clean in a clean period
    private final long gracePeriod; // in seconds

    private final JedisLock lock;
    private final RegistrationSerDes registrationSerDes;
    private final ObservationSerDes observationSerDes;

    public RedisRegistrationStore(Pool<Jedis> p) {
        this(new Builder(p).generateDefaultValue());
    }

    public RedisRegistrationStore(Builder builder) {
        this.pool = builder.pool;
        this.registrationByEndpointPrefix = builder.registrationByEndpointPrefix;
        this.endpointByRegistrationIdPrefix = builder.endpointByRegistrationIdPrefix;
        this.endpointBySocketAddressPrefix = builder.endpointBySocketAddressPrefix;
        this.endpointByIdentityPrefix = builder.endpointByIdentityPrefix;
        this.endpointLockPrefix = builder.endpointLockPrefix;
        this.observationTokenPrefix = builder.observationTokenPrefix.getBytes(UTF_8);
        this.observationTokensByRegistrationIdPrefix = builder.observationTokensByRegistrationIdPrefix;
        this.endpointExpirationKey = builder.endpointExpirationKey.getBytes(UTF_8);
        this.cleanPeriod = builder.cleanPeriod;
        this.cleanLimit = builder.cleanLimit;
        this.gracePeriod = builder.gracePeriod;
        this.schedExecutor = builder.schedExecutor;
        this.lock = builder.lock;
        this.registrationSerDes = builder.registrationSerDes;
        this.observationSerDes = builder.observationSerDes;
    }

    /* *************** Redis Key utility function **************** */

    private byte[] toKey(byte[] prefix, byte[] key) {
        byte[] result = new byte[prefix.length + key.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(key, 0, result, prefix.length, key.length);
        return result;
    }

    private byte[] toKey(String prefix, String registrationID) {
        return (prefix + registrationID).getBytes();
    }

    private byte[] toLockKey(String endpoint) {
        return toKey(endpointLockPrefix, endpoint);
    }

    private byte[] toLockKey(byte[] endpoint) {
        return toKey(endpointLockPrefix.getBytes(UTF_8), endpoint);
    }

    /* *************** Leshan Registration API **************** */

    @Override
    public Deregistration addRegistration(Registration registration) {
        try (Jedis j = pool.getResource()) {
            byte[] lockValue = null;
            byte[] lockKey = toLockKey(registration.getEndpoint());

            try {
                lockValue = lock.acquire(j, lockKey);

                // add registration
                byte[] k = toEndpointKey(registration.getEndpoint());
                byte[] old = j.getSet(k, serializeReg(registration));

                // add registration: secondary indexes
                byte[] regid_idx = toRegIdKey(registration.getId());
                j.set(regid_idx, registration.getEndpoint().getBytes(UTF_8));
                byte[] addr_idx = toRegAddrKey(registration.getSocketAddress());
                j.set(addr_idx, registration.getEndpoint().getBytes(UTF_8));
                byte[] identity_idx = toRegIdentityKey(registration.getIdentity());
                j.set(identity_idx, registration.getEndpoint().getBytes(UTF_8));

                // Add or update expiration
                addOrUpdateExpiration(j, registration);

                if (old != null) {
                    Registration oldRegistration = deserializeReg(old);
                    // remove old secondary index
                    if (!registration.getId().equals(oldRegistration.getId()))
                        j.del(toRegIdKey(oldRegistration.getId()));
                    if (!oldRegistration.getSocketAddress().equals(registration.getSocketAddress())) {
                        removeAddrIndex(j, oldRegistration);
                    }
                    if (!oldRegistration.getIdentity().equals(registration.getIdentity())) {
                        removeIdentityIndex(j, oldRegistration);
                    }
                    // remove old observation
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(j, oldRegistration.getId());

                    return new Deregistration(oldRegistration, obsRemoved);
                }

                return null;
            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public UpdatedRegistration updateRegistration(RegistrationUpdate update) {
        try (Jedis j = pool.getResource()) {

            // Fetch the registration ep by registration ID index
            byte[] ep = j.get(toRegIdKey(update.getRegistrationId()));
            if (ep == null) {
                return null;
            }

            byte[] lockValue = null;
            byte[] lockKey = toLockKey(ep);
            try {
                lockValue = lock.acquire(j, lockKey);

                // Fetch the registration
                byte[] data = j.get(toEndpointKey(ep));
                if (data == null) {
                    return null;
                }

                Registration r = deserializeReg(data);

                Registration updatedRegistration = update.update(r);

                // Store the new registration
                j.set(toEndpointKey(updatedRegistration.getEndpoint()), serializeReg(updatedRegistration));

                // Add or update expiration
                addOrUpdateExpiration(j, updatedRegistration);

                // Update secondary index :
                // If registration is already associated to this address we don't care as we only want to keep the most
                // recent binding.
                byte[] addr_idx = toRegAddrKey(updatedRegistration.getSocketAddress());
                j.set(addr_idx, updatedRegistration.getEndpoint().getBytes(UTF_8));
                if (!r.getSocketAddress().equals(updatedRegistration.getSocketAddress())) {
                    removeAddrIndex(j, r);
                }
                // update secondary index :
                byte[] identity_idx = toRegIdentityKey(updatedRegistration.getIdentity());
                j.set(identity_idx, updatedRegistration.getEndpoint().getBytes(UTF_8));
                if (!r.getIdentity().equals(updatedRegistration.getIdentity())) {
                    removeIdentityIndex(j, r);
                }

                return new UpdatedRegistration(r, updatedRegistration);

            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Registration getRegistration(String registrationId) {
        try (Jedis j = pool.getResource()) {
            return getRegistration(j, registrationId);
        }
    }

    @Override
    public Registration getRegistrationByEndpoint(String endpoint) {
        Validate.notNull(endpoint);
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get(toEndpointKey(endpoint));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    @Override
    public Registration getRegistrationByAdress(InetSocketAddress address) {
        Validate.notNull(address);
        try (Jedis j = pool.getResource()) {
            byte[] ep = j.get(toRegAddrKey(address));
            if (ep == null) {
                return null;
            }
            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    @Override
    public Registration getRegistrationByIdentity(Identity identity) {
        Validate.notNull(identity);
        try (Jedis j = pool.getResource()) {
            byte[] ep = j.get(toRegIdentityKey(identity));
            if (ep == null) {
                return null;
            }
            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    @Override
    public Iterator<Registration> getAllRegistrations() {
        return new RedisIterator(pool, new ScanParams().match(registrationByEndpointPrefix + "*").count(100));
    }

    protected class RedisIterator implements Iterator<Registration> {

        private final Pool<Jedis> pool;
        private final ScanParams scanParams;

        private String cursor;
        private List<Registration> scanResult;

        public RedisIterator(Pool<Jedis> p, ScanParams scanParams) {
            pool = p;
            this.scanParams = scanParams;
            // init scan result
            scanNext("0");
        }

        private void scanNext(String cursor) {
            try (Jedis j = pool.getResource()) {
                do {
                    ScanResult<byte[]> sr = j.scan(cursor.getBytes(), scanParams);

                    this.scanResult = new ArrayList<>();
                    if (sr.getResult() != null && !sr.getResult().isEmpty()) {
                        for (byte[] value : j.mget(sr.getResult().toArray(new byte[][] {}))) {
                            this.scanResult.add(deserializeReg(value));
                        }
                    }

                    cursor = sr.getCursor();
                } while (!"0".equals(cursor) && scanResult.isEmpty());

                this.cursor = cursor;
            }
        }

        @Override
        public boolean hasNext() {
            if (!scanResult.isEmpty()) {
                return true;
            }
            if ("0".equals(cursor)) {
                // no more elements to scan
                return false;
            }

            // read more elements
            scanNext(cursor);
            return !scanResult.isEmpty();
        }

        @Override
        public Registration next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return scanResult.remove(0);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Deregistration removeRegistration(String registrationId) {
        try (Jedis j = pool.getResource()) {
            return removeRegistration(j, registrationId, false);
        }
    }

    private Deregistration removeRegistration(Jedis j, String registrationId, boolean removeOnlyIfNotAlive) {
        // fetch the client ep by registration ID index
        byte[] ep = j.get(toRegIdKey(registrationId));
        if (ep == null) {
            return null;
        }

        byte[] lockValue = null;
        byte[] lockKey = toLockKey(ep);
        try {
            lockValue = lock.acquire(j, lockKey);

            // fetch the client
            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            Registration r = deserializeReg(data);

            if (!removeOnlyIfNotAlive || !r.isAlive(gracePeriod)) {
                long nbRemoved = j.del(toRegIdKey(r.getId()));
                if (nbRemoved > 0) {
                    j.del(toEndpointKey(r.getEndpoint()));
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(j, r.getId());
                    removeAddrIndex(j, r);
                    removeIdentityIndex(j, r);
                    removeExpiration(j, r);
                    return new Deregistration(r, obsRemoved);
                }
            }
            return null;
        } finally {
            lock.release(j, lockKey, lockValue);
        }
    }

    private void removeAddrIndex(Jedis j, Registration r) {
        removeSecondaryIndex(j, toRegAddrKey(r.getSocketAddress()), r.getEndpoint());
    }

    private void removeIdentityIndex(Jedis j, Registration r) {
        removeSecondaryIndex(j, toRegIdentityKey(r.getIdentity()), r.getEndpoint());
    }

    private void removeSecondaryIndex(Jedis j, byte[] indexKey, String endpointName) {
        // Watch the key to remove.
        j.watch(indexKey);

        byte[] epFromAddr = j.get(indexKey);
        // Delete the key if needed.
        if (Arrays.equals(epFromAddr, endpointName.getBytes(UTF_8))) {
            // Try to delete the key
            Transaction transaction = j.multi();
            transaction.del(indexKey);
            transaction.exec();
            // if transaction failed this is not an issue as the index is probably reused and we don't need to
            // delete it anymore.
        } else {
            // the key must not be deleted.
            j.unwatch();
        }
    }

    private void addOrUpdateExpiration(Jedis j, Registration registration) {
        j.zadd(endpointExpirationKey, registration.getExpirationTimeStamp(gracePeriod),
                registration.getEndpoint().getBytes(UTF_8));
    }

    private void removeExpiration(Jedis j, Registration registration) {
        j.zrem(endpointExpirationKey, registration.getEndpoint().getBytes(UTF_8));
    }

    private byte[] toRegIdKey(String registrationId) {
        return toKey(endpointByRegistrationIdPrefix, registrationId);
    }

    private byte[] toRegAddrKey(InetSocketAddress addr) {
        return toKey(endpointBySocketAddressPrefix, addr.getAddress().toString() + ":" + addr.getPort());
    }

    private byte[] toRegIdentityKey(Identity identity) {
        return toKey(endpointByIdentityPrefix, IdentitySerDes.serialize(identity).toString());
    }

    private byte[] toEndpointKey(String endpoint) {
        return toKey(registrationByEndpointPrefix, endpoint);
    }

    private byte[] toEndpointKey(byte[] endpoint) {
        return toKey(registrationByEndpointPrefix.getBytes(UTF_8), endpoint);
    }

    private byte[] serializeReg(Registration registration) {
        return registrationSerDes.bSerialize(registration);
    }

    private Registration deserializeReg(byte[] data) {
        return registrationSerDes.deserialize(data);
    }

    /* *************** Leshan Observation API **************** */

    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation, boolean addIfAbsent) {

        List<Observation> removed = new ArrayList<>();
        try (Jedis j = pool.getResource()) {

            // fetch the client ep by registration ID index
            byte[] ep = j.get(toRegIdKey(registrationId));
            if (ep == null) {
                throw new IllegalStateException(String.format(
                        "can not add observation %s there is no registration with id %s", observation, registrationId));
            }

            byte[] lockValue = null;
            byte[] lockKey = toLockKey(ep);
            try {
                lockValue = lock.acquire(j, lockKey);

                // Add and Get previous observation
                byte[] previousValue;
                byte[] key = toKey(observationTokenPrefix, observation.getId().getBytes());
                byte[] serializeObs = serializeObs(observation);
                if (addIfAbsent) {
                    previousValue = j.get(key);
                    if (previousValue == null || previousValue.length == 0) {
                        j.set(key, serializeObs);
                    }
                } else {
                    previousValue = j.getSet(key, serializeObs);
                }

                // secondary index to get the list by registrationId
                j.lpush(toKey(observationTokensByRegistrationIdPrefix, registrationId), observation.getId().getBytes());

                // log any collisions
                Observation previousObservation;
                if (previousValue != null && previousValue.length != 0) {
                    previousObservation = deserializeObs(previousValue);
                    LOG.warn("Token collision ? observation [{}] will be replaced by observation [{}] ",
                            previousObservation, observation);
                }
                // cancel existing observations for the same path and registration id.
                for (Observation obs : unsafeGetObservations(j, registrationId)) {
                    if (areTheSamePaths(observation, obs) && !observation.getId().equals(obs.getId())) {
                        removed.add(obs);
                        unsafeRemoveObservation(j, registrationId, obs.getId());
                    }
                }

            } finally {
                lock.release(j, lockKey, lockValue);
            }
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
        try (Jedis j = pool.getResource()) {

            // fetch the client ep by registration ID index
            byte[] ep = j.get(toRegIdKey(registrationId));
            if (ep == null) {
                return null;
            }

            // remove observation
            byte[] lockValue = null;
            byte[] lockKey = toLockKey(ep);
            try {
                lockValue = lock.acquire(j, lockKey);

                Observation observation = unsafeGetObservation(j, observationId);
                if (observation != null
                        && (registrationId == null || registrationId.equals(observation.getRegistrationId()))) {
                    unsafeRemoveObservation(j, registrationId, observationId);
                    return observation;
                }
                return null;

            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Observation getObservation(String registrationId, ObservationIdentifier observationId) {
        try (Jedis j = pool.getResource()) {
            Observation observation = unsafeGetObservation(j, observationId);
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                return observation;
            }
            return null;
        }
    }

    @Override
    public Observation getObservation(ObservationIdentifier observationId) {
        try (Jedis j = pool.getResource()) {
            return unsafeGetObservation(j, observationId);
        }
    }

    @Override
    public Collection<Observation> getObservations(String registrationId) {
        try (Jedis j = pool.getResource()) {
            return unsafeGetObservations(j, registrationId);
        }
    }

    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        try (Jedis j = pool.getResource()) {
            // check registration exists
            Registration registration = getRegistration(j, registrationId);
            if (registration == null)
                return Collections.emptyList();

            // get endpoint and create lock
            String endpoint = registration.getEndpoint();
            byte[] lockValue = null;
            byte[] lockKey = toKey(endpointLockPrefix, endpoint);
            try {
                lockValue = lock.acquire(j, lockKey);

                return unsafeRemoveAllObservations(j, registrationId);
            } finally {
                lock.release(j, lockKey, lockValue);
            }
        }
    }

    /* *************** Observation utility functions **************** */

    private Registration getRegistration(Jedis j, String registrationId) {
        byte[] ep = j.get(toRegIdKey(registrationId));
        if (ep == null) {
            return null;
        }
        byte[] data = j.get(toEndpointKey(ep));
        if (data == null) {
            return null;
        }

        return deserializeReg(data);
    }

    private Collection<Observation> unsafeGetObservations(Jedis j, String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        for (byte[] token : j.lrange(toKey(observationTokensByRegistrationIdPrefix, registrationId), 0, -1)) {
            byte[] obs = j.get(toKey(observationTokenPrefix, token));
            if (obs != null) {
                result.add(deserializeObs(obs));
            }
        }
        return result;
    }

    private Observation unsafeGetObservation(Jedis j, ObservationIdentifier observationId) {
        byte[] obs = j.get(toKey(observationTokenPrefix, observationId.getBytes()));
        if (obs == null) {
            return null;
        } else {
            return deserializeObs(obs);
        }
    }

    private void unsafeRemoveObservation(Jedis j, String registrationId, ObservationIdentifier observationId) {
        if (j.del(toKey(observationTokenPrefix, observationId.getBytes())) > 0L) {
            j.lrem(toKey(observationTokensByRegistrationIdPrefix, registrationId), 0, observationId.getBytes());
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(Jedis j, String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        byte[] regIdKey = toKey(observationTokensByRegistrationIdPrefix, registrationId);

        // fetch all observations by token
        for (byte[] token : j.lrange(regIdKey, 0, -1)) {
            byte[] obs = j.get(toKey(observationTokenPrefix, token));
            if (obs != null) {
                removed.add(deserializeObs(obs));
            }
            j.del(toKey(observationTokenPrefix, token));
        }
        j.del(regIdKey);

        return removed;
    }

    private byte[] serializeObs(Observation obs) {
        return observationSerDes.serialize(obs);
    }

    private Observation deserializeObs(byte[] data) {
        return observationSerDes.deserialize(data);
    }

    /* *************** Expiration handling **************** */

    /**
     * Start regular cleanup of dead registrations.
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
            LOG.warn("Destroying RedisRegistrationStore was interrupted.", e);
        }
    }

    private class Cleaner implements Runnable {

        @Override
        public void run() {

            try (Jedis j = pool.getResource()) {
                List<byte[]> endpointsExpired = j.zrangeByScore(endpointExpirationKey, Double.NEGATIVE_INFINITY,
                        System.currentTimeMillis(), 0, cleanLimit);

                for (byte[] endpoint : endpointsExpired) {
                    byte[] regBytes = j.get(toEndpointKey(endpoint));
                    if (regBytes != null) {
                        Registration r = deserializeReg(regBytes);
                        if (!r.isAlive(gracePeriod)) {
                            Deregistration dereg = removeRegistration(j, r.getId(), true);
                            if (dereg != null)
                                expirationListener.registrationExpired(dereg.getRegistration(),
                                        dereg.getObservations());
                        }
                    }
                }
            } catch (RuntimeException e) {
                LOG.warn("Unexpected Exception while registration cleaning", e);
            }
        }
    }

    @Override
    public void setExpirationListener(ExpirationListener listener) {
        expirationListener = listener;
    }

    /**
     * Class helping to build and configure a {@link RedisRegistrationStore}.
     */
    public static class Builder {

        private final Pool<Jedis> pool;
        private String prefix;
        private String registrationByEndpointPrefix;
        private String endpointByRegistrationIdPrefix;
        private String endpointBySocketAddressPrefix;
        private String endpointByIdentityPrefix;
        private String endpointLockPrefix;
        private String observationTokenPrefix;
        private String observationTokensByRegistrationIdPrefix;
        private String endpointExpirationKey;

        /** Time in seconds between 2 cleaning tasks (used to remove expired registration) */
        private long cleanPeriod;
        private int cleanLimit;
        /** extra time for registration lifetime in seconds */
        private long gracePeriod;

        private ScheduledExecutorService schedExecutor;
        private JedisLock lock;
        private RegistrationSerDes registrationSerDes;
        private ObservationSerDes observationSerDes;

        /**
         * Set the prefix for all keys and prefixes.
         * <p>
         * Default value is {@literal REGSTORE#}.
         */
        public Builder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Set the key prefix for registration info lookup by endpoint.
         * <p>
         * Default value is {@literal REG#EP#}. Should not be {@code null} or empty. Leshan v1.x used
         * {@literal REG:EP:}.
         */
        public Builder setRegistrationByEndpointPrefix(String registrationByEndpointPrefix) {
            this.registrationByEndpointPrefix = registrationByEndpointPrefix;
            return this;
        }

        /**
         * Set the key prefix for endpoint lookup by registration ID.
         * <p>
         * Default value is {@literal EP#REGID#}. Should not be {@code null} or empty. Leshan v1.x used
         * {@literal EP:REGID:}.
         */
        public Builder setEndpointByRegistrationIdPrefix(String endpointByRegistrationIdPrefix) {
            this.endpointByRegistrationIdPrefix = endpointByRegistrationIdPrefix;
            return this;
        }

        /**
         * Set the key prefix for endpoint lookup by socket address.
         * <p>
         * Default value is {@literal EP#ADDR#}. Should not be {@code null} or empty. Leshan v1.x used
         * {@literal EP:ADDR:}.
         */
        public Builder setEndpointBySocketAddressPrefix(String endpointBySocketAddressPrefix) {
            this.endpointBySocketAddressPrefix = endpointBySocketAddressPrefix;
            return this;
        }

        /**
         * Set the key prefix for endpoint lookup by registration identity.
         * <p>
         * Default value is {@literal EP#IDENTITY#}. Should not be {@code null} or empty. Leshan v1.x used
         * {@literal EP:IDENTITY:}.
         */
        public Builder setEndpointByIdentityPrefix(String endpointByIdentityPrefix) {
            this.endpointByIdentityPrefix = endpointByIdentityPrefix;
            return this;
        }

        /**
         * Set the key prefix for endpoint locks lookup.
         * <p>
         * Default value is {@literal LOCK#EP#}. Should not be {@code null} or empty. Leshan v1.x used
         * {@literal LOCK:EP:}.
         */
        public Builder setEndpointLockPrefix(String endpointLockPrefix) {
            this.endpointLockPrefix = endpointLockPrefix;
            return this;
        }

        /**
         * Set the key prefix for observation token lookup.
         * <p>
         * Default value is {@literal OBS#TKN#}. Should not be {@code null} or empty. Leshan v1.x used
         * {@literal OBS:TKN:}.
         */
        public Builder setObservationTokenPrefix(String observationTokenPrefix) {
            this.observationTokenPrefix = observationTokenPrefix;
            return this;
        }

        /**
         * Set the key prefix for observation tokens list lookup by registration ID.
         * <p>
         * Default value is {@literal TKNS#REGID#}. Should not be {@code null} or empty. Leshan v1.x used
         * {@literal TKNS:REGID:}.
         */
        public Builder setObservationTokensByRegistrationIdPrefix(String observationTokensByRegistrationIdPrefix) {
            this.observationTokensByRegistrationIdPrefix = observationTokensByRegistrationIdPrefix;
            return this;
        }

        /**
         * Set the key for expiration key lookup. It is a sorted set used for registration expiration (expiration date,
         * endpoint).
         * <p>
         * Default value is {@literal EXP#EP}. Should not be {@code null} or empty. Leshan v1.x used {@literal EXP:EP}.
         */
        public Builder setEndpointExpirationKey(String endpointExpirationKey) {
            this.endpointExpirationKey = endpointExpirationKey;
            return this;
        }

        /**
         * Set time between 2 periodic task about cleaning expired registration.
         * <p>
         * Default value is {@literal 60 seconds}.
         */
        public Builder setCleanPeriod(long cleanPeriod) {
            this.cleanPeriod = cleanPeriod;
            return this;
        }

        /**
         * Set maximum number of expired registration removed by clean period
         * <p>
         * Default value is {@literal 500}.
         */
        public Builder setCleanLimit(int cleanLimit) {
            this.cleanLimit = cleanLimit;
            return this;
        }

        /**
         * Set some extra time added to registration lifetime when calculating if a registration expired.
         * <p>
         * Default value is {@literal 0 seconds}.
         */
        public Builder setGracePeriod(long gracePeriod) {
            this.gracePeriod = gracePeriod;
            return this;
        }

        /**
         * Set {@link ScheduledExecutorService} used to launch period task about cleaning expired registration.
         */
        public Builder setSchedExecutor(ScheduledExecutorService schedExecutor) {
            this.schedExecutor = schedExecutor;
            return this;
        }

        /**
         * Set {@link JedisLock} implementation used to handle concurrent access to this store.
         * <p>
         * Default implementation used is {@link SingleInstanceJedisLock}
         */
        public Builder setLock(JedisLock lock) {
            this.lock = lock;
            return this;
        }

        /**
         * Set {@link RegistrationSerDes} instance used to serialize/de-serialize {@link Registration} to/from this
         * store.
         */
        public Builder setRegistrationSerDes(RegistrationSerDes registrationSerDes) {
            this.registrationSerDes = registrationSerDes;
            return this;
        }

        /**
         * Set {@link ObservationSerDes} instance used to serialize/de-serialize {@link Observation} to/from this store.
         */
        public Builder setObservationSerDes(ObservationSerDes observationSerDes) {
            this.observationSerDes = observationSerDes;
            return this;
        }

        public Builder(Pool<Jedis> pool) {
            this.pool = pool;
            this.prefix = "REGSTORE#";
            this.registrationByEndpointPrefix = "REG#EP#";
            this.endpointByRegistrationIdPrefix = "EP#REGID#";
            this.endpointBySocketAddressPrefix = "EP#ADDR#";
            this.endpointByIdentityPrefix = "EP#IDENTITY#";
            this.endpointLockPrefix = "LOCK#EP#";
            this.observationTokenPrefix = "OBS#TKN#";
            this.observationTokensByRegistrationIdPrefix = "TKNS#REGID#";
            this.endpointExpirationKey = "EXP#EP";
            this.cleanPeriod = 60;
            this.cleanLimit = 500;
            this.gracePeriod = 0;
        }

        protected Builder generateDefaultValue() {
            if (this.schedExecutor == null) {
                this.schedExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(
                        String.format("RedisRegistrationStore Cleaner (%ds)", this.cleanPeriod)));
            }

            if (this.lock == null) {
                this.lock = new SingleInstanceJedisLock();
            }

            if (this.registrationSerDes == null) {
                this.registrationSerDes = new RegistrationSerDes();
            }

            if (this.observationSerDes == null) {
                this.observationSerDes = new ObservationSerDes();
            }

            return this;
        }

        /**
         * Create the {@link RedisRegistrationStore}.
         * <p>
         * Throws {@link IllegalArgumentException} when any of prefixes is not set or is equal to some other.
         */
        public RedisRegistrationStore build() throws IllegalArgumentException {
            if (this.registrationByEndpointPrefix == null || this.registrationByEndpointPrefix.isEmpty()) {
                throw new IllegalArgumentException("registrationByEndpointPrefix should not be empty");
            }

            if (this.endpointByRegistrationIdPrefix == null || this.endpointByRegistrationIdPrefix.isEmpty()) {
                throw new IllegalArgumentException("endpointByRegistrationIdPrefix should not be empty");
            }

            if (this.endpointBySocketAddressPrefix == null || this.endpointBySocketAddressPrefix.isEmpty()) {
                throw new IllegalArgumentException("endpointBySocketAddressPrefix should not be empty");
            }

            if (this.endpointByIdentityPrefix == null || this.endpointByIdentityPrefix.isEmpty()) {
                throw new IllegalArgumentException("endpointByIdentityPrefix should not be empty");
            }

            if (this.endpointLockPrefix == null || this.endpointLockPrefix.isEmpty()) {
                throw new IllegalArgumentException("endpointLockPrefix should not be empty");
            }

            if (this.observationTokenPrefix == null || this.observationTokenPrefix.isEmpty()) {
                throw new IllegalArgumentException("observationTokenPrefix should not be empty");
            }

            if (this.observationTokensByRegistrationIdPrefix == null
                    || this.observationTokensByRegistrationIdPrefix.isEmpty()) {
                throw new IllegalArgumentException("observationTokensByRegistrationIdPrefix should not be empty");
            }

            if (this.endpointExpirationKey == null || this.endpointExpirationKey.isEmpty()) {
                throw new IllegalArgumentException("endpointExpirationKey should not be empty");
            }

            // Make sure same prefix is not used more than once
            String[] prefixes = new String[] { this.registrationByEndpointPrefix, this.endpointByRegistrationIdPrefix,
                    this.endpointBySocketAddressPrefix, this.endpointByIdentityPrefix, this.endpointLockPrefix,
                    this.observationTokenPrefix, this.observationTokensByRegistrationIdPrefix,
                    this.endpointExpirationKey };
            Set<String> uniquePrefixes = new HashSet<>();

            for (String prefix : prefixes) {
                if (!uniquePrefixes.add(prefix)) {
                    throw new IllegalArgumentException(String.format("prefix name %s is taken already", prefix));
                }
            }

            if (this.prefix != null) {
                this.registrationByEndpointPrefix = this.prefix + this.registrationByEndpointPrefix;
                this.endpointByRegistrationIdPrefix = this.prefix + this.endpointByRegistrationIdPrefix;
                this.endpointBySocketAddressPrefix = this.prefix + this.endpointBySocketAddressPrefix;
                this.endpointByIdentityPrefix = this.prefix + this.endpointByIdentityPrefix;
                this.endpointLockPrefix = this.prefix + this.endpointLockPrefix;
                this.observationTokenPrefix = this.prefix + this.observationTokenPrefix;
                this.observationTokensByRegistrationIdPrefix = this.prefix
                        + this.observationTokensByRegistrationIdPrefix;
                this.endpointExpirationKey = this.prefix + this.endpointExpirationKey;
            }

            generateDefaultValue();

            return new RedisRegistrationStore(this);
        }
    }
}

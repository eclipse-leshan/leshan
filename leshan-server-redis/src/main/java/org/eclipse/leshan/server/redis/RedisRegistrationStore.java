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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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

    /** Default time in seconds between 2 cleaning tasks (used to remove expired registration). */
    public static final long DEFAULT_CLEAN_PERIOD = 60;
    public static final int DEFAULT_CLEAN_LIMIT = 500;
    /** Defaut Extra time for registration lifetime in seconds */
    public static final long DEFAULT_GRACE_PERIOD = 0;

    private static final Logger LOG = LoggerFactory.getLogger(RedisRegistrationStore.class);

    // Redis key prefixes
    private static final String REG_EP = "REG:EP:"; // (Endpoint => Registration)
    private static final String REG_EP_REGID_IDX = "EP:REGID:"; // secondary index key (Registration ID => Endpoint)
    private static final String REG_EP_ADDR_IDX = "EP:ADDR:"; // secondary index key (Socket Address => Endpoint)
    private static final String REG_EP_IDENTITY = "EP:IDENTITY:"; // secondary index key (Identity => Endpoint)
    private static final String LOCK_EP = "LOCK:EP:";
    private static final byte[] OBS_TKN = "OBS:TKN:".getBytes(UTF_8);
    private static final String OBS_TKNS_REGID_IDX = "TKNS:REGID:"; // secondary index (token list by registration)
    private static final byte[] EXP_EP = "EXP:EP".getBytes(UTF_8); // a sorted set used for registration expiration
                                                                   // (expiration date, Endpoint)

    private final Pool<Jedis> pool;

    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    private final ScheduledExecutorService schedExecutor;
    private ScheduledFuture<?> cleanerTask;
    private boolean started = false;

    private final long cleanPeriod; // in seconds
    private final int cleanLimit; // maximum number to clean in a clean period
    private final long gracePeriod; // in seconds

    private final JedisLock lock;
    private final RegistrationSerDes registrationSerDes;

    public RedisRegistrationStore(Pool<Jedis> p) {
        this(p, DEFAULT_CLEAN_PERIOD, DEFAULT_GRACE_PERIOD, DEFAULT_CLEAN_LIMIT); // default clean period 60s
    }

    public RedisRegistrationStore(Pool<Jedis> p, long cleanPeriodInSec, long lifetimeGracePeriodInSec, int cleanLimit) {
        this(p, Executors.newScheduledThreadPool(1,
                new NamedThreadFactory(String.format("RedisRegistrationStore Cleaner (%ds)", cleanPeriodInSec))),
                cleanPeriodInSec, lifetimeGracePeriodInSec, cleanLimit);
    }

    public RedisRegistrationStore(Pool<Jedis> p, ScheduledExecutorService schedExecutor, long cleanPeriodInSec,
            long lifetimeGracePeriodInSec, int cleanLimit) {
        this(p, schedExecutor, cleanPeriodInSec, lifetimeGracePeriodInSec, cleanLimit, new SingleInstanceJedisLock());
    }

    public RedisRegistrationStore(Pool<Jedis> p, ScheduledExecutorService schedExecutor, long cleanPeriodInSec,
            long lifetimeGracePeriodInSec, int cleanLimit, JedisLock redisLock) {
        this(p, schedExecutor, cleanPeriodInSec, lifetimeGracePeriodInSec, cleanLimit, redisLock,
                new RegistrationSerDes());
    }

    public RedisRegistrationStore(Pool<Jedis> p, ScheduledExecutorService schedExecutor, long cleanPeriodInSec,
            long lifetimeGracePeriodInSec, int cleanLimit, JedisLock redisLock, RegistrationSerDes registrationSerDes) {
        this.pool = p;
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
        this.cleanLimit = cleanLimit;
        this.gracePeriod = lifetimeGracePeriodInSec;
        this.lock = redisLock;
        this.registrationSerDes = registrationSerDes;
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
        return toKey(LOCK_EP, endpoint);
    }

    private byte[] toLockKey(byte[] endpoint) {
        return toKey(LOCK_EP.getBytes(UTF_8), endpoint);
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
        return new RedisIterator(pool, new ScanParams().match(REG_EP + "*").count(100));
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
        j.zadd(EXP_EP, registration.getExpirationTimeStamp(gracePeriod), registration.getEndpoint().getBytes(UTF_8));
    }

    private void removeExpiration(Jedis j, Registration registration) {
        j.zrem(EXP_EP, registration.getEndpoint().getBytes(UTF_8));
    }

    private byte[] toRegIdKey(String registrationId) {
        return toKey(REG_EP_REGID_IDX, registrationId);
    }

    private byte[] toRegAddrKey(InetSocketAddress addr) {
        return toKey(REG_EP_ADDR_IDX, addr.getAddress().toString() + ":" + addr.getPort());
    }

    private byte[] toRegIdentityKey(Identity identity) {
        return toKey(REG_EP_IDENTITY, IdentitySerDes.serialize(identity).toString());
    }

    private byte[] toEndpointKey(String endpoint) {
        return toKey(REG_EP, endpoint);
    }

    private byte[] toEndpointKey(byte[] endpoint) {
        return toKey(REG_EP.getBytes(UTF_8), endpoint);
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
                byte[] key = toKey(OBS_TKN, observation.getId().getBytes());
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
                j.lpush(toKey(OBS_TKNS_REGID_IDX, registrationId), observation.getId().getBytes());

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
            byte[] lockKey = toKey(LOCK_EP, endpoint);
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
        for (byte[] token : j.lrange(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, -1)) {
            byte[] obs = j.get(toKey(OBS_TKN, token));
            if (obs != null) {
                result.add(deserializeObs(obs));
            }
        }
        return result;
    }

    private Observation unsafeGetObservation(Jedis j, ObservationIdentifier observationId) {
        byte[] obs = j.get(toKey(OBS_TKN, observationId.getBytes()));
        if (obs == null) {
            return null;
        } else {
            return deserializeObs(obs);
        }
    }

    private void unsafeRemoveObservation(Jedis j, String registrationId, ObservationIdentifier observationId) {
        if (j.del(toKey(OBS_TKN, observationId.getBytes())) > 0L) {
            j.lrem(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, observationId.getBytes());
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(Jedis j, String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        byte[] regIdKey = toKey(OBS_TKNS_REGID_IDX, registrationId);

        // fetch all observations by token
        for (byte[] token : j.lrange(regIdKey, 0, -1)) {
            byte[] obs = j.get(toKey(OBS_TKN, token));
            if (obs != null) {
                removed.add(deserializeObs(obs));
            }
            j.del(toKey(OBS_TKN, token));
        }
        j.del(regIdKey);

        return removed;
    }

    private byte[] serializeObs(Observation obs) {
        return ObservationSerDes.serialize(obs);
    }

    private Observation deserializeObs(byte[] data) {
        return ObservationSerDes.deserialize(data);
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
                List<byte[]> endpointsExpired = j.zrangeByScore(EXP_EP, Double.NEGATIVE_INFINITY,
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
}

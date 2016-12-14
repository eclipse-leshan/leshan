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
package org.eclipse.leshan.server.cluster;

import static org.eclipse.leshan.server.californium.impl.CoapRequestBuilder.*;
import static org.eclipse.leshan.util.Charsets.UTF_8;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.CorrelationContext;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.impl.CoapRequestBuilder;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.cluster.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.cluster.serialization.ObservationSerDes;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.util.Pool;

/**
 * A RegistrationStore which stores registrations and observations in Redis.
 */
public class RedisRegistrationStore implements CaliforniumRegistrationStore, Startable, Stoppable {

    private static final Logger LOG = LoggerFactory.getLogger(RedisRegistrationStore.class);

    // Redis key prefixes
    private static final String EP_REG = "EP#REG#";
    private static final String REGID_EP = "REGID#EP#";
    private static final String LOCK_EP = "LOCK#EP#";
    private static final byte[] OBS_TKN = "OBS#TKN#".getBytes(UTF_8);
    private static final String OBS_REGID = "OBS#REGID#";

    private final Pool<Jedis> pool;

    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    public RedisRegistrationStore(Pool<Jedis> p) {
        this.pool = p;
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
                lockValue = RedisLock.acquire(j, lockKey);

                // add registration
                byte[] k = toEndpointKey(registration.getEndpoint());
                byte[] old = j.getSet(k, serializeReg(registration));

                // add registration: secondary index
                byte[] idx = toRegIdKey(registration.getId());
                j.set(idx, registration.getEndpoint().getBytes(UTF_8));

                if (old != null) {
                    Registration oldRegistration = deserializeReg(old);
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(j,
                            oldRegistration.getId());
                    return new Deregistration(oldRegistration, obsRemoved);
                }

                return null;
            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Registration updateRegistration(RegistrationUpdate update) {
        try (Jedis j = pool.getResource()) {

            // fetch the client ep by registration ID index
            byte[] ep = j.get(toRegIdKey(update.getRegistrationId()));
            if (ep == null) {
                return null;
            }

            // fetch the client
            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }

            Registration r = deserializeReg(data);

            byte[] lockValue = null;
            byte[] lockKey = toLockKey(r.getEndpoint());
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                Registration updatedRegistration = update.update(r);

                // store the new client
                j.set(toEndpointKey(updatedRegistration.getEndpoint()), serializeReg(updatedRegistration));

                return updatedRegistration;

            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Registration getRegistration(String registrationId) {
        try (Jedis j = pool.getResource()) {
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
    }

    @Override
    public Registration getRegistrationByEndpoint(String endpoint) {
        Validate.notNull(endpoint);
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get(toEndpointKey(endpoint));
            if (data == null) {
                return null;
            }
            Registration r = deserializeReg(data);
            return r.isAlive() ? r : null;
        }
    }

    @Override
    public Collection<Registration> getRegistrationByAdress(InetSocketAddress address) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Registration> getAllRegistration() {
        try (Jedis j = pool.getResource()) {
            ScanParams params = new ScanParams().match(EP_REG + "*").count(100);
            Collection<Registration> list = new LinkedList<>();
            String cursor = "0";
            do {
                ScanResult<byte[]> res = j.scan(cursor.getBytes(), params);
                for (byte[] key : res.getResult()) {
                    byte[] element = j.get(key);
                    if (element != null) {
                        Registration r = deserializeReg(element);
                        if (r.isAlive()) {
                            list.add(r);
                        }
                    }
                }
                cursor = res.getStringCursor();
            } while (!"0".equals(cursor));
            return list;
        }
    }

    @Override
    public Deregistration removeRegistration(String registrationId) {
        try (Jedis j = pool.getResource()) {

            byte[] regKey = toRegIdKey(registrationId);

            // fetch the client ep by registration ID index
            byte[] ep = j.get(regKey);
            if (ep == null) {
                return null;
            }

            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }

            Registration r = deserializeReg(data);
            deleteRegistration(j, r);
            Collection<Observation> obsRemoved = unsafeRemoveAllObservations(j, r.getId());
            return new Deregistration(r, obsRemoved);
        }
    }

    private void deleteRegistration(Jedis j, Registration r) {
        byte[] lockValue = null;
        byte[] lockKey = toLockKey(r.getEndpoint());
        try {
            lockValue = RedisLock.acquire(j, lockKey);

            // delete all entries
            j.del(toRegIdKey(r.getId()));
            j.del(toEndpointKey(r.getEndpoint()));

        } finally {
            RedisLock.release(j, lockKey, lockValue);
        }
    }

    private byte[] toRegIdKey(String registrationId) {
        return toKey(REGID_EP, registrationId);
    }

    private byte[] toEndpointKey(String endpoint) {
        return toKey(EP_REG, endpoint);
    }

    private byte[] toEndpointKey(byte[] endpoint) {
        return toKey(EP_REG.getBytes(UTF_8), endpoint);
    }

    private byte[] serializeReg(Registration registration) {
        return RegistrationSerDes.bSerialize(registration);
    }

    private Registration deserializeReg(byte[] data) {
        return RegistrationSerDes.deserialize(data);
    }

    /* *************** Leshan Observation API **************** */

    @Override
    public Observation addObservation(String registrationId, Observation observation) {
        // not used observation was added by Californium
        return null;
    }

    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
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
                lockValue = RedisLock.acquire(j, lockKey);

                Observation observation = build(get(observationId));
                if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                    unsafeRemoveObservation(j, registrationId, observationId);
                    return observation;
                }
                return null;

            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Observation getObservation(String registrationId, byte[] ObservationId) {
        return build(get(ObservationId));
    }

    @Override
    public Collection<Observation> getObservations(String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        try (Jedis j = pool.getResource()) {
            for (byte[] token : j.lrange(toKey(OBS_REGID, registrationId), 0, -1)) {
                byte[] obs = j.get(toKey(OBS_TKN, token));
                if (obs != null) {
                    result.add(build(deserializeObs(obs)));
                }
            }
        }
        return result;
    }

    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        try (Jedis j = pool.getResource()) {
            // check registration exists
            Registration registration = getRegistration(registrationId);
            if (registration == null)
                return Collections.emptyList();

            // get endpoint and create lock
            String endpoint = registration.getEndpoint();
            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, endpoint);
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                return unsafeRemoveAllObservations(j, registrationId);
            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    /* *************** Californium ObservationStore API **************** */

    @Override
    public void add(org.eclipse.californium.core.observe.Observation obs) {
        String endpoint = this.validateObservation(obs);

        try (Jedis j = pool.getResource()) {
            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, endpoint);
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                String registrationId = obs.getRequest().getUserContext().get(CTX_REGID);
                if (!j.exists(toRegIdKey(registrationId)))
                    throw new IllegalStateException("no registration for this Id");

                byte[] previousValue = j.getSet(toKey(OBS_TKN, obs.getRequest().getToken()), serializeObs(obs));

                // secondary index to get the list by registrationId
                j.lpush(toKey(OBS_REGID, registrationId), obs.getRequest().getToken());

                // log any collisions
                if (previousValue != null && previousValue.length != 0) {
                    org.eclipse.californium.core.observe.Observation previousObservation = deserializeObs(
                            previousValue);
                    LOG.warn(
                            "Token collision ? observation from request [{}] will be replaced by observation from request [{}] ",
                            previousObservation.getRequest(), obs.getRequest());
                }
            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public void remove(byte[] token) {
        try (Jedis j = pool.getResource()) {
            byte[] tokenKey = toKey(OBS_TKN, token);

            // fetch the observation by token
            byte[] serializedObs = j.get(tokenKey);
            if (serializedObs == null)
                return;

            org.eclipse.californium.core.observe.Observation obs = deserializeObs(serializedObs);
            String registrationId = extractRegistrationId(obs);
            Registration registration = getRegistration(registrationId);
            String endpoint = registration.getEndpoint();

            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, endpoint);
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                unsafeRemoveObservation(j, registrationId, token);
            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }

    }

    @Override
    public org.eclipse.californium.core.observe.Observation get(byte[] token) {
        try (Jedis j = pool.getResource()) {
            byte[] obs = j.get(toKey(OBS_TKN, token));
            if (obs == null) {
                return null;
            } else {
                return deserializeObs(obs);
            }
        }
    }

    /* *************** Observation utility functions **************** */

    private void unsafeRemoveObservation(Jedis j, String registrationId,
            byte[] observationId) {
        if (j.del(observationId) > 0L) {
            j.lrem(toKey(OBS_REGID, registrationId), 0, observationId);
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(Jedis j, String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        byte[] regIdKey = toKey(OBS_REGID, registrationId);

        // fetch all observations by token
        for (byte[] token : j.lrange(regIdKey, 0, -1)) {
            byte[] obs = j.get(toKey(OBS_TKN, token));
            if (obs != null) {
                removed.add(build(deserializeObs(obs)));
                }
            j.del(toKey(OBS_TKN, token));
        }
        j.del(regIdKey);

        return removed;
    }

    @Override
    public void setContext(byte[] token, CorrelationContext correlationContext) {
        // TODO should be implemented
    }

    private byte[] serializeObs(org.eclipse.californium.core.observe.Observation obs) {
        return ObservationSerDes.serialize(obs);
    }

    private org.eclipse.californium.core.observe.Observation deserializeObs(byte[] data) {
        return ObservationSerDes.deserialize(data);
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

    private String validateObservation(org.eclipse.californium.core.observe.Observation observation) {
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_REGID))
            throw new IllegalStateException("missing registrationId info in the request context");
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_LWM2M_PATH))
            throw new IllegalStateException("missing lwm2m path info in the request context");

        String endpoint = observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_ENDPOINT);
        if (endpoint == null)
            throw new IllegalStateException("missing endpoint info in the request context");

        return endpoint;
    }

    /* *************** Expiration handling **************** */

    /**
     * Start regular cleanup of dead registrations.
     */
    @Override
    public void start() {
        // clean the registration list every minute
        schedExecutor.scheduleAtFixedRate(new Cleaner(), 1, 1, TimeUnit.MINUTES);
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

            try (Jedis j = pool.getResource()) {
                ScanParams params = new ScanParams().match(EP_REG + "*").count(100);
                String cursor = "0";
                do {
                    // TODO we probably need a lock here
                    ScanResult<byte[]> res = j.scan(cursor.getBytes(), params);
                    for (byte[] key : res.getResult()) {
                        Registration r = deserializeReg(j.get(key));
                        if (!r.isAlive()) {
                            deleteRegistration(j, r);
                            expirationListener.registrationExpired(r, new ArrayList<Observation>());
                        }
                    }
                    cursor = res.getStringCursor();
                } while (!"0".equals(cursor));
            } catch (Exception e) {
                LOG.warn("Unexcepted Exception while registration cleaning", e);
            }
        }
    }

    @Override
    public void setExpirationListener(ExpirationListener listener) {
        expirationListener = listener;
    }
}

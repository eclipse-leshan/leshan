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
package org.eclipse.leshan.server.demo.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.elements.CorrelationContext;
import org.eclipse.leshan.server.californium.impl.CoapRequestBuilder;
import org.eclipse.leshan.server.californium.impl.LwM2mObservationStore;
import org.eclipse.leshan.server.demo.cluster.serialization.ObservationSerDes;
import org.eclipse.leshan.util.Charsets;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.Pool;

/**
 * An implementation of the Californium {@link ObservationStore} storing {@link Observation} in a Redis store.
 * 
 * Observations are stored using the token as primary key and a secondary index based on the registration Id.
 * 
 * Write accesses are protected with a lock which is implemented in single-instance use case (see
 * http://redis.io/topics/distlock#correct-implementation-with-a-single-instance for more information).
 */
public class RedisObservationStore implements LwM2mObservationStore {

    private final Pool<Jedis> pool;

    // Redis key prefixes
    private static final byte[] OBS_TKN = "OBS#TKN#".getBytes();
    private static final byte[] DELETE_OBS_TKN = "TODELETE#OBS#TKN#".getBytes();
    private static final String OBS_REG = "OBS#REG#";
    private static final String DELETE_OBS_REG = "TODELETE#OBS#REG#";

    private static final String LOCK_REG = "LOCK#REG#";

    private static final byte[] NX_OPTION = "NX".getBytes(Charsets.UTF_8); // set the key if it does not already exist
    private static final byte[] PX_OPTION = "PX".getBytes(Charsets.UTF_8); // expire time in millisecond

    private static final Random RND = new Random();

    public RedisObservationStore(Pool<Jedis> pool) {
        this.pool = pool;
    }

    @Override
    public void add(Observation obs) {
        this.validateObservation(obs);
        String registrationId = getRegistrationId(obs);

        try (Jedis j = pool.getResource()) {
            byte[] lockValue = null;
            try {
                lockValue = acquireLock(j, registrationId);

                j.set(toKey(OBS_TKN, obs.getRequest().getToken()), serialize(obs));

                // secondary index to get the list by registrationId
                j.lpush(toKey(OBS_REG, registrationId), obs.getRequest().getToken());

            } finally {
                releaseLock(j, registrationId, lockValue);
            }
        }
    }

    @Override
    public void remove(byte[] token) {
        try (Jedis j = pool.getResource()) {
            byte[] tokenKey = toKey(OBS_TKN, token);
            byte[] delTokenKey = toKey(DELETE_OBS_TKN, token);

            // fetch the observation by token
            Observation obs = deserialize(j.get(tokenKey));
            String registrationId = getRegistrationId(obs);

            byte[] lockValue = null;
            try {
                lockValue = acquireLock(j, registrationId);

                try {
                    // rename for atomicity
                    if (!"OK".equals(j.rename(tokenKey, delTokenKey))) {
                        return;
                    }
                } catch (JedisDataException e) {
                    // the Jedis library raises an exception (ERR no such key)
                    // instead of returning a status code
                    return;
                }

                j.del(delTokenKey);
                j.lrem(toKey(OBS_REG, registrationId), 0, token);

            } finally {
                releaseLock(j, registrationId, lockValue);
            }
        }
    }

    @Override
    public Collection<Observation> removeAll(String registrationId) {
        try (Jedis j = pool.getResource()) {

            byte[] lockValue = null;
            try {
                lockValue = acquireLock(j, registrationId);

                byte[] regIdKey = toKey(OBS_REG, registrationId);
                byte[] delRegIdKey = toKey(DELETE_OBS_REG, registrationId);

                // first rename for atomicity
                try {
                    if (!"OK".equals(j.rename(regIdKey, delRegIdKey))) {
                        return null;
                    }
                } catch (JedisDataException e) {
                    // the Jedis library raises an exception (ERR no such key)
                    // instead of returning a status code
                    return null;
                }

                Collection<Observation> removed = new ArrayList<>();

                // fetch all observations by token
                for (byte[] token : j.lrange(delRegIdKey, 0, -1)) {
                    byte[] obs = j.get(toKey(OBS_TKN, token));
                    if (obs != null) {
                        removed.add(deserialize(obs));
                    }
                    j.del(toKey(OBS_TKN, token));
                }
                j.del(delRegIdKey);

                return removed;

            } finally {
                releaseLock(j, registrationId, lockValue);
            }
        }
    }

    @Override
    public Observation get(byte[] token) {
        try (Jedis j = pool.getResource()) {
            byte[] obs = j.get(toKey(OBS_TKN, token));
            if (obs == null) {
                return null;
            } else {
                return deserialize(obs);
            }
        }
    }

    @Override
    public Collection<Observation> getByRegistrationId(String regId) {
        Collection<Observation> result = new ArrayList<>();
        try (Jedis j = pool.getResource()) {
            for (byte[] token : j.lrange(toKey(OBS_REG, regId), 0, -1)) {
                byte[] obs = j.get(toKey(OBS_TKN, token));
                if (obs != null) {
                    result.add(deserialize(obs));
                }
            }
        }
        return result;
    }

    @Override
    public void setContext(byte[] token, CorrelationContext correlationContext) {
        // TODO handle security context
    }

    private byte[] toKey(byte[] prefix, byte[] key) {
        byte[] result = new byte[prefix.length + key.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(key, 0, result, prefix.length, key.length);
        return result;
    }

    private byte[] toKey(String prefix, String registrationID) {
        return (prefix + registrationID).getBytes();
    }

    private byte[] serialize(Observation obs) {
        return ObservationSerDes.serialize(obs);
    }

    private Observation deserialize(byte[] data) {
        return ObservationSerDes.deserialize(data);
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

    private byte[] acquireLock(Jedis j, String registrationId) {
        byte[] lockKey = toKey(LOCK_REG, registrationId);
        long start = System.currentTimeMillis();

        byte[] randomLockValue = new byte[10];
        RND.nextBytes(randomLockValue);

        // setnx with a 500ms expiration
        while (!"OK".equals(j.set(lockKey, randomLockValue, NX_OPTION, PX_OPTION, 500))) {
            if (System.currentTimeMillis() - start > 5_000L)
                throw new IllegalStateException("Could not acquire the lock to access observations from redis");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        return randomLockValue;
    }

    private void releaseLock(Jedis j, String registrationId, byte[] lockValue) {
        if (lockValue != null) {
            byte[] lockKey = toKey(LOCK_REG, registrationId);
            if (Arrays.equals(j.get(lockKey), lockValue)) {
                j.del(lockKey);
            }
        }
    }
}
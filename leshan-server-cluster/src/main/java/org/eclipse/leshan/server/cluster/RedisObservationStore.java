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

import static org.eclipse.leshan.util.Charsets.UTF_8;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.elements.CorrelationContext;
import org.eclipse.leshan.server.californium.impl.CoapRequestBuilder;
import org.eclipse.leshan.server.californium.impl.LwM2mObservationStore;
import org.eclipse.leshan.server.cluster.serialization.ObservationSerDes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * An implementation of the Californium {@link ObservationStore} storing {@link Observation} in a Redis store.
 * 
 * Observations are stored using the token as primary key and a secondary index based on the registration Id.
 */
public class RedisObservationStore implements LwM2mObservationStore {

    private final Logger LOG = LoggerFactory.getLogger(RedisObservationStore.class);

    private final Pool<Jedis> pool;

    // Redis key prefixes
    private static final byte[] OBS_TKN = "OBS#TKN#".getBytes(UTF_8);
    private static final String OBS_REG = "OBS#REG#";
    private static final String LOCK_REG = "LOCK#REG#";

    public RedisObservationStore(Pool<Jedis> pool) {
        this.pool = pool;
    }

    @Override
    public void add(Observation obs) {
        this.validateObservation(obs);
        String registrationId = getRegistrationId(obs);

        try (Jedis j = pool.getResource()) {
            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_REG, registrationId);
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                byte[] previousValue = j.getSet(toKey(OBS_TKN, obs.getRequest().getToken()), serialize(obs));

                // secondary index to get the list by registrationId
                j.lpush(toKey(OBS_REG, registrationId), obs.getRequest().getToken());

                // log any collisions
                if (previousValue != null && previousValue.length != 0) {
                    Observation previousObservation = deserialize(previousValue);
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

            Observation obs = deserialize(serializedObs);
            String registrationId = getRegistrationId(obs);

            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_REG, registrationId);
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                if (j.del(tokenKey) > 0L) {
                    j.lrem(toKey(OBS_REG, registrationId), 0, token);
                }

            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Collection<Observation> removeAll(String registrationId) {
        try (Jedis j = pool.getResource()) {

            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_REG, registrationId);
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                Collection<Observation> removed = new ArrayList<>();
                byte[] regIdKey = toKey(OBS_REG, registrationId);

                // fetch all observations by token
                for (byte[] token : j.lrange(regIdKey, 0, -1)) {
                    byte[] obs = j.get(toKey(OBS_TKN, token));
                    if (obs != null) {
                        removed.add(deserialize(obs));
                    }
                    j.del(toKey(OBS_TKN, token));
                }
                j.del(regIdKey);

                return removed;

            } finally {
                RedisLock.release(j, lockKey, lockValue);
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

}
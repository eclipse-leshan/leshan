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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.californium.elements.CorrelationContext;
import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.impl.CoapRequestBuilder;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.cluster.serialization.ClientSerDes;
import org.eclipse.leshan.server.cluster.serialization.ObservationSerDes;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.util.Pool;

public class RedisRegistrationStore implements CaliforniumRegistrationStore {

    private static final Logger LOG = LoggerFactory.getLogger(RedisClientRegistry.class);

    private static final String EP_CLIENT = "EP#CLIENT#";
    private static final String REG_EP = "REG#EP#";
    private static final String LOCK_EP = "LOCK#EP#";

    // Redis key prefixes
    private static final byte[] OBS_TKN = "OBS#TKN#".getBytes(UTF_8);
    private static final String OBS_REG = "OBS#REG#";

    private final Pool<Jedis> pool;

    public RedisRegistrationStore(Pool<Jedis> p) {
        this.pool = p;
    }

    @Override
    public Client addRegistration(Client registration) {
        try (Jedis j = pool.getResource()) {
            byte[] lockValue = null;
            byte[] lockKey = toLockKey(registration.getEndpoint());

            try {
                lockValue = RedisLock.acquire(j, lockKey);

                byte[] k = toEndpointKey(registration.getEndpoint());
                byte[] old = j.getSet(k, serializeReg(registration));

                // secondary index
                byte[] idx = toRegKey(registration.getRegistrationId());
                j.set(idx, registration.getEndpoint().getBytes(UTF_8));

                if (old != null) {
                    Client oldRegistration = deserializeReg(old);
                    return oldRegistration;
                }

                return null;
            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Client updateRegistration(ClientUpdate update) {
        try (Jedis j = pool.getResource()) {

            // fetch the client ep by registration ID index
            byte[] ep = j.get(toRegKey(update.getRegistrationId()));
            if (ep == null) {
                return null;
            }

            // fetch the client
            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }

            Client r = deserializeReg(data);

            byte[] lockValue = null;
            byte[] lockKey = toLockKey(r.getEndpoint());
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                Client clientUpdated = update.updateClient(r);

                // store the new client
                j.set(toEndpointKey(clientUpdated.getEndpoint()), serializeReg(clientUpdated));

                return clientUpdated;

            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    @Override
    public Client getRegistration(String registrationId) {
        try (Jedis j = pool.getResource()) {
            byte[] ep = j.get(toRegKey(registrationId));
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
    public Client getRegistrationByEndpoint(String endpoint) {
        Validate.notNull(endpoint);
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get(toEndpointKey(endpoint));
            if (data == null) {
                return null;
            }
            Client r = deserializeReg(data);
            return r.isAlive() ? r : null;
        }
    }

    @Override
    public Collection<Client> getRegistrationByAdress(InetSocketAddress address) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Client> getAllRegistration() {
        try (Jedis j = pool.getResource()) {
            ScanParams params = new ScanParams().match(EP_CLIENT + "*").count(100);
            Collection<Client> list = new LinkedList<>();
            String cursor = "0";
            do {
                ScanResult<byte[]> res = j.scan(cursor.getBytes(), params);
                for (byte[] key : res.getResult()) {
                    byte[] element = j.get(key);
                    if (element != null) {
                        Client c = deserializeReg(element);
                        if (c.isAlive()) {
                            list.add(c);
                        }
                    }
                }
                cursor = res.getStringCursor();
            } while (!"0".equals(cursor));
            return list;
        }
    }

    @Override
    public Client removeRegistration(String registrationId) {
        try (Jedis j = pool.getResource()) {

            byte[] regKey = toRegKey(registrationId);

            // fetch the client ep by registration ID index
            byte[] ep = j.get(regKey);
            if (ep == null) {
                return null;
            }

            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }

            Client r = deserializeReg(data);
            deleteClient(j, r);

            return r;
        }
    }

    private void deleteClient(Jedis j, Client c) {
        byte[] lockValue = null;
        byte[] lockKey = toLockKey(c.getEndpoint());
        try {
            lockValue = RedisLock.acquire(j, lockKey);

            // delete all entries
            j.del(toRegKey(c.getRegistrationId()));
            j.del(toEndpointKey(c.getEndpoint()));

        } finally {
            RedisLock.release(j, lockKey, lockValue);
        }
    }

    private byte[] toRegKey(String registrationId) {
        return (REG_EP + registrationId).getBytes(UTF_8);
    }

    private byte[] toEndpointKey(String endpoint) {
        return (EP_CLIENT + endpoint).getBytes(UTF_8);
    }

    private byte[] toEndpointKey(byte[] endpoint) {
        return ByteArrayUtils.concatenate(EP_CLIENT.getBytes(UTF_8), endpoint);
    }

    private byte[] toLockKey(String endpoint) {
        return (LOCK_EP + endpoint).getBytes(UTF_8);
    }

    private byte[] serializeReg(Client client) {
        return ClientSerDes.bSerialize(client);
    }

    private Client deserializeReg(byte[] data) {
        return ClientSerDes.deserialize(data);
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
        Client registration = this.validateObservation(obs);

        try (Jedis j = pool.getResource()) {
            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, registration.getEndpoint());
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                byte[] previousValue = j.getSet(toKey(OBS_TKN, obs.getRequest().getToken()), serializeObs(obs));

                // secondary index to get the list by registrationId
                j.lpush(toKey(OBS_REG, registration.getRegistrationId()), obs.getRequest().getToken());

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
            String registrationId = getRegistrationId(obs);
            Client registration = getRegistration(registrationId);
            String endpoint = registration.getEndpoint();

            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, endpoint);
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

    public Collection<org.eclipse.californium.core.observe.Observation> removeAll(String registrationId) {
        try (Jedis j = pool.getResource()) {
            Client registration = getRegistration(registrationId);
            if (registration == null)
                return Collections.emptyList();
            String endpoint = registration.getEndpoint();


            byte[] lockValue = null;
            byte[] lockKey = toKey(LOCK_EP, endpoint);
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                Collection<org.eclipse.californium.core.observe.Observation> removed = new ArrayList<>();
                byte[] regIdKey = toKey(OBS_REG, registrationId);

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

            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    public Collection<org.eclipse.californium.core.observe.Observation> getByRegistrationId(String regId) {
        Collection<org.eclipse.californium.core.observe.Observation> result = new ArrayList<>();
        try (Jedis j = pool.getResource()) {
            for (byte[] token : j.lrange(toKey(OBS_REG, regId), 0, -1)) {
                byte[] obs = j.get(toKey(OBS_TKN, token));
                if (obs != null) {
                    result.add(deserializeObs(obs));
                }
            }
        }
        return result;
    }

    @Override
    public void setContext(byte[] token, CorrelationContext correlationContext) {
        // TODO Auto-generated method stub

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

    private byte[] serializeObs(org.eclipse.californium.core.observe.Observation obs) {
        return ObservationSerDes.serialize(obs);
    }

    private org.eclipse.californium.core.observe.Observation deserializeObs(byte[] data) {
        return ObservationSerDes.deserialize(data);
    }

    /* Retrieve the registrationId from the request context */
    private String getRegistrationId(org.eclipse.californium.core.observe.Observation observation) {
        return observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID);
    }

    private Client validateObservation(org.eclipse.californium.core.observe.Observation observation) {
        String registrationId = observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID);
        if (registrationId == null)
            throw new IllegalStateException("missing registrationId info in the request context");
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_LWM2M_PATH))
            throw new IllegalStateException("missing lwm2m path info in the request context");
        Client registration = getRegistration(registrationId);
        if (registration == null)
            throw new IllegalStateException("no registration for this Id");

        return registration;
    }

}

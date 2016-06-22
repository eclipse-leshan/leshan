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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.elements.CorrelationContext;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.californium.CaliforniumObservationRegistry;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * A registry storing observations in a Redis store.
 * 
 * Observations are stored using the token as primary key and a secondary index based on the registration Id.
 */
public class RedisObservationRegistry
        implements CaliforniumObservationRegistry, ObservationRegistry, NotificationListener {

    private final Logger LOG = LoggerFactory.getLogger(RedisObservationRegistry.class);

    private final ObservationStore observationStore;
    private final ClientRegistry clientRegistry;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mNodeDecoder decoder;
    private Endpoint nonSecureEndpoint;
    private Endpoint secureEndpoint;

    private final List<ObservationRegistryListener> listeners = new CopyOnWriteArrayList<>();

    private final Pool<Jedis> pool;

    // Redis key prefixes
    private static final byte[] OBS_TKN = "OBS#TKN#".getBytes();
    private static final String OBS_REG = "OBS#REG#";

    // observation field names (ser/des)
    private static final byte[] ID = "id".getBytes();
    private static final byte[] PATH = "path".getBytes();
    private static final byte[] REGID = "regid".getBytes();

    public RedisObservationRegistry(Pool<Jedis> pool, ClientRegistry clientRegistry, LwM2mModelProvider modelProvider,
            LwM2mNodeDecoder decoder) {
        this.pool = pool;
        this.modelProvider = modelProvider;
        this.clientRegistry = clientRegistry;
        this.observationStore = new RedisObservationStore(pool);
        this.decoder = decoder;
    }

    @Override
    public void addObservation(Observation observation) {
        try (Jedis j = pool.getResource()) {

            j.hmset(toTokenKey(observation.getId()), serialize(observation));
            j.sadd(toRegIdKey(observation.getRegistrationId()), observation.getId()); // secondary index

            for (ObservationRegistryListener listener : listeners) {
                listener.newObservation(observation);
            }
        }
    }

    @Override
    public void setNonSecureEndpoint(Endpoint endpoint) {
        nonSecureEndpoint = endpoint;
    }

    @Override
    public void setSecureEndpoint(Endpoint endpoint) {
        secureEndpoint = endpoint;
    }

    @Override
    public int cancelObservations(Client client) {
        // check registration id
        String registrationId = client.getRegistrationId();
        if (registrationId == null)
            return 0;

        Set<Observation> observations = getObservations(registrationId);
        for (Observation observation : observations) {
            cancelObservation(observation);
        }
        return observations.size();
    }

    @Override
    public int cancelObservations(Client client, String resourcepath) {
        // check registration id
        String registrationId = client.getRegistrationId();
        if (registrationId == null || resourcepath == null || resourcepath.isEmpty())
            return 0;

        Set<Observation> observations = getObservations(registrationId, resourcepath);
        for (Observation observation : observations) {
            cancelObservation(observation);
        }
        return observations.size();
    }

    @Override
    public void cancelObservation(Observation observation) {
        if (observation == null)
            return;

        if (secureEndpoint != null)
            secureEndpoint.cancelObservation(observation.getId());
        if (nonSecureEndpoint != null)
            nonSecureEndpoint.cancelObservation(observation.getId());

        try (Jedis j = pool.getResource()) {
            j.del(toTokenKey(observation.getId()));

            byte[] regIdKey = toRegIdKey(observation.getRegistrationId());
            j.srem(regIdKey, observation.getId());
            if (j.scard(regIdKey) == 0) {
                // remove regId entry if no more observations
                j.del(regIdKey);
            }
        }

        for (ObservationRegistryListener listener : listeners) {
            listener.cancelled(observation);
        }
    }

    @Override
    public Set<Observation> getObservations(Client client) {
        return getObservations(client.getRegistrationId());
    }

    private Set<Observation> getObservations(String registrationId) {
        if (registrationId == null)
            return Collections.emptySet();

        Set<Observation> result = new HashSet<Observation>();

        try (Jedis j = pool.getResource()) {
            Set<byte[]> tokens = j.smembers(toRegIdKey(registrationId));
            if (tokens != null && !tokens.isEmpty()) {
                for (byte[] token : tokens) {
                    Observation obs = deserialize(j.hgetAll(toTokenKey(token)));
                    if (obs != null) {
                        result.add(obs);
                    }
                }
            }
        }
        return result;
    }

    private Set<Observation> getObservations(String registrationId, String resourcePath) {
        if (registrationId == null || resourcePath == null)
            return Collections.emptySet();

        Set<Observation> result = new HashSet<Observation>();
        LwM2mPath lwPath = new LwM2mPath(resourcePath);
        for (Observation obs : this.getObservations(registrationId)) {
            if (lwPath.equals(obs.getPath())) {
                result.add(obs);
            }
        }
        return result;
    }

    private Observation getObservation(byte[] token) {
        if (token == null || token.length == 0)
            return null;

        try (Jedis j = pool.getResource()) {
            return deserialize(j.hgetAll(toTokenKey(token)));
        }
    }

    public ObservationStore getObservationStore() {
        return observationStore;
    }

    @Override
    public void addListener(ObservationRegistryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ObservationRegistryListener listener) {
        listeners.remove(listener);
    }

    private byte[] toTokenKey(byte[] token) {
        byte[] key = new byte[OBS_TKN.length + token.length];
        System.arraycopy(OBS_TKN, 0, key, 0, OBS_TKN.length);
        System.arraycopy(token, 0, key, OBS_TKN.length, token.length);
        return key;
    }

    private byte[] toRegIdKey(String registrationID) {
        return (OBS_REG + registrationID).getBytes();
    }

    // ********* Serialization/De-serialization **********

    private Map<byte[], byte[]> serialize(Observation observation) {
        Map<byte[], byte[]> fields = new HashMap<>();
        fields.put(ID, observation.getId());
        fields.put(PATH, observation.getPath().toString().getBytes());
        fields.put(REGID, observation.getRegistrationId().getBytes());
        return fields;
    }

    private Observation deserialize(Map<byte[], byte[]> fields) {
        if (fields == null) {
            return null;
        }
        byte[] id = fields.get(ID);
        if (id == null) {
            return null;
        }
        byte[] path = fields.get(PATH);
        if (path == null) {
            return null;
        }
        byte[] regId = fields.get(REGID);
        if (regId == null) {
            return null;
        }
        return new Observation(id, new String(regId), new LwM2mPath(new String(path)));
    }

    // ********** NotificationListener interface **********

    @Override
    public void onNotification(Request coapRequest, Response coapResponse) {
        if (listeners.isEmpty())
            return;

        if (coapResponse.getCode() == CoAP.ResponseCode.CHANGED
                || coapResponse.getCode() == CoAP.ResponseCode.CONTENT) {
            try {
                // get observation for this request
                Observation observation = this.getObservation(coapResponse.getToken());
                if (observation == null)
                    return;

                // get client for this registration ID
                Client client = clientRegistry.findByRegistrationId(observation.getRegistrationId());
                if (client == null)
                    // TODO Should we clean registrationIDs maps ?
                    return;

                // get model for this client
                LwM2mModel model = modelProvider.getObjectModel(client);

                // decode response
                LwM2mNode content = decoder.decode(coapResponse.getPayload(),
                        ContentFormat.fromCode(coapResponse.getOptions().getContentFormat()), observation.getPath(),
                        model);

                // notify all listeners
                for (ObservationRegistryListener listener : listeners) {
                    listener.newValue(observation, content);
                }
            } catch (InvalidValueException e) {
                String msg = String.format("[%s] ([%s])", e.getMessage(), e.getPath().toString());
                LOG.debug(msg);
            }
        }
    }

    /**
     * An {@link ObservationStore} storing requests in a Redis store.
     * 
     * The CoAP request is serialized using the Californium network serialization (see {@link DataParser} and
     * {@link DataSerializer})
     */
    private static class RedisObservationStore implements ObservationStore {

        private final Pool<Jedis> pool;

        public RedisObservationStore(Pool<Jedis> pool) {
            this.pool = pool;
        }

        @Override
        public void add(org.eclipse.californium.core.observe.Observation obs) {
            try (Jedis j = pool.getResource()) {
                j.set(obs.getRequest().getToken(), DataSerializer.serializeRequest(obs.getRequest()));
            }
        }

        @Override
        public void remove(byte[] token) {
            try (Jedis j = pool.getResource()) {
                j.del(token);
            }
        }

        @Override
        public org.eclipse.californium.core.observe.Observation get(byte[] token) {
            try (Jedis j = pool.getResource()) {
                byte[] req = j.get(token);
                if (req == null) {
                    return null;
                } else {
                    // TODO handle security context
                    return new org.eclipse.californium.core.observe.Observation(new DataParser(req).parseRequest(),
                            null);
                }
            }
        }

        @Override
        public void setContext(byte[] token, CorrelationContext correlationContext) {
            // TODO handle security context
        }
    }

}

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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.demo.cluster.serialization.ClientSerDes;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.util.Pool;

/**
 * A client registry storing registration in Redis.
 * 
 * the main key is the client end-point and the registration key is used as a secondary index
 */
public class RedisClientRegistry implements ClientRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(RedisClientRegistry.class);

    private static final String EP_CLIENT = "EP#CLIENT#";

    private static final String EXPIRE = "EXPIRE#";

    private static final String REG_EP = "REG#EP#";

    private final Pool<Jedis> pool;

    private final List<ClientRegistryListener> listeners = new CopyOnWriteArrayList<>();

    public RedisClientRegistry(Pool<Jedis> p) {
        this.pool = p;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String pattern = "__keyspace@0__:" + EXPIRE + EP_CLIENT;
                do {
                    try (Jedis j = pool.getResource()) {
                        // TODO not sure this is a good idea to set config here
                        j.configSet("notify-keyspace-events", "Kx");
                        j.psubscribe(new JedisPubSub() {
                            @Override
                            public void onPMessage(String pattern, String channel, String message) {
                                String endpoint = channel.substring(pattern.length() - 1);
                                handleRegistrationExpiration(endpoint);
                            }
                        }, pattern + "*");
                    } catch (Throwable e) {
                        LOG.warn("Redis PSUBSCRIBE interrupted.", e);
                    }

                    // wait & re-launch
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    LOG.warn("Relaunch Redis PSUBSCRIBE.");
                } while (true);
            }
        }).start();
    }

    @Override
    public Client get(String endpoint) {
        Validate.notNull(endpoint);
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get((EP_CLIENT + endpoint).getBytes());
            if (data == null) {
                return null;
            } else {
                Client c = deserialize(data);
                return c.isAlive() ? c : null;
            }
        }
    }

    @Override
    public Collection<Client> allClients() {
        try (Jedis j = pool.getResource()) {
            ScanParams params = new ScanParams().match(EP_CLIENT + "*").count(100);
            Collection<Client> list = new LinkedList<>();
            String cursor = "0";
            do {
                ScanResult<byte[]> res = j.scan(cursor.getBytes(), params);
                for (byte[] key : res.getResult()) {
                    byte[] element = j.get(key);
                    Client c = deserialize(element);
                    if (c.isAlive()) {
                        list.add(c);
                    }
                }
                cursor = res.getStringCursor();
            } while (!"0".equals(cursor));
            return list;
        }
    }

    @Override
    public boolean registerClient(Client client) {
        byte[] data = serialize(client);
        try (Jedis j = pool.getResource()) {
            byte[] k = (EP_CLIENT + client.getEndpoint()).getBytes();
            byte[] old = j.getSet(k, data);

            // create expire key
            j.setex(ByteArrayUtils.concatenate(EXPIRE.getBytes(), k), client.getLifeTimeInSec().intValue(),
                    new byte[0]);
            j.expire(k, client.getLifeTimeInSec().intValue() + 60);

            // secondary index
            byte[] idx = (REG_EP + client.getRegistrationId()).getBytes();
            j.set(idx, client.getEndpoint().getBytes());

            j.expire(idx, client.getLifeTimeInSec().intValue());

            if (old != null) {
                Client oldClient = deserialize(old);
                for (ClientRegistryListener l : listeners) {
                    l.unregistered(oldClient);
                }
            }
            for (ClientRegistryListener l : listeners) {
                l.registered(client);
            }
            return true;
        }
    }

    @Override
    public Client updateClient(ClientUpdate update) {
        try (Jedis j = pool.getResource()) {
            // fetch the client ep by registration ID index
            byte[] key = j.get((REG_EP + update.getRegistrationId()).getBytes());
            if (key == null) {
                return null;
            }

            // fetch the client
            byte[] data = j.get(ByteArrayUtils.concatenate(EP_CLIENT.getBytes(), key));
            if (data == null) {
                return null;
            }

            Client c = deserialize(data);
            Client clientUpdated = update.updateClient(c);
            // store the new client
            byte[] k = (EP_CLIENT + clientUpdated.getEndpoint()).getBytes();

            // update expiratation
            j.setex(ByteArrayUtils.concatenate(EXPIRE.getBytes(), k), c.getLifeTimeInSec().intValue(), new byte[0]);
            j.setex(k, clientUpdated.getLifeTimeInSec().intValue() + 60, serialize(clientUpdated));

            byte[] idx = (REG_EP + clientUpdated.getRegistrationId()).getBytes();

            j.expire(idx, clientUpdated.getLifeTimeInSec().intValue());

            // notify listener
            for (ClientRegistryListener l : listeners) {
                l.updated(update, clientUpdated);
            }
            return clientUpdated;
        }
    }

    @Override
    public Client deregisterClient(String registrationId) {
        try (Jedis j = pool.getResource()) {
            byte[] regKey = (REG_EP + registrationId).getBytes();
            byte[] delRegKey = ("TODELETE#" + REG_EP + registrationId).getBytes();

            // first rename for atomicity
            if (!"OK".equals(j.rename(regKey, delRegKey))) {
                return null;
            }

            // fetch the client ep by registration ID index
            byte[] key = j.get(delRegKey);
            if (key == null) {
                return null;
            }

            byte[] epKey = ByteArrayUtils.concatenate(EP_CLIENT.getBytes(), key);
            byte[] data = j.get(epKey);

            Client c = deserialize(data);

            // delete everything
            j.del(delRegKey);
            j.del(epKey);
            j.del((EXPIRE + key).getBytes());

            for (ClientRegistryListener l : listeners) {
                l.unregistered(c);
            }
            return c;
        }
    }

    @Override
    public Client findByRegistrationId(String registrationId) {
        try (Jedis j = pool.getResource()) {
            byte[] key = j.get((REG_EP + registrationId).getBytes());
            if (key == null) {
                return null;
            }
            byte[] data = j.get(ByteArrayUtils.concatenate(EP_CLIENT.getBytes(), key));

            return deserialize(data);
        }
    }

    private void handleRegistrationExpiration(final String endpoint) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (Jedis j = pool.getResource()) {
                    // rename to be able to do GET/DEL in an atomic way
                    String key = EP_CLIENT + endpoint;
                    byte[] atomicKey = ("TODELETE#" + key).getBytes();
                    if (!"OK".equals(j.rename(key.getBytes(), atomicKey)))
                        return;

                    // get the client
                    byte[] val = j.get(atomicKey);
                    Client c = deserialize(val);

                    // remove client
                    j.del(atomicKey);

                    // raise event
                    for (ClientRegistryListener listener : listeners) {
                        listener.unregistered(c);
                    }
                } catch (Throwable e) {
                    LOG.error("Unexpected exception pending registration expiration.", e);
                }
            }
        }).start();
    }

    @Override
    public void addListener(ClientRegistryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ClientRegistryListener listener) {
        listeners.remove(listener);
    }

    private byte[] serialize(Client client) {
        return ClientSerDes.bSerialize(client);
    }

    private Client deserialize(byte[] data) {
        return ClientSerDes.deserialize(data);
    }

}

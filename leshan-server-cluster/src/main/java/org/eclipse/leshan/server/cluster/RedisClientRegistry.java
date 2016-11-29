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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.cluster.serialization.ClientSerDes;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.util.Pool;

/**
 * A client registry storing registration in Redis.
 * <p>
 * The main key is the client end-point and the registration key is used as a secondary index.
 * </p>
 * <p>
 * Be aware of the limitations of this Redis implementation:
 * <ul>
 * <li>it is based on a simple lock mechanism which is not valid in case of distributed Redis (more information here:
 * http://redis.io/topics/distlock)</li>
 * <li>the registration expiration is very basic and not meant to be used in a production environment</li>
 * </ul>
 * <p>
 */
public class RedisClientRegistry implements ClientRegistry, Startable, Stoppable {

    private static final Logger LOG = LoggerFactory.getLogger(RedisClientRegistry.class);

    private static final String EP_CLIENT = "EP#CLIENT#";
    private static final String REG_EP = "REG#EP#";
    private static final String LOCK_EP = "LOCK#EP#";

    private final Pool<Jedis> pool;

    private final List<ClientRegistryListener> listeners = new CopyOnWriteArrayList<>();

    public RedisClientRegistry(Pool<Jedis> p) {
        this.pool = p;
    }

    @Override
    public Client get(String endpoint) {
        Validate.notNull(endpoint);
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get(toEndpointKey(endpoint));
            if (data == null) {
                return null;
            }
            Client c = deserialize(data);
            return c.isAlive() ? c : null;
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
                    if (element != null) {
                        Client c = deserialize(element);
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

    public boolean registerClient(Client client) {
        try (Jedis j = pool.getResource()) {
            byte[] lockValue = null;
            byte[] lockKey = toLockKey(client.getEndpoint());

            try {
                lockValue = RedisLock.acquire(j, lockKey);

                byte[] k = toEndpointKey(client.getEndpoint());
                byte[] old = j.getSet(k, serialize(client));

                // secondary index
                byte[] idx = toRegKey(client.getRegistrationId());
                j.set(idx, client.getEndpoint().getBytes(UTF_8));

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

            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    public Client updateClient(ClientUpdate update) {
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

            Client c = deserialize(data);

            byte[] lockValue = null;
            byte[] lockKey = toLockKey(c.getEndpoint());
            try {
                lockValue = RedisLock.acquire(j, lockKey);

                Client clientUpdated = update.updateClient(c);

                // store the new client
                j.set(toEndpointKey(clientUpdated.getEndpoint()), serialize(clientUpdated));

                // notify listener
                for (ClientRegistryListener l : listeners) {
                    l.updated(update, clientUpdated);
                }
                return clientUpdated;

            } finally {
                RedisLock.release(j, lockKey, lockValue);
            }
        }
    }

    public Client deregisterClient(String registrationId) {
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

            Client c = deserialize(data);
            deleteClient(j, c);

            return c;
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

            for (ClientRegistryListener l : listeners) {
                l.unregistered(c);
            }

        } finally {
            RedisLock.release(j, lockKey, lockValue);
        }
    }

    @Override
    public Client findByRegistrationId(String registrationId) {
        try (Jedis j = pool.getResource()) {
            byte[] ep = j.get(toRegKey(registrationId));
            if (ep == null) {
                return null;
            }
            byte[] data = j.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }

            return deserialize(data);
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
                ScanParams params = new ScanParams().match(EP_CLIENT + "*").count(100);
                String cursor = "0";
                do {
                    ScanResult<byte[]> res = j.scan(cursor.getBytes(), params);
                    for (byte[] key : res.getResult()) {
                        Client c = deserialize(j.get(key));
                        if (!c.isAlive()) {
                            deleteClient(j, c);
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

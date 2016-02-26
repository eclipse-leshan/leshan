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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.util.Validate;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.util.Pool;

/**
 * A client registry storing registration in Redis.
 * 
 * the main key is the client end-point and the registration key is used as a secondary index
 * 
 * TODO
 * 
 * - receive key expiration event (see http://redis.io/topics/notifications)
 */
public class RedisClientRegistry implements ClientRegistry {

    private static final String EP_CLIENT = "EP#CLIENT#";

    private static final String REG_EP = "REG#EP#";

    private final Pool<Jedis> pool;

    private final List<ClientRegistryListener> listeners = new CopyOnWriteArrayList<>();

    public RedisClientRegistry(Pool<Jedis> pool) {
        this.pool = pool;
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

            j.expire(k, client.getLifeTimeInSec().intValue());

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

            j.setex(k, clientUpdated.getLifeTimeInSec().intValue(), serialize(clientUpdated));

            byte[] idx = (REG_EP + clientUpdated.getRegistrationId()).getBytes();

            j.expire(idx, clientUpdated.getLifeTimeInSec().intValue());

            // notify listener
            for (ClientRegistryListener l : listeners) {
                l.updated(clientUpdated);
            }
            return clientUpdated;
        }
    }

    @Override
    public Client deregisterClient(String registrationId) {
        try (Jedis j = pool.getResource()) {
            // first rename for atomicity
            if (!"OK".equals(j.rename((REG_EP + registrationId).getBytes(),
                    ("TODELETE#" + REG_EP + registrationId).getBytes()))) {
                System.err.println("del meh");
                return null;
            }

            // fetch the client ep by registration ID index
            byte[] key = j.get(("TODELETE#" + REG_EP + registrationId).getBytes());
            if (key == null) {
                return null;
            }
            byte[] data = j.get(ByteArrayUtils.concatenate(EP_CLIENT.getBytes(), key));

            Client c = deserialize(data);

            // delete everything
            j.del(("TODELETE#" + REG_EP + registrationId).getBytes());
            j.del(ByteArrayUtils.concatenate(EP_CLIENT.getBytes(), key));
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

    private byte[] serialize(Client client) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(client);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Client is not serializable", e);
        }
    }

    private Client deserialize(byte[] data) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try (ObjectInputStream in = new ObjectInputStream(bis)) {
            return (Client) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Client is not deserializable", e);
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

}
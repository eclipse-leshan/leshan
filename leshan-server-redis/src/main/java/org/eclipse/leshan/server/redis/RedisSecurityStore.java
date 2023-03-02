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
 *******************************************************************************/
package org.eclipse.leshan.server.redis;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.oscore.OscoreIdentity;
import org.eclipse.leshan.server.redis.serialization.SecurityInfoSerDes;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.SecurityStoreListener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.Pool;

/**
 * A {@link SecurityStore} implementation based on Redis.
 * <p>
 * Security info are stored using the endpoint as primary key and a secondary index is created for endpoint lookup by
 * PSK identity.
 */
public class RedisSecurityStore implements EditableSecurityStore {

    private final String securityInfoByEndpointPrefix;
    private final String endpointByPskIdKey;
    private final Pool<Jedis> pool;

    private final List<SecurityStoreListener> listeners = new CopyOnWriteArrayList<>();

    public RedisSecurityStore(Pool<Jedis> pool) {
        this(new Builder(pool));
    }

    protected RedisSecurityStore(Builder builder) {
        this.pool = builder.pool;
        this.securityInfoByEndpointPrefix = builder.securityInfoByEndpointPrefix;
        this.endpointByPskIdKey = builder.endpointByPskIdKey;
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get((securityInfoByEndpointPrefix + endpoint).getBytes());
            if (data == null) {
                return null;
            } else {
                return deserialize(data);
            }
        }
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        try (Jedis j = pool.getResource()) {
            String ep = j.hget(endpointByPskIdKey, identity);
            if (ep == null) {
                return null;
            } else {
                byte[] data = j.get((securityInfoByEndpointPrefix + ep).getBytes());
                if (data == null) {
                    return null;
                } else {
                    return deserialize(data);
                }
            }
        }
    }

    @Override
    public SecurityInfo getByOscoreIdentity(OscoreIdentity pskIdentity) {
        // TODO OSCORE to be implemented
        return null;
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        try (Jedis j = pool.getResource()) {
            ScanParams params = new ScanParams().match(securityInfoByEndpointPrefix + "*").count(100);
            Collection<SecurityInfo> list = new LinkedList<>();
            String cursor = "0";
            do {
                ScanResult<byte[]> res = j.scan(cursor.getBytes(), params);
                for (byte[] key : res.getResult()) {
                    byte[] element = j.get(key);
                    list.add(deserialize(element));
                }
                cursor = res.getCursor();
            } while (!"0".equals(cursor));
            return list;
        }
    }

    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        byte[] data = serialize(info);
        try (Jedis j = pool.getResource()) {
            if (info.getPskIdentity() != null) {
                // populate the secondary index (security info by PSK id)
                String oldEndpoint = j.hget(endpointByPskIdKey, info.getPskIdentity());
                if (oldEndpoint != null && !oldEndpoint.equals(info.getEndpoint())) {
                    throw new NonUniqueSecurityInfoException(
                            "PSK Identity " + info.getPskIdentity() + " is already used");
                }
                j.hset(endpointByPskIdKey.getBytes(), info.getPskIdentity().getBytes(), info.getEndpoint().getBytes());
            }

            byte[] previousData = j.getSet((securityInfoByEndpointPrefix + info.getEndpoint()).getBytes(), data);
            SecurityInfo previous = previousData == null ? null : deserialize(previousData);
            String previousIdentity = previous == null ? null : previous.getPskIdentity();
            if (previousIdentity != null && !previousIdentity.equals(info.getPskIdentity())) {
                j.hdel(endpointByPskIdKey, previousIdentity);
            }

            return previous;
        }
    }

    @Override
    public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get((securityInfoByEndpointPrefix + endpoint).getBytes());

            if (data != null) {
                SecurityInfo info = deserialize(data);
                if (info.getPskIdentity() != null) {
                    j.hdel(endpointByPskIdKey.getBytes(), info.getPskIdentity().getBytes());
                }
                j.del((securityInfoByEndpointPrefix + endpoint).getBytes());
                for (SecurityStoreListener listener : listeners) {
                    listener.securityInfoRemoved(infosAreCompromised, info);
                }
                return info;
            }
        }
        return null;
    }

    private byte[] serialize(SecurityInfo secInfo) {
        return SecurityInfoSerDes.serialize(secInfo);
    }

    private SecurityInfo deserialize(byte[] data) {
        return SecurityInfoSerDes.deserialize(data);
    }

    @Override
    public void addListener(SecurityStoreListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SecurityStoreListener listener) {
        listeners.remove(listener);
    }

    /**
     * Class helping to build and configure a {@link RedisSecurityStore}.
     * <p>
     * By default, uses {@code SECSTORE#} prefix for all keys, {@code SEC#EP#} key prefix to find security info by
     * endpoint and {@code EP#PSKID} key to get the endpoint by PSK ID. Leshan v1.x used {@code SEC#EP#} and
     * {@code PSKID#SEC} keys for that accordingly.
     */
    public static class Builder {

        private final Pool<Jedis> pool;
        private String securityInfoByEndpointPrefix;
        private String endpointByPskIdKey;
        private String prefix;

        /**
         * Set the key prefix for security info lookup by endpoint.
         * <p>
         * Default value is {@literal SEC#EP#}. Should not be {@code null} or empty.
         */
        public Builder setSecurityInfoByEndpointPrefix(String securityInfoByEndpointPrefix) {
            this.securityInfoByEndpointPrefix = securityInfoByEndpointPrefix;
            return this;
        }

        /**
         * Set the key for endpoint lookup by PSK identity.
         * <p>
         * Default value is {@literal EP#PSKID}. Should not be {@code null} or empty.
         */
        public Builder setEndpointByPskIdKey(String endpointByPskIdKey) {
            this.endpointByPskIdKey = endpointByPskIdKey;
            return this;
        }

        /**
         * Set the prefix for all keys and prefixes including {@link #securityInfoByEndpointPrefix} and
         * {@link #endpointByPskIdKey}.
         * <p>
         * Default value is {@literal SECSTORE#}.
         */
        public Builder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder(Pool<Jedis> pool) {
            this.pool = pool;
            this.prefix = "SECSTORE#";
            this.securityInfoByEndpointPrefix = "SEC#EP#";
            this.endpointByPskIdKey = "EP#PSKID";
        }

        /**
         * Create the {@link RedisSecurityStore}.
         * <p>
         * Throws {@link IllegalArgumentException} when {@link #securityInfoByEndpointPrefix} or
         * {@link #endpointByPskIdKey} are not set or are equal to each other.
         */
        public RedisSecurityStore build() throws IllegalArgumentException {
            if (this.securityInfoByEndpointPrefix == null || this.securityInfoByEndpointPrefix.isEmpty()) {
                throw new IllegalArgumentException("securityInfoByEndpointPrefix should not be empty");
            }

            if (this.endpointByPskIdKey == null || this.endpointByPskIdKey.isEmpty()) {
                throw new IllegalArgumentException("endpointByPskIdKey should not be empty");
            }

            if (this.securityInfoByEndpointPrefix.equals(this.endpointByPskIdKey)) {
                throw new IllegalArgumentException(
                        "securityInfoByEndpointPrefix should not be equal to endpointByPskIdKey");
            }

            if (this.prefix != null) {
                this.securityInfoByEndpointPrefix = this.prefix + this.securityInfoByEndpointPrefix;
                this.endpointByPskIdKey = this.prefix + this.endpointByPskIdKey;
            }

            return new RedisSecurityStore(this);
        }
    }
}

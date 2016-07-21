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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.leshan.server.cluster.serialization.SecurityInfoSerDes;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityRegistry;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.util.Pool;

/**
 * A security registry storing values in a Redis store.
 * 
 * Only root CA (trust chain) used for validating X.509 certificate and raw public/private server keys are stored in
 * each node memory. Because they are static.
 * 
 * Security info are stored using the endpoint as primary key and a secondary index is created for psk-identity lookup.
 */
public class RedisSecurityRegistry implements SecurityRegistry {

    private static final String SEC_EP = "SEC#EP#";

    private static final String PSKID_SEC = "PSKID#SEC";

    private final Pool<Jedis> pool;

    private PublicKey serverPublicKey = null;

    private PrivateKey serverPrivateKey = null;

    private X509Certificate[] serverX509CertChain = null;

    private Certificate[] trustedCertificates = null;

    public RedisSecurityRegistry(Pool<Jedis> pool, PrivateKey serverPrivateKey, X509Certificate[] serverX509CertChain,
            Certificate[] trustedCertificates) {
        this.pool = pool;
        this.serverPrivateKey = serverPrivateKey;
        this.serverX509CertChain = serverX509CertChain;
        this.trustedCertificates = trustedCertificates;
    }

    public RedisSecurityRegistry(Pool<Jedis> pool, PrivateKey serverPrivateKey, PublicKey serverPublicKey) {
        this.pool = pool;
        this.serverPrivateKey = serverPrivateKey;
        this.serverPublicKey = serverPublicKey;
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get((SEC_EP + endpoint).getBytes());
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
            String ep = j.hget(PSKID_SEC, identity);
            if (ep == null) {
                return null;
            } else {
                byte[] data = j.get((SEC_EP + ep).getBytes());
                if (data == null) {
                    return null;
                } else {
                    return deserialize(data);
                }
            }
        }
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        try (Jedis j = pool.getResource()) {
            ScanParams params = new ScanParams().match(SEC_EP + "*").count(100);
            Collection<SecurityInfo> list = new LinkedList<>();
            String cursor = "0";
            do {
                ScanResult<byte[]> res = j.scan(cursor.getBytes(), params);
                for (byte[] key : res.getResult()) {
                    byte[] element = j.get(key);
                    list.add(deserialize(element));
                }
                cursor = res.getStringCursor();
            } while (!"0".equals(cursor));
            return list;
        }
    }

    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        byte[] data = serialize(info);
        try (Jedis j = pool.getResource()) {
            j.set((SEC_EP + info.getEndpoint()).getBytes(), data);
            if (info.getIdentity() != null) {
                // populate the secondary index (security info by PSK id)
                j.hset(PSKID_SEC.getBytes(), info.getIdentity().getBytes(), info.getEndpoint().getBytes());
            }
            return null;
        }
    }

    @Override
    public SecurityInfo remove(String endpoint) {
        try (Jedis j = pool.getResource()) {
            byte[] data = j.get((SEC_EP + endpoint).getBytes());

            if (data != null) {
                SecurityInfo info = deserialize(data);
                if (info.getIdentity() != null) {
                    j.hdel(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
                }
                j.del((SEC_EP + endpoint).getBytes());
                return info;
            }
        }
        return null;
    }

    @Override
    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }

    @Override
    public PrivateKey getServerPrivateKey() {
        return serverPrivateKey;
    }

    @Override
    public X509Certificate[] getServerX509CertChain() {
        return serverX509CertChain;
    }

    @Override
    public Certificate[] getTrustedCertificates() {
        return trustedCertificates;
    }

    private byte[] serialize(SecurityInfo secInfo) {
        return SecurityInfoSerDes.serialize(secInfo);
    }

    private SecurityInfo deserialize(byte[] data) {
        return SecurityInfoSerDes.deserialize(data);
    }
}
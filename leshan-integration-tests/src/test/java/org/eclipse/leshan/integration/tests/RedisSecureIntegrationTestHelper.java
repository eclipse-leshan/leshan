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
package org.eclipse.leshan.integration.tests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;

import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.cluster.RedisRegistrationStore;
import org.eclipse.leshan.server.cluster.RedisSecurityStore;
import org.eclipse.leshan.server.model.StaticModelProvider;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

public class RedisSecureIntegrationTestHelper extends SecureIntegrationTestHelper {
    @Override
    public void createServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        StaticModelProvider modelProvider = new StaticModelProvider(createObjectModels());
        builder.setObjectModelProvider(modelProvider);
        DefaultLwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalSecureAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

        // Create redis store
        String redisURI = System.getenv("REDIS_URI");
        if (redisURI == null)
            redisURI = "";
        Pool<Jedis> jedis = new JedisPool(redisURI);
        builder.setRegistrationStore(new RedisRegistrationStore(jedis));
        builder.setSecurityStore(new RedisSecurityStore(jedis));

        // Build server !
        server = builder.build();
        // monitor client registration
        resetLatch();
        server.getRegistrationService().addListener(new RegistrationListener() {
            @Override
            public void updated(RegistrationUpdate update, Registration updatedRegistration) {
                if (updatedRegistration.getEndpoint().equals(getCurrentEndpoint())) {
                    updateLatch.countDown();
                }
            }

            @Override
            public void unregistered(Registration registration) {
                if (registration.getEndpoint().equals(getCurrentEndpoint())) {
                    deregisterLatch.countDown();
                }
            }

            @Override
            public void registered(Registration registration) {
                if (registration.getEndpoint().equals(getCurrentEndpoint())) {
                    last_registration = registration;
                    registerLatch.countDown();
                }
            }
        });
    }

    @Override
    public void createServerWithRPK() {
        throw new UnsupportedOperationException("Not implemeneted");
    }

    @Override
    public void createServerWithX509Cert(Certificate[] trustedCertificates) {
        throw new UnsupportedOperationException("Not implemeneted");
    }

}

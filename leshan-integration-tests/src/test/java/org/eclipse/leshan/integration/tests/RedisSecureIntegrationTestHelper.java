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

import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.cluster.RedisRegistrationStore;
import org.eclipse.leshan.server.cluster.RedisSecurityStore;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

public class RedisSecureIntegrationTestHelper extends SecureIntegrationTestHelper {

    @Override
    protected LeshanServerBuilder createServerBuilder() {
        LeshanServerBuilder builder = super.createServerBuilder();

        // Create redis store
        String redisURI = System.getenv("REDIS_URI");
        if (redisURI == null)
            redisURI = "";
        Pool<Jedis> jedis = new JedisPool(redisURI);
        builder.setRegistrationStore(new RedisRegistrationStore(jedis));
        securityStore = new RedisSecurityStore(jedis);
        builder.setSecurityStore(securityStore);

        return builder;
    }
}

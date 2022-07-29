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
package org.eclipse.leshan.integration.tests.util;

import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

public class RedisIntegrationTestHelper extends IntegrationTestHelper {
    @Override
    protected LeshanServerBuilder createServerBuilder() {
        LeshanServerBuilder builder = super.createServerBuilder();

        // Create redis store
        Pool<Jedis> jedis = createJedisPool();
        builder.setRegistrationStore(new RedisRegistrationStore(jedis));

        return builder;
    }

    public Pool<Jedis> createJedisPool() {
        String redisURI = System.getenv("REDIS_URI");
        if (redisURI != null && !redisURI.isEmpty()) {
            return new JedisPool(redisURI);
        } else {
            return new JedisPool();
        }
    }
}

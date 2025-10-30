/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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

import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.servers.security.SecurityStore;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

public class RedisTestUtil {
    public static Pool<Jedis> createJedisPool() {
        String redisURI = System.getenv("REDIS_URI");
        if (redisURI != null && !redisURI.isEmpty()) {
            return new JedisPool(redisURI);
        } else {
            return new JedisPool();
        }
    }

    public static RedisRegistrationStore createRedisRegistrationStore() {
        return new RedisRegistrationStore.Builder(RedisTestUtil.createJedisPool()) //
                .setPrefix("LESHAN_TEST_REGSTORE#") //
                .build();
    }

    public static SecurityStore createRedisSecurityStore() {
        return new RedisSecurityStore.Builder(RedisTestUtil.createJedisPool()).setPrefix("LESHAN_TEST_SECSTORE#")
                .build();
    }
}

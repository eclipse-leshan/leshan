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

import java.util.Arrays;
import java.util.Random;

import redis.clients.jedis.Jedis;

/**
 * Utility class providing locking methods based on the Redis SETNX primitive (see
 * http://redis.io/topics/distlock#correct-implementation-with-a-single-instance for more information).
 */
public class RedisLock {

    private static final byte[] NX_OPTION = "NX".getBytes(UTF_8); // set the key if it does not already exist
    private static final byte[] PX_OPTION = "PX".getBytes(UTF_8); // expire time in millisecond

    private static final Random RND = new Random();

    /**
     * Acquires a lock for the given key.
     * 
     * @param j a Redis connection
     * @param lockKey the key to use as lock
     * @return a lock value that must be used to release the lock.
     */
    public static byte[] acquire(Jedis j, byte[] lockKey) {
        long start = System.currentTimeMillis();

        byte[] randomLockValue = new byte[10];
        RND.nextBytes(randomLockValue);

        // setnx with a 500ms expiration
        while (!"OK".equals(j.set(lockKey, randomLockValue, NX_OPTION, PX_OPTION, 500))) {
            if (System.currentTimeMillis() - start > 5_000L)
                throw new IllegalStateException("Could not acquire a lock from redis");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        return randomLockValue;
    }

    /**
     * Releases a lock for a given key and value.
     * 
     * @param j a Redis connection
     * @param lockKey the locked key
     * @param lockValue the value returned when the lock was acquired
     */
    public static void release(Jedis j, byte[] lockKey, byte[] lockValue) {
        if (lockValue != null) {
            if (Arrays.equals(j.get(lockKey), lockValue)) {
                j.del(lockKey);
            }
        }
    }

}

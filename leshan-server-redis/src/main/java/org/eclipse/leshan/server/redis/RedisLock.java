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

import java.util.Arrays;
import java.util.Random;

import org.eclipse.leshan.core.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.SetParams;

/**
 * Utility class providing locking methods based on the Redis SETNX primitive (see
 * http://redis.io/topics/distlock#correct-implementation-with-a-single-instance for more information).
 * 
 * @deprecated use a {@link SingleInstanceJedisLock} instead or any {@link JedisLock} implementation.
 */
@Deprecated
public class RedisLock {
    private static final Logger LOG = LoggerFactory.getLogger(RedisLock.class);

    private static final Random RND = new Random();
    private static final int LOCK_EXP = 500; // in ms

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
        while (!"OK".equals(j.set(lockKey, randomLockValue, SetParams.setParams().nx().px(LOCK_EXP)))) {
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
            // Watch the key to remove.
            j.watch(lockKey);

            byte[] prevousLockValue = j.get(lockKey);
            // Delete the key if needed.
            if (Arrays.equals(prevousLockValue, lockValue)) {
                // Try to delete the key
                Transaction transaction = j.multi();
                transaction.del(lockKey);
                boolean succeed = transaction.exec() != null;
                if (!succeed) {
                    LOG.warn(
                            "Failed to release lock for key {}/{}, meaning the key probably expired because of acquiring the lock for too long (more than {}ms)",
                            new String(lockKey), Hex.encodeHexString(lockValue), LOCK_EXP);
                }
            } else {
                // the key must not be deleted.
                LOG.warn(
                        "Nothing to release for key {}/{}, meaning the key probably expired because of acquiring the lock for too long (more than {}ms)",
                        new String(lockKey), Hex.encodeHexString(lockValue), LOCK_EXP);
                j.unwatch();
            }
        } else {
            LOG.warn("Trying to release a lock for {} with a null value", new String(lockKey));
        }
    }
}

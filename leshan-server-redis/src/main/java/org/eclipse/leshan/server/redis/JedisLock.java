/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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

import redis.clients.jedis.Jedis;

/**
 * This interface define the API of a Redis Lock based on Jedis library.
 * 
 * @since 1.1
 */
public interface JedisLock {

    /**
     * Acquires a lock for the given key.
     * 
     * @param j a Redis connection
     * @param lockKey the key to use as lock
     * @return a lock value that must be used to release the lock.
     */
    byte[] acquire(Jedis j, byte[] lockKey);

    /**
     * Releases a lock for a given key and value.
     * 
     * @param j a Redis connection
     * @param lockKey the locked key
     * @param lockValue the value returned when the lock was acquired
     */
    void release(Jedis j, byte[] lockKey, byte[] lockValue);
}

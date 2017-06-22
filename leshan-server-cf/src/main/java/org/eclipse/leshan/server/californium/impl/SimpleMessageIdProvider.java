/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.californium.core.network.MessageIdProvider;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.elements.util.LeastRecentlyUsedCache;

public class SimpleMessageIdProvider implements MessageIdProvider {

    private static final int TOTAL_NB_OF_MIDS = 1 << 16;

    // a cache keeping track of the last use mid for each peer address
    private final LeastRecentlyUsedCache<InetSocketAddress, AtomicInteger> mids;

    private final NetworkConfig config;

    private static final Random RDM = new Random();

    /**
     * Creates a new provider for configuration values.
     * 
     * @param config the configuration to use.
     * @throws NullPointerException if the config is {@code null}.
     */
    public SimpleMessageIdProvider(final NetworkConfig config) {
        if (config == null) {
            throw new NullPointerException("Config must not be null");
        }
        this.config = config;
        mids = new LeastRecentlyUsedCache<>(config.getInt(NetworkConfig.Keys.MAX_ACTIVE_PEERS, 150000),
                config.getLong(NetworkConfig.Keys.MAX_PEER_INACTIVITY_PERIOD, 10 * 60L)); // 10 minutes
    }

    @Override
    public synchronized int getNextMessageId(final InetSocketAddress destination) {
        AtomicInteger lastMid = mids.get(destination);
        if (lastMid == null) {
            // initialize a messageId
            if (config.getBoolean(NetworkConfig.Keys.USE_RANDOM_MID_START)) {
                lastMid = new AtomicInteger(RDM.nextInt(TOTAL_NB_OF_MIDS));
            } else {
                lastMid = new AtomicInteger(0);
            }
            if (!mids.put(destination, lastMid)) {
                // we have reached the maximum number of active peers
                // TODO: throw an exception?
                return -1;
            }
        } else {
            if (lastMid.get() + 1 >= TOTAL_NB_OF_MIDS) {
                lastMid.set(0);
            } else {
                lastMid.incrementAndGet();
            }
        }

        return lastMid.get();
    }

}

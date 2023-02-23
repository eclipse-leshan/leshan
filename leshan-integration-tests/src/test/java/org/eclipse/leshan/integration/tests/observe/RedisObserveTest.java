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
package org.eclipse.leshan.integration.tests.observe;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.RedisTestUtil;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

public class RedisObserveTest extends ObserveTest {

    @Override
    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        LeshanTestServerBuilder builder = super.givenServerUsing(givenProtocol);

        // Create redis store
        Pool<Jedis> jedis = RedisTestUtil.createJedisPool();
        // TODO use custom key when https://github.com/eclipse/leshan/issues/1249 will be available
        builder.setRegistrationStore(new RedisRegistrationStore(jedis));

        return builder;
    }
}

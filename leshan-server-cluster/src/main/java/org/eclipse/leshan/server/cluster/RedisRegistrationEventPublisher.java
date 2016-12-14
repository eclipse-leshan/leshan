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

import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.cluster.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.cluster.serialization.RegistrationUpdateSerDes;

import com.eclipsesource.json.JsonObject;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * A Registration registry Listener which publish registration event on Redis channel.
 */
public class RedisRegistrationEventPublisher implements RegistrationListener {

    private static String REGISTER_EVENT = "LESHAN_REG_NEW";
    private static String UPDATE_EVENT = "LESHAN_REG_UP";
    private static String DEREGISTER_EVENT = "LESHAN_REG_DEL";
    private Pool<Jedis> pool;

    public RedisRegistrationEventPublisher(Pool<Jedis> p) {
        this.pool = p;
    }

    @Override
    public void registered(Registration registration) {
        String payload = RegistrationSerDes.sSerialize(registration);
        try (Jedis j = pool.getResource()) {
            j.publish(REGISTER_EVENT, payload);
        }
    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedRegistration) {
        JsonObject value = new JsonObject();
        value.add("regUpdate", RegistrationUpdateSerDes.jSerialize(update));
        value.add("regUpdated", RegistrationSerDes.jSerialize(updatedRegistration));

        try (Jedis j = pool.getResource()) {
            j.publish(UPDATE_EVENT, value.toString());
        }
    }

    @Override
    public void unregistered(Registration registration) {
        String payload = RegistrationSerDes.sSerialize(registration);
        try (Jedis j = pool.getResource()) {
            j.publish(DEREGISTER_EVENT, payload);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.leshan.client.exchange.LwM2mCallbackExchange;
import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.exchange.aggregate.AggregatedLwM2mExchange;
import org.eclipse.leshan.client.exchange.aggregate.LwM2mObjectReadResponseAggregator;
import org.eclipse.leshan.client.exchange.aggregate.LwM2mResponseAggregator;
import org.eclipse.leshan.client.response.CreateResponse;
import org.eclipse.leshan.client.response.DeleteResponse;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.client.response.WriteResponse;

public class LwM2mClientObject extends LwM2mClientNode {

    private final LwM2mClientObjectDefinition definition;
    private final AtomicInteger instanceCounter;
    private final Map<Integer, LwM2mClientObjectInstance> instances;

    public LwM2mClientObject(final LwM2mClientObjectDefinition definition) {
        this.definition = definition;
        this.instanceCounter = new AtomicInteger(0);
        this.instances = new ConcurrentHashMap<>();
    }

    public LwM2mClientObjectInstance createMandatoryInstance() {
        LwM2mClientObjectInstance instance = createNewInstance(false, 0);
        instance.createMandatory();
        return instance;
    }

    public void createInstance(final LwM2mCallbackExchange<LwM2mClientObjectInstance> exchange) {
        if (instanceCounter.get() >= 1 && definition.isSingle()) {
            exchange.respond(CreateResponse.invalidResource());
        }

        final LwM2mClientObjectInstance instance = createNewInstance(exchange.hasObjectInstanceId(),
                exchange.getObjectInstanceId());
        exchange.setNode(instance);
        instance.createInstance(exchange);
    }

    @Override
    public void read(LwM2mExchange exchange) {
        final Collection<LwM2mClientObjectInstance> instances = this.instances.values();

        if (instances.isEmpty()) {
            exchange.respond(ReadResponse.success(new byte[0]));
            return;
        }

        final LwM2mResponseAggregator aggr = new LwM2mObjectReadResponseAggregator(exchange, instances.size());
        for (final LwM2mClientObjectInstance inst : instances) {
            inst.read(new AggregatedLwM2mExchange(aggr, inst.getId()));
        }
    }

    @Override
    public void write(LwM2mExchange exchange) {
        exchange.respond(WriteResponse.notAllowed());
    }

    private LwM2mClientObjectInstance createNewInstance(boolean hasObjectInstanceId, int objectInstanceId) {
        final int newInstanceId = getNewInstanceId(hasObjectInstanceId, objectInstanceId);
        final LwM2mClientObjectInstance instance = new LwM2mClientObjectInstance(newInstanceId, this, definition);
        return instance;
    }

    public void onSuccessfulCreate(final LwM2mClientObjectInstance instance) {
        instances.put(instance.getId(), instance);
    }

    private int getNewInstanceId(boolean hasObjectInstanceId, int objectInstanceId) {
        if (hasObjectInstanceId) {
            return objectInstanceId;
        } else {
            return instanceCounter.getAndIncrement();
        }
    }

    public void delete(LwM2mExchange exchange, int id) {
        instances.remove(id);
        exchange.respond(DeleteResponse.success());
    }

}
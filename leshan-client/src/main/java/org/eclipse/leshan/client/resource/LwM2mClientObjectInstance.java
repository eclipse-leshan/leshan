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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.exchange.aggregate.AggregatedLwM2mExchange;
import org.eclipse.leshan.client.exchange.aggregate.LwM2mObjectInstanceCreateResponseAggregator;
import org.eclipse.leshan.client.exchange.aggregate.LwM2mObjectInstanceReadResponseAggregator;
import org.eclipse.leshan.client.exchange.aggregate.LwM2mResponseAggregator;
import org.eclipse.leshan.client.response.CreateResponse;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvDecoder;
import org.eclipse.leshan.tlv.TlvException;

public class LwM2mClientObjectInstance extends LwM2mClientNode {

    private final LwM2mClientObjectDefinition definition;
    private final Map<Integer, LwM2mClientResource> resources;
    private final int id;
    private final LwM2mClientObject parent;

    public LwM2mClientObjectInstance(final int id, final LwM2mClientObject parent,
            final LwM2mClientObjectDefinition definition) {
        this.id = id;
        this.resources = new HashMap<>();
        this.definition = definition;
        this.parent = parent;
    }

    public int getId() {
        return id;
    }

    public void createMandatory() {
        for (final LwM2mClientResourceDefinition def : definition.getResourceDefinitions()) {
            resources.put(def.getId(), def.createResource());
        }
    }

    public void createInstance(final LwM2mExchange exchange) {
        final byte[] payload = exchange.getRequestPayload();
        Tlv[] tlvs;
        try {
            tlvs = TlvDecoder.decode(ByteBuffer.wrap(payload));
        } catch (TlvException e) {
            throw new IllegalStateException(e);
        }

        if (!definition.hasAllRequiredResourceIds(tlvs)) {
            exchange.respond(CreateResponse.invalidResource());
            return;
        }

        for (final LwM2mClientResourceDefinition def : definition.getResourceDefinitions()) {
            resources.put(def.getId(), def.createResource());
        }

        final LwM2mResponseAggregator aggr = new LwM2mObjectInstanceCreateResponseAggregator(exchange, tlvs.length, id);
        for (final Tlv tlv : tlvs) {
            final LwM2mClientResourceDefinition def = definition.getResourceDefinition(tlv.getIdentifier());
            if (def == null) {
                aggr.respond(tlv.getIdentifier(), CreateResponse.invalidResource());
            } else {
                final LwM2mClientResource res = def.createResource();
                final AggregatedLwM2mExchange partialExchange = new AggregatedLwM2mExchange(aggr, tlv.getIdentifier());
                partialExchange.setRequestPayload(tlv.getValue());
                resources.put(tlv.getIdentifier(), res);
                res.write(partialExchange);
            }
        }
    }

    @Override
    public void read(final LwM2mExchange exchange) {
        final LwM2mResponseAggregator aggr = new LwM2mObjectInstanceReadResponseAggregator(exchange, resources.size());
        for (final Entry<Integer, LwM2mClientResource> entry : resources.entrySet()) {
            final LwM2mClientResource res = entry.getValue();
            final int id = entry.getKey();
            res.read(new AggregatedLwM2mExchange(aggr, id));
        }
    }

    public void addResource(final Integer resourceId, final LwM2mClientResource resource) {
        resources.put(resourceId, resource);
    }

    public Map<Integer, LwM2mClientResource> getAllResources() {
        return new HashMap<>(resources);
    }

    public void delete(LwM2mExchange exchange) {
        parent.delete(exchange, id);
    }

}

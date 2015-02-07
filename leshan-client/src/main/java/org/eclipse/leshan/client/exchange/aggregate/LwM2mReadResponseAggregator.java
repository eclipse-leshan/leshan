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
package org.eclipse.leshan.client.exchange.aggregate;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.response.LwM2mResponse;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvEncoder;

public abstract class LwM2mReadResponseAggregator extends LwM2mResponseAggregator {

    public LwM2mReadResponseAggregator(final LwM2mExchange exchange, final int numExpectedResults) {
        super(exchange, numExpectedResults);
    }

    @Override
    protected void respondToExchange(final Map<Integer, LwM2mResponse> responses, final LwM2mExchange exchange) {
        final TreeMap<Integer, LwM2mResponse> sortedResponses = new TreeMap<>(responses);
        final Queue<Tlv> tlvs = new LinkedList<Tlv>();
        for (final Entry<Integer, LwM2mResponse> entry : sortedResponses.entrySet()) {
            final int id = entry.getKey();
            final LwM2mResponse response = entry.getValue();
            if (response.isSuccess()) {
                tlvs.add(createTlv(id, response));
            }
        }
        final byte[] payload = TlvEncoder.encode(tlvs.toArray(new Tlv[0])).array();
        exchange.respond(ReadResponse.success(payload));
    }

    protected abstract Tlv createTlv(final int id, final LwM2mResponse response);

}

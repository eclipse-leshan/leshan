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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.response.LwM2mResponse;

public abstract class LwM2mResponseAggregator {

    private final LwM2mExchange exchange;
    private final Map<Integer, LwM2mResponse> responses;
    private final int numExpectedResults;

    public LwM2mResponseAggregator(final LwM2mExchange exchange, final int numExpectedResults) {
        this.exchange = exchange;
        this.responses = new ConcurrentHashMap<>();
        this.numExpectedResults = numExpectedResults;
        respondIfReady();
    }

    public void respond(final int id, final LwM2mResponse response) {
        responses.put(id, response);
        respondIfReady();
    }

    private void respondIfReady() {
        if (responses.size() == numExpectedResults) {
            respondToExchange(responses, exchange);
        }
    }

    protected abstract void respondToExchange(Map<Integer, LwM2mResponse> responses, LwM2mExchange exchange);

    public LwM2mExchange getUnderlyingExchange() {
        return exchange;
    }

}

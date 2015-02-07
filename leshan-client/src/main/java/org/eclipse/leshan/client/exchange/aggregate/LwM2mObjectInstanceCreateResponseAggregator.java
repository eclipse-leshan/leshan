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

import java.util.Collection;
import java.util.Map;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.response.CreateResponse;
import org.eclipse.leshan.client.response.LwM2mResponse;

public class LwM2mObjectInstanceCreateResponseAggregator extends LwM2mResponseAggregator {

    private final int instanceId;

    public LwM2mObjectInstanceCreateResponseAggregator(final LwM2mExchange exchange, final int numExpectedResults,
            final int instanceId) {
        super(exchange, numExpectedResults);
        this.instanceId = instanceId;
    }

    @Override
    protected void respondToExchange(final Map<Integer, LwM2mResponse> responses, final LwM2mExchange exchange) {
        if (isSuccess(responses.values())) {
            exchange.respond(CreateResponse.success(instanceId));
        } else {
            exchange.respond(CreateResponse.methodNotAllowed());
        }
    }

    private boolean isSuccess(final Collection<LwM2mResponse> values) {
        for (final LwM2mResponse response : values) {
            if (!response.isSuccess()) {
                return false;
            }
        }
        return true;
    }

}

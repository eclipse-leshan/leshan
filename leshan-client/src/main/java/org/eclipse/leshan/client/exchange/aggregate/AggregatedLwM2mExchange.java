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

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.response.LwM2mResponse;

public class AggregatedLwM2mExchange implements LwM2mExchange {

    private final LwM2mResponseAggregator aggr;
    private final int id;
    private byte[] payload;

    public AggregatedLwM2mExchange(final LwM2mResponseAggregator aggr, final int id) {
        this.aggr = aggr;
        this.id = id;
    }

    @Override
    public void respond(final LwM2mResponse response) {
        aggr.respond(id, response);
    }

    @Override
    public byte[] getRequestPayload() {
        return payload;
    }

    public void setRequestPayload(final byte[] newPayload) {
        payload = newPayload;
    }

    @Override
    public boolean hasObjectInstanceId() {
        return false;
    }

    @Override
    public int getObjectInstanceId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isObserve() {
        return aggr.getUnderlyingExchange().isObserve();
    }

    @Override
    public ObserveSpec getObserveSpec() {
        return aggr.getUnderlyingExchange().getObserveSpec();
    }

}

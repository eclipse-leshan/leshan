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

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.response.LwM2mResponse;
import org.eclipse.leshan.tlv.Tlv;

public class LwM2mObjectInstanceReadResponseAggregator extends LwM2mReadResponseAggregator {

    public LwM2mObjectInstanceReadResponseAggregator(final LwM2mExchange exchange, final int numExpectedResults) {
        super(exchange, numExpectedResults);
    }

    @Override
    protected Tlv createTlv(final int id, final LwM2mResponse response) {
        Tlv result = response.getResponsePayloadAsTlv();
        result.setIdentifier(id);
        return result;
    }

}

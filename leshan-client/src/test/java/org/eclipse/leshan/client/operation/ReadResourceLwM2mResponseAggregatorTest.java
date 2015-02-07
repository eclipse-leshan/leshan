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
package org.eclipse.leshan.client.operation;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.exchange.aggregate.AggregatedLwM2mExchange;
import org.eclipse.leshan.client.exchange.aggregate.LwM2mObjectInstanceReadResponseAggregator;
import org.eclipse.leshan.client.exchange.aggregate.LwM2mResponseAggregator;
import org.eclipse.leshan.client.response.LwM2mResponse;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReadResourceLwM2mResponseAggregatorTest {

    @Mock
    private LwM2mExchange coapExchange;

    @Test
    public void testSingleSuccessfulRead() {
        final int numExpectedResults = 1;
        final int resourceId = 45;
        final byte[] resourceValue = "value".getBytes();

        final LwM2mResponseAggregator aggr = new LwM2mObjectInstanceReadResponseAggregator(coapExchange,
                numExpectedResults);
        final LwM2mExchange exchange = new AggregatedLwM2mExchange(aggr, resourceId);

        exchange.respond(ReadResponse.success(resourceValue));

        final Tlv[] tlvs = new Tlv[numExpectedResults];
        tlvs[0] = new Tlv(TlvType.RESOURCE_VALUE, null, resourceValue, resourceId);
        verify(coapExchange).respond(ReadResponse.success(TlvEncoder.encode(tlvs).array()));
    }

    @Test
    public void testMultipleSuccessfulReads() {
        final int numExpectedResults = 2;
        final int resourceId1 = 45;
        final int resourceId2 = 78;
        final byte[] resourceValue1 = "hello".getBytes();
        final byte[] resourceValue2 = "world".getBytes();

        final LwM2mResponseAggregator aggr = new LwM2mObjectInstanceReadResponseAggregator(coapExchange,
                numExpectedResults);
        final LwM2mExchange exchange1 = new AggregatedLwM2mExchange(aggr, resourceId1);
        final LwM2mExchange exchange2 = new AggregatedLwM2mExchange(aggr, resourceId2);

        exchange1.respond(ReadResponse.success(resourceValue1));
        exchange2.respond(ReadResponse.success(resourceValue2));

        final Tlv[] tlvs = new Tlv[numExpectedResults];
        tlvs[0] = new Tlv(TlvType.RESOURCE_VALUE, null, resourceValue1, resourceId1);
        tlvs[1] = new Tlv(TlvType.RESOURCE_VALUE, null, resourceValue2, resourceId2);
        verify(coapExchange).respond(ReadResponse.success(TlvEncoder.encode(tlvs).array()));
    }

    @Test
    public void testIncompleteReadDoesNotSend() {
        final int numExpectedResults = 2;
        final int resourceId = 45;
        final byte[] resourceValue = "hello".getBytes();

        final LwM2mResponseAggregator aggr = new LwM2mObjectInstanceReadResponseAggregator(coapExchange,
                numExpectedResults);
        final LwM2mExchange exchange = new AggregatedLwM2mExchange(aggr, resourceId);

        exchange.respond(ReadResponse.success(resourceValue));

        verify(coapExchange, never()).respond(any(LwM2mResponse.class));
    }

}

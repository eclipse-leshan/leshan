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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import static org.junit.Assert.*;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.server.californium.impl.CaliforniumObservation;
import org.eclipse.leshan.server.observation.Observation;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CaliforniumObservationTest {

    final String reportedValue = "15";
    Request coapRequest;
    LwM2mPath target;

    private CaliforniumTestSupport support = new CaliforniumTestSupport();

    @Before
    public void setUp() throws Exception {
        support.givenASimpleClient();
        target = new LwM2mPath(3, 0, 15);
    }

    @Test
    public void coapNotification_is_forwarded_to_observationListener() {

        ObservationListener listener = new ObservationListener() {
            @Override
            public void newValue(Observation observation, LwM2mNode value) {
                assertEquals(CaliforniumObservationTest.this.reportedValue, ((LwM2mResource) value).getValue().value);
                assertEquals(3, observation.getPath().getObjectId());
                assertEquals((Integer) 0, observation.getPath().getObjectInstanceId());
                assertEquals((Integer) 15, observation.getPath().getResourceId());
                assertEquals(CaliforniumObservationTest.this.support.client, observation.getClient());
            }

            @Override
            public void cancelled(Observation observation) {
            }
        };
        givenAnObserveRequest(target);
        CaliforniumObservation observation = new CaliforniumObservation(coapRequest, support.client, target);
        observation.addListener(listener);
        Response coapResponse = new Response(ResponseCode.CONTENT);
        coapResponse.setPayload(reportedValue);
        observation.onResponse(coapResponse);
    }

    @Test
    public void cancel_Observation_cancel_coapRequest() {

        givenAnObserveRequest(target);
        assertFalse(coapRequest.isCanceled());
        CaliforniumObservation observation = new CaliforniumObservation(coapRequest, support.client, target);
        observation.cancel();
        Assert.assertTrue(coapRequest.isCanceled());
    }

    private void givenAnObserveRequest(LwM2mPath target) {
        coapRequest = Request.newGet();
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectInstanceId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getResourceId()));
        coapRequest.setDestination(support.client.getAddress());
        coapRequest.setDestinationPort(support.client.getPort());
    }
}

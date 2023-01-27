/*******************************************************************************
 * Copyright (c) 2021 Orange.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.DummyDecoder;
import org.junit.jupiter.api.Test;

public class LwM2mResponseBuilderTest {

    private final DummyDecoder decoder = new DummyDecoder();
    private final LwM2mLinkParser linkParser = new DefaultLwM2mLinkParser();

    @Test
    public void visit_observe_request() {
        // given
        String examplePath = "/1/2/3";

        ObserveRequest observeRequest = new ObserveRequest(null, examplePath);

        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(null, null, observeRequest);

        Request coapRequest = new Request(null);
        coapRequest.setToken(Token.EMPTY);
        coapRequest.setUserContext(userContext);

        Response coapResponse = new Response(CoAP.ResponseCode.CONTENT);
        coapResponse.getOptions().setObserve(1);

        LwM2mResponseBuilder<ObserveResponse> responseBuilder = new LwM2mResponseBuilder<>(coapRequest, coapResponse,
                null, null, decoder, linkParser);
        // when
        responseBuilder.visit(observeRequest);

        // then
        ObserveResponse response = responseBuilder.getResponse();
        assertNotNull(response);
        assertNotNull(response.getObservation());

        SingleObservation observation = response.getObservation();
        assertEquals(examplePath, observation.getPath().toString());
    }

    @Test
    public void visit_observe_composite_request() {
        // given
        List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));

        ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(null, null, examplePaths);

        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(null, null,
                observeRequest);

        Request coapRequest = new Request(null);
        coapRequest.setToken(Token.EMPTY);
        coapRequest.setUserContext(userContext);

        Response coapResponse = new Response(CoAP.ResponseCode.CONTENT);
        coapResponse.getOptions().setObserve(1);

        LwM2mResponseBuilder<ObserveCompositeResponse> responseBuilder = new LwM2mResponseBuilder<>(coapRequest,
                coapResponse, null, null, decoder, linkParser);
        // when
        responseBuilder.visit(observeRequest);

        // then
        ObserveCompositeResponse response = responseBuilder.getResponse();
        assertNotNull(response);
        assertNotNull(response.getObservation());

        CompositeObservation observation = response.getObservation();
        assertEquals(examplePaths, observation.getPaths());
    }

}

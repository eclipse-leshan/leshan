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
 *     Michał Wadowski (Orange) - Add Cancel Composite-Observation feature.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.observation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

public class ObserveUtilTest {

    @Test
    public void should_create_observation_from_context() {
        // given
        String examplePath = "/1/2/3";
        String exampleRegistrationId = "registrationId";
        Token exampleToken = Token.EMPTY;

        ObserveRequest observeRequest = new ObserveRequest(null, examplePath);

        // when
        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(null, exampleRegistrationId,
                observeRequest);
        userContext.put("extraKey", "extraValue");

        Request coapRequest = new Request(null);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());

        SingleObservation observation = ObserveUtil.createLwM2mObservation(coapRequest);

        // then
        assertEquals(examplePath, observation.getPath().toString());
        assertEquals(exampleRegistrationId, observation.getRegistrationId());
        assertArrayEquals(exampleToken.getBytes(), observation.getId().getBytes());
        assertTrue(observation.getContext().containsKey("extraKey"));
        assertEquals("extraValue", observation.getContext().get("extraKey"));
        assertEquals(ContentFormat.DEFAULT, observation.getContentFormat());
    }

    @Test
    public void should_create_composite_observation_from_context() {
        // given
        List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));
        String exampleRegistrationId = "registrationId";
        Token exampleToken = Token.EMPTY;

        ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(null, null, examplePaths);

        // when
        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(null,
                exampleRegistrationId, observeRequest);
        userContext.put("extraKey", "extraValue");

        Request coapRequest = new Request(null);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.getOptions().setContentFormat(ContentFormat.CBOR.getCode());
        coapRequest.getOptions().setAccept(ContentFormat.JSON.getCode());

        CompositeObservation observation = ObserveUtil.createLwM2mCompositeObservation(coapRequest);

        // then
        assertEquals(examplePaths, observation.getPaths());
        assertEquals(exampleRegistrationId, observation.getRegistrationId());
        assertArrayEquals(exampleToken.getBytes(), observation.getId().getBytes());
        assertTrue(observation.getContext().containsKey("extraKey"));
        assertEquals("extraValue", observation.getContext().get("extraKey"));
        assertEquals(ContentFormat.CBOR, observation.getRequestContentFormat());
        assertEquals(ContentFormat.JSON, observation.getResponseContentFormat());
    }

    @Test
    public void should_not_create_observation_without_context() {
        // given
        final Request coapRequest = new Request(null);

        // when / then
        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                ObserveUtil.createLwM2mObservation(coapRequest);
            }
        });
    }

    @Test
    public void should_not_create_observation_without_path_in_context() {
        // given
        Map<String, String> userContext = new HashMap<>();

        final Request coapRequest = new Request(null);
        coapRequest.setUserContext(userContext);

        // when / then
        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                ObserveUtil.createLwM2mObservation(coapRequest);
            }
        });
    }
}

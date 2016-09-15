/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.observe.Observation;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link InMemoryLwM2mObservationStore}
 */
public class InMemoryLwM2mObservationStoreTest {

    private LwM2mObservationStore store;

    @Before
    public void setUp() throws Exception {
        store = new InMemoryLwM2mObservationStore();
    }

    @Test(expected = IllegalStateException.class)
    public void missing_registration_id() {
        store.add(new Observation(Request.newGet(), null));
    }

    @Test
    public void get_by_token() {
        byte[] token = CaliforniumTestSupport.createToken();
        store.add(newObservation(token, "regId"));

        Observation obs = store.get(token);
        assertNotNull(obs);
        assertArrayEquals(token, obs.getRequest().getToken());
    }

    @Test
    public void get_by_registrationId() {
        store.add(newObservation("regId1"));
        store.add(newObservation("regId1"));
        store.add(newObservation("regId2"));

        Collection<Observation> obs = store.getByRegistrationId("regId1");
        assertEquals(2, obs.size());
    }

    @Test
    public void remove_by_token() {
        byte[] token = CaliforniumTestSupport.createToken();
        store.add(newObservation(token, "regId"));

        store.remove(token);

        assertNull(store.get(token));
    }

    @Test
    public void remove_by_registrationId() {
        store.add(newObservation("regId1"));
        store.add(newObservation("regId1"));

        Collection<Observation> obs = store.removeAll("regId1");
        assertEquals(2, obs.size());

        assertEquals(0, store.getByRegistrationId("regId").size());
    }

    private Observation newObservation(String registrationId) {
        return newObservation(CaliforniumTestSupport.createToken(), registrationId);
    }

    private Observation newObservation(byte[] token, String registrationId) {
        Request coapRequest = Request.newGet();
        coapRequest.setToken(token);
        Map<String, String> context = new HashMap<>();
        context.put(CoapRequestBuilder.CTX_REGID, registrationId);
        context.put(CoapRequestBuilder.CTX_LWM2M_PATH, "/3/0");
        coapRequest.setUserContext(context);
        return new Observation(coapRequest, null);
    }

}

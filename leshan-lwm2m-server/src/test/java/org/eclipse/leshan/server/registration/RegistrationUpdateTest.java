/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import static org.eclipse.leshan.core.util.TestToolBox.uriHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.server.queue.PresenceService;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * tests the implementation of {@link PresenceService}
 *
 */
class RegistrationUpdateTest {

    @Test
    void testAdditionalAttributesUpdate() throws Exception {
        Registration.Builder builder = new Registration.Builder("registrationId", "endpoint",
                new IpPeer(new InetSocketAddress(Inet4Address.getLocalHost(), 1)),
                uriHandler.createUri("coap://localhost:5683"));

        Map<String, String> additionalAttributes = new HashMap<String, String>();
        additionalAttributes.put("x", "1");
        additionalAttributes.put("y", "10");
        additionalAttributes.put("z", "100");
        builder.additionalRegistrationAttributes(additionalAttributes);

        IRegistration r = builder.build();

        Map<String, String> updateAdditionalAttributes = new HashMap<String, String>();
        updateAdditionalAttributes.put("x", "2");
        updateAdditionalAttributes.put("y", "11");
        updateAdditionalAttributes.put("z", "101");
        updateAdditionalAttributes.put("h", "hello");

        RegistrationUpdate updateReg = new RegistrationUpdate(r.getId(), r.getClientTransportData(), null, null, null,
                null, null, null, null, null, updateAdditionalAttributes, null);

        r = updateReg.update(r);

        Map<String, String> updatedAdditionalAttributes = r.getAdditionalRegistrationAttributes();

        assertEquals("2", updatedAdditionalAttributes.get("x"));
        assertEquals("11", updatedAdditionalAttributes.get("y"));
        assertEquals("101", updatedAdditionalAttributes.get("z"));
        assertTrue(updatedAdditionalAttributes.containsKey("h"));
        assertEquals("hello", updatedAdditionalAttributes.get("h"));
    }

    @Test
    void testApplicationDataUpdate() throws Exception {

        Registration.Builder builder = new Registration.Builder("registrationId", "endpoint",
                new IpPeer(new InetSocketAddress(Inet4Address.getLocalHost(), 1)),
                uriHandler.createUri("coap://localhost:5683"));
        Map<String, String> customData = new HashMap<String, String>();
        customData.put("x", "1");
        customData.put("y", "10");
        customData.put("z", "100");
        builder.customRegistrationData(customData);
        IRegistration r = builder.build();

        RegistrationUpdate updateReg = new RegistrationUpdate(r.getId(), r.getClientTransportData(), null, null, null,
                null, null, null, null, null, null, null);

        r = updateReg.update(r);

        Map<String, String> updatedCustomData = r.getCustomRegistrationData();

        assertEquals("1", updatedCustomData.get("x"));
        assertEquals("10", updatedCustomData.get("y"));
        assertEquals("100", updatedCustomData.get("z"));
    }

    @Test
    void assertEqualsHashcode() {
        EqualsVerifier.forClass(RegistrationUpdate.class).verify();
    }
}

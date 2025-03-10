/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.link.DefaultLinkParser;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.servers.DefaultServerEndpointNameProvider;
import org.eclipse.leshan.servers.security.Authorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RegistrationHandlerTest {

    private RegistrationHandler registrationHandler;
    private RegistrationStore registrationStore;
    private TestAuthorizer authorizer;

    @BeforeEach
    public void setUp() throws UnknownHostException {
        authorizer = new TestAuthorizer();
        registrationStore = new InMemoryRegistrationStore();
        registrationHandler = new RegistrationHandler(new RegistrationServiceImpl(registrationStore), authorizer,
                new RandomStringRegistrationIdProvider(), new DefaultRegistrationDataExtractor(),
                new DefaultServerEndpointNameProvider());
    }

    @Test
    public void test_application_data_from_authorizer() {

        // Prepare authorizer
        Map<String, String> appData = new HashMap<>();
        appData.put("key1", "value1");
        appData.put("key2", "value2");
        authorizer.willReturn(Authorization.approved(appData));

        // handle REGISTER request
        registrationHandler.register(givenIdentity(), givenRegisterRequestWithEndpoint("myEndpoint"),
                givenServerEndpointUri());

        // check result
        Registration registration = registrationStore.getRegistrationByEndpoint("myEndpoint");
        assertEquals(appData, registration.getCustomRegistrationData());

        // Prepare authorizer
        Map<String, String> updatedAppData = new HashMap<>();
        updatedAppData.put("key3", "value3");
        authorizer.willReturn(Authorization.approved(updatedAppData));

        // handle UPDATE request
        registrationHandler.update(givenIdentity(), givenUpdateRequestWithID(registration.getId()),
                givenServerEndpointUri());

        // check result
        registration = registrationStore.getRegistrationByEndpoint("myEndpoint");
        assertEquals(updatedAppData, registration.getCustomRegistrationData());

    }

    @Test
    public void test_update_without_application_data_from_authorizer() {

        // Prepare authorizer
        Map<String, String> appData = new HashMap<>();
        appData.put("key1", "value1");
        appData.put("key2", "value2");
        authorizer.willReturn(Authorization.approved(appData));

        // handle REGISTER request
        registrationHandler.register(givenIdentity(), givenRegisterRequestWithEndpoint("myEndpoint"),
                givenServerEndpointUri());

        // check result
        Registration registration = registrationStore.getRegistrationByEndpoint("myEndpoint");
        assertEquals(appData, registration.getCustomRegistrationData());

        // Prepare authorizer
        authorizer.willReturn(Authorization.approved());

        // handle UPDATE request
        registrationHandler.update(givenIdentity(), givenUpdateRequestWithID(registration.getId()),
                givenServerEndpointUri());

        // check result
        registration = registrationStore.getRegistrationByEndpoint("myEndpoint");
        assertEquals(appData, registration.getCustomRegistrationData());
    }

    @Test
    public void test_unsupported_lwm2m_version() {
        // handle REGISTER request
        SendableResponse<RegisterResponse> response = registrationHandler.register(givenIdentity(),
                givenRegisterRequestWithEndpoint("myEndpoint", LwM2mVersion.get("1.2")), givenServerEndpointUri());
        assertEquals(response.getResponse().getCode(), ResponseCode.PRECONDITION_FAILED);

        // check result
        Registration registration = registrationStore.getRegistrationByEndpoint("myEndpoint");
        assertNull(registration);
    }

    private IpPeer givenIdentity() {
        return new IpPeer(new InetSocketAddress(0));
    }

    private EndpointUri givenServerEndpointUri() {
        return new EndpointUri("coap", "localhost", 5683);
    }

    private RegisterRequest givenRegisterRequestWithEndpoint(String endpoint) {
        return givenRegisterRequestWithEndpoint(endpoint, LwM2mVersion.V1_1);
    }

    private RegisterRequest givenRegisterRequestWithEndpoint(String endpoint, LwM2mVersion version) {
        try {
            return new RegisterRequest(endpoint, 3600l, version.toString(), EnumSet.of(BindingMode.U), false, null,
                    new DefaultLinkParser().parseCoreLinkFormat("</1/0/1>,</2/1>,</3>".getBytes()), null);
        } catch (LinkParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private UpdateRequest givenUpdateRequestWithID(String registrationID) {
        return new UpdateRequest(registrationID, null, null, null, null, null);
    }

    private static class TestAuthorizer implements Authorizer {

        private Authorization autorization;

        public void willReturn(Authorization authorization) {
            this.autorization = authorization;
        }

        @Override
        public Authorization isAuthorized(UplinkRequest<?> request, Registration registration, LwM2mPeer sender,
                EndpointUri endpointUri) {
            return autorization;
        }
    }
}

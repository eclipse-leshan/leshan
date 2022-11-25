/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InMemoryRegistrationStoreTest {

    private final String ep = "urn:endpoint";
    private final int port = 23452;
    private final Long lifetime = 10000L;
    private final String sms = "0171-32423545";
    private final EnumSet<BindingMode> binding = EnumSet.of(BindingMode.U, BindingMode.Q, BindingMode.S);
    private final Link[] objectLinks = new Link[] { new Link("/3") };
    private final String registrationId = "4711";
    private final Token exampleToken = Token.EMPTY;
    private final String examplePath = "/1/2/3";
    private final List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));

    CaliforniumRegistrationStore store;
    InetAddress address;
    Registration registration;

    @Before
    public void setUp() throws UnknownHostException {
        address = InetAddress.getLocalHost();
        store = new InMemoryRegistrationStore();
    }

    @Test
    public void update_registration_keeps_properties_unchanged() {
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        RegistrationUpdate update = new RegistrationUpdate(registrationId, Identity.unsecure(address, port), null, null,
                null, null, null, null);
        UpdatedRegistration updatedRegistration = store.updateRegistration(update);
        assertEquals(lifetime, updatedRegistration.getUpdatedRegistration().getLifeTimeInSec());
        Assert.assertSame(binding, updatedRegistration.getUpdatedRegistration().getBindingMode());
        assertEquals(sms, updatedRegistration.getUpdatedRegistration().getSmsNumber());

        assertEquals(registration, updatedRegistration.getPreviousRegistration());

        Registration reg = store.getRegistrationByEndpoint(ep);
        assertEquals(lifetime, reg.getLifeTimeInSec());
        Assert.assertSame(binding, reg.getBindingMode());
        assertEquals(sms, reg.getSmsNumber());
    }

    @Test
    public void client_registration_sets_time_to_live() {
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);
        Assert.assertTrue(registration.isAlive());
    }

    @Test
    public void update_registration_to_extend_time_to_live() {
        givenASimpleRegistration(0L);
        store.addRegistration(registration);
        Assert.assertFalse(registration.isAlive());

        RegistrationUpdate update = new RegistrationUpdate(registrationId, Identity.unsecure(address, port), lifetime,
                null, null, null, null, null);
        UpdatedRegistration updatedRegistration = store.updateRegistration(update);
        Assert.assertTrue(updatedRegistration.getUpdatedRegistration().isAlive());

        Registration reg = store.getRegistrationByEndpoint(ep);
        Assert.assertTrue(reg.isAlive());
    }

    @Test
    public void put_coap_observation_with_valid_request() {
        // given
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapObservation();

        // when
        store.put(exampleToken, observationToStore);

        // then
        org.eclipse.californium.core.observe.Observation observationFetched = store.get(exampleToken);

        assertNotNull(observationFetched);
        assertEquals(observationToStore.toString(), observationFetched.toString());
    }

    @Test
    public void get_observation_from_request() {
        // given
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapObservation();

        // when
        store.put(exampleToken, observationToStore);

        // then
        Observation leshanObservation = store.getObservation(registrationId, exampleToken.getBytes());
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof SingleObservation);
        SingleObservation observation = (SingleObservation) leshanObservation;
        assertEquals(examplePath, observation.getPath().toString());
    }

    @Test
    public void get_composite_observation_from_request() {
        // given
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapCompositeObservation();

        // when
        store.put(exampleToken, observationToStore);

        // then
        Observation leshanObservation = store.getObservation(registrationId, exampleToken.getBytes());
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof CompositeObservation);
        CompositeObservation observation = (CompositeObservation) leshanObservation;
        assertEquals(examplePaths, observation.getPaths());
    }

    private org.eclipse.californium.core.observe.Observation prepareCoapObservation() {
        ObserveRequest observeRequest = new ObserveRequest(null, examplePath);

        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(ep, registrationId,
                observeRequest);

        Request coapRequest = new Request(CoAP.Code.GET);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.setObserve();
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());

        return new org.eclipse.californium.core.observe.Observation(coapRequest, null);
    }

    private org.eclipse.californium.core.observe.Observation prepareCoapCompositeObservation() {
        ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(null, null, examplePaths);

        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(ep, registrationId,
                observeRequest);

        Request coapRequest = new Request(CoAP.Code.FETCH);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.setObserve();
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());

        return new org.eclipse.californium.core.observe.Observation(coapRequest, null);
    }

    private void givenASimpleRegistration(Long lifetime) {

        Registration.Builder builder = new Registration.Builder(registrationId, ep, Identity.unsecure(address, port));

        registration = builder.lifeTimeInSec(lifetime).smsNumber(sms).bindingMode(binding).objectLinks(objectLinks)
                .build();
    }
}

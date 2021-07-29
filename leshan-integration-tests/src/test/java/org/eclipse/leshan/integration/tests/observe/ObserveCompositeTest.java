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
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.observe;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

public class ObserveCompositeTest {

    private final String examplePath1 = "/3/0/15";
    private final String examplePath2 = "/3/0/14";
    private final String exampleValue1 = "Europe/Paris";
    private final String exampleValue2 = "+11";

    protected IntegrationTestHelper helper = new IntegrationTestHelper();
    private Registration currentRegistration;
    private TestObservationListener listener;

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        currentRegistration = helper.getCurrentRegistration();
        listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void can_composite_observe_on_single_resource() throws InterruptedException {
        ObserveCompositeResponse observeResponse = sendObserveCompose(examplePath1);

        assertObserveCompositeResponse(observeResponse);

        CompositeObservation observation = observeResponse.getObservation();
        assertObservationContainsPaths(observation, examplePath1);
        assertOneValidObservation(observation);

        writeSingleExampleValue();

        assertResponseContainsPaths(examplePath1);

        assertListenerResponseContainsValue(15, examplePath1, exampleValue1);

        assertListenerResponse(listener);
    }

    @Test
    public void should_not_get_response_if_modified_other_resource_than_observed() throws InterruptedException {
        ObserveCompositeResponse observeResponse = sendObserveCompose(examplePath2);

        assertObserveCompositeResponse(observeResponse);

        CompositeObservation observation = observeResponse.getObservation();
        assertObservationContainsPaths(observation, examplePath2);
        assertOneValidObservation(observation);

        writeSingleExampleValue();

        assertResponseEmpty();
    }

    @Test
    public void can_composite_observe_on_multiple_resources() throws InterruptedException {
        ObserveCompositeResponse observeResponse = sendObserveCompose(examplePath1, examplePath2);

        assertObserveCompositeResponse(observeResponse);

        CompositeObservation observation = observeResponse.getObservation();
        assertObservationContainsPaths(observation, examplePath1, examplePath2);
        assertOneValidObservation(observation);

        writeSingleExampleValue();

        assertResponseContainsPaths(examplePath1, examplePath2);
        assertListenerResponseContainsValue(15, examplePath1, exampleValue1);
        assertListenerResponseContainsValue(14, examplePath2, "+02");

        assertListenerResponse(listener);
    }

    @Test
    public void can_composite_observe_on_multiple_resources_with_write_composite() throws InterruptedException {
        ObserveCompositeResponse observeResponse = sendObserveCompose(examplePath1, examplePath2);

        assertObserveCompositeResponse(observeResponse);

        CompositeObservation observation = observeResponse.getObservation();
        assertObservationContainsPaths(observation, examplePath1, examplePath2);
        assertOneValidObservation(observation);

        writeCompositeExampleValues();

        assertResponseContainsPaths(examplePath1, examplePath2);
        assertListenerResponseContainsValue(15, examplePath1, exampleValue1);
        assertListenerResponseContainsValue(14, examplePath2, exampleValue2);

        assertListenerResponse(listener);
    }

    @Test
    public void can_observe_instance() throws InterruptedException {
        String examplePath = "/3/0";

        ObserveCompositeResponse observeResponse = sendObserveCompose(examplePath);

        assertObserveCompositeResponse(observeResponse);

        CompositeObservation observation = observeResponse.getObservation();
        assertObservationContainsPaths(observation, examplePath);
        assertOneValidObservation(observation);

        writeSingleExampleValue();

        assertResponseContainsPaths(examplePath);

        assertListenerResponseEqualsToReadResponse(examplePath);
    }

    private void assertListenerResponseEqualsToReadResponse(String examplePath) throws InterruptedException {
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();

        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(ContentFormat.SENML_JSON, examplePath));

        assertEquals(readResp.getContent(), content.get(new LwM2mPath(examplePath)));
    }

    @Test
    public void can_observe_object() throws InterruptedException {
        String examplePath = "/3";

        ObserveCompositeResponse observeResponse = sendObserveCompose(examplePath);

        assertObserveCompositeResponse(observeResponse);

        CompositeObservation observation = observeResponse.getObservation();
        assertObservationContainsPaths(observation, examplePath);
        assertOneValidObservation(observation);

        writeSingleExampleValue();

        assertResponseContainsPaths(examplePath);

        assertListenerResponseEqualsToReadResponse(examplePath);
    }

    private void assertObservationContainsPaths(CompositeObservation observation, String... paths) {
        assertNotNull(observation);
        assertEquals(paths.length, observation.getPaths().size());
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            assertEquals(path, observation.getPaths().get(i).toString());
        }
    }

    private void assertListenerResponseContainsValue(int id, String path, String value) {
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();
        assertEquals(
                LwM2mSingleResource.newStringResource(id, value),
                content.get(new LwM2mPath(path))
        );
    }

    private void assertResponseEmpty() {
        assertFalse(listener.receivedNotify().get());
    }

    private void assertResponseContainsPaths(String... paths) {
        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();
        for (String path : paths) {
            assertTrue(content.containsKey(new LwM2mPath(path)));
        }
    }

    private ObserveCompositeResponse sendObserveCompose(String... paths) throws InterruptedException {
        return helper.server.send(
                currentRegistration,
                new ObserveCompositeRequest(
                        ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, paths
                )
        );
    }

    private void writeSingleExampleValue() throws InterruptedException {
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, exampleValue1));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
    }

    private void writeCompositeExampleValues() throws InterruptedException {
        Map<String, Object> nodes = new HashMap<>();
        nodes.put(examplePath1, exampleValue1);
        nodes.put(examplePath2, exampleValue2);

        WriteCompositeResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(ContentFormat.SENML_JSON, nodes)
        );
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
    }

    private void assertListenerResponse(TestObservationListener listener) {
        assertNotNull(listener.getObserveCompositeResponse().getCoapResponse());
        assertThat(listener.getObserveCompositeResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    private void assertObserveCompositeResponse(ObserveCompositeResponse observeResponse) {
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));
    }

    private void assertOneValidObservation(CompositeObservation observation) {
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        Set<Observation> observations =
                helper.server.getObservationService().getObservations(helper.getCurrentRegistration());
        assertEquals("We should have only one observation", 1, observations.size());
        assertTrue("New observation is not there", observations.contains(observation));
    }
}

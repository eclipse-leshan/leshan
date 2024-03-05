/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.attributes;

import static org.eclipse.leshan.core.util.TestLwM2mId.MULTIPLE_INTEGER_VALUE;
import static org.eclipse.leshan.core.util.TestLwM2mId.TEST_OBJECT;
import static org.eclipse.leshan.integration.tests.util.LeshanTestClientBuilder.givenClientUsing;
import static org.eclipse.leshan.integration.tests.util.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.CancelObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.CancelObservationResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;
import org.eclipse.leshan.integration.tests.util.junit5.extensions.BeforeEachParameterizedResolver;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BeforeEachParameterizedResolver.class)
public class WriteAttributeHouseKeepingTest {

    /*---------------------------------/
     *  Parameterized Tests
     * -------------------------------*/
    @ParameterizedTest(name = "{0} - Client using {1} - Server using {2}")
    @MethodSource("transports")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllTransportLayer {
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> transports() {
        return Stream.of(//
                // ProtocolUsed - Client Endpoint Provider - Server Endpoint Provider
                arguments(Protocol.COAP, "Californium", "Californium"), //
                arguments(Protocol.COAP, "java-coap", "Californium"), //
                arguments(Protocol.COAP, "Californium", "java-coap"), //
                arguments(Protocol.COAP, "java-coap", "java-coap"));
    }

    /*---------------------------------/
     *  Set-up and Tear-down Tests
     * -------------------------------*/

    LeshanTestServer server;
    LeshanTestClient client;
    Registration currentRegistration;

    @BeforeEach
    public void start(Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider) {
        server = givenServerUsing(givenProtocol).with(givenServerEndpointProvider).build();
        server.start();
        client = givenClientUsing(givenProtocol).with(givenClientEndpointProvider).connectingTo(server).build();
        client.start();
        server.waitForNewRegistrationOf(client);
        client.waitForRegistrationTo(server);

        currentRegistration = server.getRegistrationFor(client);
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (client != null)
            client.destroy(false);
        if (server != null)
            server.destroy();
    }

    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return new LeshanTestServerBuilder(givenProtocol);
    }

    /*---------------------------------/
     *  Tests
     * -------------------------------*/

    @TestAllTransportLayer
    public void write_attribute_on_tree_then_remove_object_instance(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // For each of this path,
        LwM2mPath instancePath = new LwM2mPath(TEST_OBJECT, 0);
        LwM2mPath resourcePath = new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE);
        LwM2mPath resourceInstancePath = new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE, 0);
        List<LwM2mPath> path = Arrays.asList(instancePath, resourcePath, resourceInstancePath);

        // this attribute set will be written,
        LwM2mAttributeSet attributeSet = new LwM2mAttributeSet(
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 10l));

        // Then this request will be executed (to delete node)
        DeleteRequest deleteRequest = new DeleteRequest(instancePath);

        // Then ensure there is no more attribute for those path.
        write_attribute_on_tree_then_remove_node_then_check_attributes_are_removed(path, attributeSet, deleteRequest);

    }

    @TestAllTransportLayer
    public void write_attribute_on_tree_then_remove_resource_instance(Protocol givenProtocol,
            String givenClientEndpointProvider, String givenServerEndpointProvider) throws InterruptedException {

        // For each of this path,
        LwM2mPath resourceInstancePath = new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE, 0);
        List<LwM2mPath> path = Arrays.asList(resourceInstancePath);

        // this attribute set will be written,
        LwM2mAttributeSet attributeSet = new LwM2mAttributeSet(
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 10l));

        // Then this request will be executed (to delete node : we will replace resource instance with id 0 by resource
        // instance with id 1)
        LwM2mPath resourcePath = new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE);
        WriteRequest writeRequest = new WriteRequest(Mode.REPLACE, ContentFormat.TLV, resourcePath, //
                new LwM2mMultipleResource(MULTIPLE_INTEGER_VALUE, Type.INTEGER,
                        LwM2mResourceInstance.newIntegerInstance(1, 10)));

        // Then ensure there is no more attribute for those path.
        write_attribute_on_tree_then_remove_node_then_check_attributes_are_removed(path, attributeSet, writeRequest);
    }

    private void write_attribute_on_tree_then_remove_node_then_check_attributes_are_removed(List<LwM2mPath> pathToCheck,
            LwM2mAttributeSet attributeSet, DownlinkRequest<?> deleteRequest) throws InterruptedException {

        // For each of this path, be sure there is no attributes
        LwM2mServer lwServer = client.getServerIdForRegistrationId(currentRegistration.getId());
        for (LwM2mPath path : pathToCheck) {
            assertThat(client).hasNoAttributeSetFor(lwServer, path);
        }

        // For each of this path, Write Attribute and check it is present.
        for (LwM2mPath path : pathToCheck) {
            WriteAttributesResponse response = server.send(currentRegistration,
                    new WriteAttributesRequest(path, attributeSet));

            assertThat(response).isSuccess();
            assertThat(client).hasAttributesFor(lwServer, path, attributeSet);
        }

        // Delete given node
        LwM2mResponse response = server.send(currentRegistration, deleteRequest);
        assertThat(response).isSuccess();

        // assert there is no attributes for those path after node deletion
        for (LwM2mPath path : pathToCheck) {
            assertThat(client).hasNoAttributeSetFor(lwServer, path);
        }
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_object_then_passive_cancel_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {
        write_attribute_then_observe_then_passive_cancel_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_object_instance_then_passive_cancel_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_passive_cancel_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_resource_then_passive_cancel_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_passive_cancel_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_resource_instance_then_passive_cancel_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_passive_cancel_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE, 0));
    }

    public void write_attribute_then_observe_then_passive_cancel_then_check_no_more_notification_data(
            LwM2mPath targetedPath) throws InterruptedException {

        // Add Attribute pmin=1seconds to targeted node
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration, new WriteAttributesRequest(
                targetedPath, new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 1l))));
        assertThat(writeAttributeResponse).isSuccess();

        // Check there is no notification data
        assertThat(client).hasNoNotificationData();

        // Observe targeted node
        ObserveResponse response = server.send(currentRegistration, new ObserveRequest(targetedPath));
        SingleObservation observation = (SingleObservation) server.waitForNewObservation(client);
        assertThat(response).isSuccess();

        // Check that we have new NotificationData
        Thread.sleep(100); // wait to be sure client handle observation : TODO We should do better.
        assertThat(client).hasNotificationData();

        // Remove observation at server side : Passive Cancel observation
        server.getObservationService().cancelObservation(observation);

        // wait for notification, then assert the is no more notification data
        Thread.sleep(1200); // wait to be sure client handle RST (passive cancel)
        assertThat(client).hasNoNotificationData();
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_object_then_active_cancel_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_active_cancel_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_object_instance_then_active_cancel_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_active_cancel_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_resource_then_active_cancel_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_active_cancel_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_resource_instance_then_active_cancel_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_active_cancel_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE, 0));
    }

    public void write_attribute_then_observe_then_active_cancel_then_check_no_more_notification_data(
            LwM2mPath targetedPath) throws InterruptedException {

        // Add Attribute pmin=1seconds to targeted node
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration, new WriteAttributesRequest(
                targetedPath, new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 1l))));
        assertThat(writeAttributeResponse).isSuccess();

        // Check there is no notification data
        assertThat(client).hasNoNotificationData();

        // Observe targeted node
        ObserveResponse response = server.send(currentRegistration, new ObserveRequest(targetedPath));
        SingleObservation observation = (SingleObservation) server.waitForNewObservation(client);
        assertThat(response).isSuccess();

        // Check that we have new NotificationData
        Thread.sleep(100); // wait to be sure client handle observation : TODO We should do better.
        assertThat(client).hasNotificationData();

        // Do active cancel
        CancelObservationResponse cancelObservationResponse = server.send(currentRegistration,
                new CancelObservationRequest(observation));
        assertThat(cancelObservationResponse).isSuccess();

        // then assert the is no more notification data
        assertThat(client).hasNoNotificationData();
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_object_then_remove_object_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_remove_object_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_object_instance_then_remove_object_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_remove_object_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_resource_then_remove_object_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_remove_object_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE));
    }

    @TestAllTransportLayer
    public void write_attribute_then_observe_resource_instance_then_remove_object_then_check_no_more_notification_data(
            Protocol givenProtocol, String givenClientEndpointProvider, String givenServerEndpointProvider)
            throws InterruptedException {

        write_attribute_then_observe_then_remove_object_then_check_no_more_notification_data(
                new LwM2mPath(TEST_OBJECT, 0, MULTIPLE_INTEGER_VALUE, 0));
    }

    public void write_attribute_then_observe_then_remove_object_then_check_no_more_notification_data(
            LwM2mPath targetedPath) throws InterruptedException {

        // Add Attribute pmin=1seconds to targeted node
        WriteAttributesResponse writeAttributeResponse = server.send(currentRegistration, new WriteAttributesRequest(
                targetedPath, new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 2l))));
        assertThat(writeAttributeResponse).isSuccess();

        // Check there is no notification data
        assertThat(client).hasNoNotificationData();

        // Observe targeted node
        ObserveResponse response = server.send(currentRegistration, new ObserveRequest(targetedPath));
        assertThat(response).isSuccess();

        // Check that we have new NotificationData
        Thread.sleep(100); // wait to be sure client handle observation : TODO We should do better.
        assertThat(client).hasNotificationData();

        // Disable object
        client.getObjectTree().removeObjectEnabler(targetedPath.getObjectId());

        // then assert the is no more notification data
        assertThat(client).hasNoNotificationData();
    }

}

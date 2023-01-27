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
 *     Zebra Technologies - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for create security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.eclipse.leshan.integration.tests.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CreateTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentFormats")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllContentFormat {
    }

    static Stream<ContentFormat> contentFormats() {
        return Stream.of(//
                ContentFormat.TLV, //
                ContentFormat.JSON, //
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR);
    }

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @BeforeEach
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @AfterEach
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @TestAllContentFormat
    public void can_create_instance_without_instance_id(ContentFormat contentFormat) throws InterruptedException {
        try {
            // create ACL instance
            CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                    new CreateRequest(contentFormat, 2, LwM2mSingleResource.newIntegerResource(3, 33)));

            // verify result
            assertEquals(ResponseCode.CREATED, response.getCode());
            assertEquals("2/0", response.getLocation());
            assertNotNull(response.getCoapResponse());
            assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

            // create a second ACL instance
            response = helper.server.send(helper.getCurrentRegistration(),
                    new CreateRequest(contentFormat, 2, LwM2mSingleResource.newIntegerResource(3, 34)));

            // verify result
            assertEquals(ResponseCode.CREATED, response.getCode());
            assertEquals("2/1", response.getLocation());
            assertNotNull(response.getCoapResponse());
            assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

            // read object 2
            ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
            assertEquals(ResponseCode.CONTENT, readResponse.getCode());
            LwM2mObject object = (LwM2mObject) readResponse.getContent();
            assertEquals(33l, object.getInstance(0).getResource(3).getValue());
            assertEquals(34l, object.getInstance(1).getResource(3).getValue());
        } catch (InvalidRequestException e) {
            // only TLV support create instance without instance id
            assertNotEquals(ContentFormat.TLV, contentFormat);
        }
    }

    @TestAllContentFormat
    public void can_create_instance_with_id(ContentFormat contentFormat) throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance = new LwM2mObjectInstance(12, LwM2mSingleResource.newIntegerResource(3, 123));
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(contentFormat, 2, instance));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals(null, response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read object 2
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
        assertEquals(ResponseCode.CONTENT, readResponse.getCode());
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertEquals(object.getInstance(12).getResource(3).getValue(), 123l);
    }

    @TestAllContentFormat
    public void can_create_2_instances_of_object(ContentFormat contentFormat) throws InterruptedException {
        // create ACL instance
        LwM2mObjectInstance instance1 = new LwM2mObjectInstance(12, LwM2mSingleResource.newIntegerResource(3, 123));
        LwM2mObjectInstance instance2 = new LwM2mObjectInstance(13, LwM2mSingleResource.newIntegerResource(3, 124));
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(contentFormat, 2, instance1, instance2));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals(null, response.getLocation());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read object 2
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2));
        assertEquals(ResponseCode.CONTENT, readResponse.getCode());
        LwM2mObject object = (LwM2mObject) readResponse.getContent();
        assertEquals(object.getInstance(12).getResource(3).getValue(), 123l);
        assertEquals(object.getInstance(13).getResource(3).getValue(), 124l);
    }

    @TestAllContentFormat
    public void cannot_create_instance_without_all_required_resources(ContentFormat contentFormat)
            throws InterruptedException {
        // create ACL instance without any resources
        CreateResponse response = helper.server.send(helper.getCurrentRegistration(), new CreateRequest(contentFormat,
                LwM2mId.ACCESS_CONTROL, new LwM2mObjectInstance(0, new LwM2mResource[0])));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // create ACL instance with only 1 mandatory resources (2 missing)
        CreateResponse response2 = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(contentFormat, LwM2mId.ACCESS_CONTROL,
                        new LwM2mObjectInstance(0, LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_OBJECT_ID, 12))));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response2.getCode());

        // create 2 ACL instances, one with missing mandatory resource and the other with all mandatory resources
        LwM2mObjectInstance instance0 = new LwM2mObjectInstance(0,
                LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_OBJECT_ID, 22),
                LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_OBJECT_INSTANCE_ID, 22),
                LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_ACCESS_CONTROL_OWNER, 11));
        LwM2mObjectInstance instance1 = new LwM2mObjectInstance(1,
                LwM2mSingleResource.newIntegerResource(LwM2mId.ACL_OBJECT_ID, 22));

        CreateResponse response3 = helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(contentFormat, LwM2mId.ACCESS_CONTROL, instance0, instance1));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response3.getCode());

        // try to read to check if the instance is not created
        // client registration
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(2, 0));
        assertEquals(ResponseCode.NOT_FOUND, readResponse.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }
}

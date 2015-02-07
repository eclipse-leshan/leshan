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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.response.CreateResponse;
import org.junit.After;
import org.junit.Test;

public class CreateTest {

    IntegrationTestHelper helper = new IntegrationTestHelper();

    @After
    public void stop() {
        helper.stop();
    }

    @Test
    public void can_create_instance_of_object() {
        helper.register();

        final CreateResponse response = helper.sendCreate(
                IntegrationTestHelper.createGoodObjectInstance("hello", "goodbye"),
                IntegrationTestHelper.GOOD_OBJECT_ID);
        IntegrationTestHelper.assertEmptyResponse(response, ResponseCode.CREATED);
        assertEquals(IntegrationTestHelper.GOOD_OBJECT_ID + "/0", response.getLocation());
    }

    @Test
    public void can_create_specific_instance_of_object() {
        helper.register();

        final CreateResponse response = helper.sendCreate(IntegrationTestHelper.createGoodObjectInstance("one", "two"),
                IntegrationTestHelper.GOOD_OBJECT_ID, 14);
        IntegrationTestHelper.assertEmptyResponse(response, ResponseCode.CREATED);
        assertEquals(IntegrationTestHelper.GOOD_OBJECT_ID + "/14", response.getLocation());
    }

    @Test
    public void can_create_multiple_instance_of_object() {
        helper.register();

        final CreateResponse response = helper.sendCreate(
                IntegrationTestHelper.createGoodObjectInstance("hello", "goodbye"),
                IntegrationTestHelper.GOOD_OBJECT_ID);
        IntegrationTestHelper.assertEmptyResponse(response, ResponseCode.CREATED);
        assertEquals(IntegrationTestHelper.GOOD_OBJECT_ID + "/0", response.getLocation());

        final CreateResponse responseTwo = helper.sendCreate(
                IntegrationTestHelper.createGoodObjectInstance("hello", "goodbye"),
                IntegrationTestHelper.GOOD_OBJECT_ID);
        IntegrationTestHelper.assertEmptyResponse(responseTwo, ResponseCode.CREATED);
        assertEquals(IntegrationTestHelper.GOOD_OBJECT_ID + "/1", responseTwo.getLocation());
    }

    @Test
    public void cannot_create_instance_of_object() {
        helper.register();

        final CreateResponse response = helper
                .sendCreate(IntegrationTestHelper.createGoodObjectInstance("hello", "goodbye"),
                        IntegrationTestHelper.BAD_OBJECT_ID);
        IntegrationTestHelper.assertEmptyResponse(response, ResponseCode.NOT_FOUND);
    }

    @Test
    public void cannot_create_instance_without_all_required_resources() {
        helper.register();

        final LwM2mObjectInstance instance = new LwM2mObjectInstance(IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID,
                new LwM2mResource[] { new LwM2mResource(IntegrationTestHelper.FIRST_RESOURCE_ID,
                        Value.newStringValue("hello")) });

        final CreateResponse response = helper.sendCreate(instance, IntegrationTestHelper.GOOD_OBJECT_ID);
        IntegrationTestHelper.assertEmptyResponse(response, ResponseCode.BAD_REQUEST);

        IntegrationTestHelper.assertEmptyResponse(
                helper.sendRead(IntegrationTestHelper.GOOD_OBJECT_ID, IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID),
                ResponseCode.NOT_FOUND);
    }

    @Test
    public void cannot_create_instance_with_extraneous_resources() {
        helper.register();

        final LwM2mObjectInstance instance = new LwM2mObjectInstance(IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID,
                new LwM2mResource[] {
                                        new LwM2mResource(IntegrationTestHelper.FIRST_RESOURCE_ID,
                                                Value.newStringValue("hello")),
                                        new LwM2mResource(IntegrationTestHelper.SECOND_RESOURCE_ID,
                                                Value.newStringValue("goodbye")),
                                        new LwM2mResource(IntegrationTestHelper.INVALID_RESOURCE_ID,
                                                Value.newStringValue("lolz")) });

        final CreateResponse response = helper.sendCreate(instance, IntegrationTestHelper.GOOD_OBJECT_ID);
        IntegrationTestHelper.assertEmptyResponse(response, ResponseCode.METHOD_NOT_ALLOWED);

        IntegrationTestHelper.assertEmptyResponse(
                helper.sendRead(IntegrationTestHelper.GOOD_OBJECT_ID, IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID),
                ResponseCode.NOT_FOUND);
    }

    @Test
    public void cannot_create_instance_with_non_writable_resource() {
        helper.register();

        final LwM2mObjectInstance instance = new LwM2mObjectInstance(IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID,
                new LwM2mResource[] {
                                        new LwM2mResource(IntegrationTestHelper.FIRST_RESOURCE_ID,
                                                Value.newStringValue("hello")),
                                        new LwM2mResource(IntegrationTestHelper.SECOND_RESOURCE_ID,
                                                Value.newStringValue("goodbye")),
                                        new LwM2mResource(IntegrationTestHelper.EXECUTABLE_RESOURCE_ID,
                                                Value.newStringValue("lolz")) });

        final CreateResponse response = helper.sendCreate(instance, IntegrationTestHelper.GOOD_OBJECT_ID);
        IntegrationTestHelper.assertEmptyResponse(response, ResponseCode.METHOD_NOT_ALLOWED);

        IntegrationTestHelper.assertEmptyResponse(
                helper.sendRead(IntegrationTestHelper.GOOD_OBJECT_ID, IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID),
                ResponseCode.NOT_FOUND);
    }

    @Test
    public void can_create_object_instance_with_empty_payload() {
        helper.register();
        IntegrationTestHelper.assertEmptyResponse(helper.sendCreate(new LwM2mObjectInstance(
                IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]),
                IntegrationTestHelper.MULTIPLE_OBJECT_ID), ResponseCode.CREATED);
    }

    @Test
    public void cannot_create_mandatory_single_object() {
        helper.register();
        IntegrationTestHelper.assertEmptyResponse(helper.sendCreate(new LwM2mObjectInstance(
                IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]),
                IntegrationTestHelper.MANDATORY_SINGLE_OBJECT_ID), ResponseCode.BAD_REQUEST);
    }

    @Test
    public void can_create_mandatory_multiple_object() {
        helper.register();
        IntegrationTestHelper.assertEmptyResponse(helper.sendCreate(new LwM2mObjectInstance(
                IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]),
                IntegrationTestHelper.MANDATORY_MULTIPLE_OBJECT_ID), ResponseCode.CREATED);
        IntegrationTestHelper.assertEmptyResponse(helper.sendCreate(new LwM2mObjectInstance(
                IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]),
                IntegrationTestHelper.MANDATORY_MULTIPLE_OBJECT_ID), ResponseCode.CREATED);
    }

    @Test
    public void cannot_create_more_than_one_single_object() {
        helper.register();
        IntegrationTestHelper.assertEmptyResponse(helper.sendCreate(new LwM2mObjectInstance(
                IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]),
                IntegrationTestHelper.OPTIONAL_SINGLE_OBJECT_ID), ResponseCode.CREATED);
        IntegrationTestHelper.assertEmptyResponse(helper.sendCreate(new LwM2mObjectInstance(
                IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]),
                IntegrationTestHelper.OPTIONAL_SINGLE_OBJECT_ID), ResponseCode.BAD_REQUEST);
    }

    @Test
    public void can_access_mandatory_object_without_create() {
        helper.register();
        IntegrationTestHelper.assertResponse(
                helper.sendRead(IntegrationTestHelper.MANDATORY_SINGLE_OBJECT_ID, 0,
                        IntegrationTestHelper.MANDATORY_SINGLE_RESOURCE_ID),
                ResponseCode.CONTENT,
                new LwM2mResource(IntegrationTestHelper.MANDATORY_SINGLE_RESOURCE_ID, Value.newStringValue(Integer
                        .toString(helper.intResource.getValue()))));
    }

}

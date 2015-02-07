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

import static org.eclipse.leshan.ResponseCode.METHOD_NOT_ALLOWED;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.EXECUTABLE_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.GOOD_OBJECT_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.SECOND_RESOURCE_ID;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.junit.After;
import org.junit.Test;

public class ExecuteTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper();

    @After
    public void stop() {
        helper.stop();
    }

    @Test
    public void cannot_execute_write_only_resource() {
        helper.register();

        helper.sendCreate(IntegrationTestHelper.createGoodObjectInstance("hello", "goodbye"), GOOD_OBJECT_ID);

        final LwM2mResponse response = helper.server.send(helper.getClient(), new ExecuteRequest(GOOD_OBJECT_ID,
                GOOD_OBJECT_INSTANCE_ID, SECOND_RESOURCE_ID, "world".getBytes(), ContentFormat.TEXT));

        IntegrationTestHelper.assertEmptyResponse(response, METHOD_NOT_ALLOWED);
    }

    @Test
    public void can_execute_resource() {
        helper.register();

        helper.sendCreate(IntegrationTestHelper.createGoodObjectInstance("hello", "goodbye"), GOOD_OBJECT_ID);

        final LwM2mResponse response = helper.server.send(helper.getClient(), new ExecuteRequest(GOOD_OBJECT_ID,
                GOOD_OBJECT_INSTANCE_ID, EXECUTABLE_RESOURCE_ID, "world".getBytes(), ContentFormat.TEXT));

        IntegrationTestHelper.assertEmptyResponse(response, ResponseCode.CHANGED);
    }

}

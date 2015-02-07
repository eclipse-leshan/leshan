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

import static org.eclipse.leshan.ResponseCode.CONTENT;
import static org.eclipse.leshan.ResponseCode.DELETED;
import static org.eclipse.leshan.ResponseCode.NOT_FOUND;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.GOOD_OBJECT_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.assertEmptyResponse;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.assertResponse;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.createGoodObjectInstance;

import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.junit.After;
import org.junit.Test;

public class DeleteTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper();

    @After
    public void stop() {
        helper.stop();
    }

    @Test
    public void delete_created_object_instance() {
        helper.register();

        createAndThenAssertDeleted();
    }

    @Test
    public void delete_and_cant_read_object_instance() {
        helper.register();

        createAndThenAssertDeleted();

        assertEmptyResponse(helper.sendRead(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID), NOT_FOUND);
    }

    @Test
    public void delete_and_read_object_is_empty() {
        helper.register();

        createAndThenAssertDeleted();

        assertResponse(helper.sendRead(GOOD_OBJECT_ID), CONTENT, new LwM2mObject(GOOD_OBJECT_ID,
                new LwM2mObjectInstance[0]));
    }

    @Test
    public void cannot_delete_unknown_object_instance() {
        helper.register();

        final LwM2mResponse responseDelete = helper.sendDelete(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID);
        assertEmptyResponse(responseDelete, NOT_FOUND);
    }

    private void createAndThenAssertDeleted() {
        helper.sendCreate(createGoodObjectInstance("hello", "goodbye"), GOOD_OBJECT_ID);

        final LwM2mResponse responseDelete = helper.sendDelete(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID);
        assertEmptyResponse(responseDelete, DELETED);
    }

}

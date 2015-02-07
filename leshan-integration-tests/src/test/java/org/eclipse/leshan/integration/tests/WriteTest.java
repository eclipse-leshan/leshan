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

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.*;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class WriteTest {

    private static final String HELLO = "hello";
    private static final String GOODBYE = "goodbye";

    private IntegrationTestHelper helper = new IntegrationTestHelper();

    @After
    public void stop() {
        helper.stop();
    }

    @Test
    public void can_write_replace_to_resource() {
        helper.register();

        helper.sendCreate(createGoodObjectInstance(HELLO, GOODBYE), GOOD_OBJECT_ID);

        assertEmptyResponse(helper.sendReplace("world", GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, SECOND_RESOURCE_ID),
                ResponseCode.CHANGED);
        assertResponse(helper.sendRead(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, SECOND_RESOURCE_ID),
                ResponseCode.CONTENT, new LwM2mResource(SECOND_RESOURCE_ID, Value.newStringValue("world")));
        assertEquals("world", helper.secondResource.getValue());
    }

    @Test
    public void bad_write_replace_to_resource() {
        helper.register();

        helper.sendCreate(createUnwritableResource("i'm broken!"), BROKEN_OBJECT_ID);

        assertEmptyResponse(
                helper.sendReplace("fix me!", BROKEN_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, BROKEN_RESOURCE_ID),
                ResponseCode.BAD_REQUEST);
        assertResponse(helper.sendRead(BROKEN_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, BROKEN_RESOURCE_ID),
                ResponseCode.CONTENT, new LwM2mResource(BROKEN_RESOURCE_ID, Value.newStringValue("i'm broken!")));
    }

    @Test
    public void cannot_write_to_non_writable_resource() {
        helper.register();

        helper.sendCreate(createGoodObjectInstance(HELLO, GOODBYE), GOOD_OBJECT_ID);

        assertEmptyResponse(
                helper.sendReplace("world", GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, EXECUTABLE_RESOURCE_ID),
                ResponseCode.METHOD_NOT_ALLOWED);
    }

    @Ignore
    @Test
    public void can_write_to_writable_multiple_resource() {
        helper.register();
        helper.sendCreate(new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]), MULTIPLE_OBJECT_ID);

        final LwM2mResource newValues = new LwM2mResource(MULTIPLE_OBJECT_ID, new Value<?>[] {
                                Value.newStringValue(HELLO), Value.newStringValue(GOODBYE) });

        final Map<Integer, byte[]> map = new HashMap<>();
        map.put(0, HELLO.getBytes());
        map.put(1, GOODBYE.getBytes());

        helper.multipleResource.setValue(map);

        assertEmptyResponse(
                helper.sendReplace(newValues, MULTIPLE_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, MULTIPLE_RESOURCE_ID),
                ResponseCode.CHANGED);
    }

    // TODO: This test tests something that is untestable by the LWM2M spec and should
    // probably be deleted. Ignored until this is confirmed
    @Ignore
    @Test
    public void can_write_partial_update_to_resource() {
        helper.register();

        helper.sendCreate(createGoodObjectInstance(HELLO, GOODBYE), GOOD_OBJECT_ID);

        assertEmptyResponse(helper.sendUpdate("world", GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, SECOND_RESOURCE_ID),
                ResponseCode.CHANGED);
        assertResponse(helper.sendRead(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, SECOND_RESOURCE_ID),
                ResponseCode.CONTENT, new LwM2mResource(SECOND_RESOURCE_ID, Value.newStringValue("world")));
        assertEquals("world", helper.secondResource.getValue());
    }

    protected LwM2mObjectInstance createUnwritableResource(final String value) {
        return new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[] { new LwM2mResource(
                BROKEN_RESOURCE_ID, Value.newStringValue(value)) });
    }

}

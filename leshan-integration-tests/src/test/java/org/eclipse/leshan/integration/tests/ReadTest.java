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
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.EXECUTABLE_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.FIRST_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.GOOD_OBJECT_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.GOOD_OBJECT_INSTANCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.INVALID_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.MULTIPLE_OBJECT_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.MULTIPLE_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.SECOND_RESOURCE_ID;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.assertEmptyResponse;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.assertResponse;
import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.createGoodObjectInstance;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.junit.After;
import org.junit.Test;

public class ReadTest {

    private static final String HELLO = "hello";
    private static final String GOODBYE = "goodbye";

    public IntegrationTestHelper helper = new IntegrationTestHelper();

    @After
    public void stop() {
        helper.stop();
    }

    @Test
    public void can_read_empty_object() {
        helper.register();
        assertEmptyResponse(helper.sendRead(GOOD_OBJECT_ID), CONTENT);
    }

    @Test
    public void can_read_object_with_created_instance() {
        helper.register();
        helper.sendCreate(createGoodObjectInstance(HELLO, GOODBYE), GOOD_OBJECT_ID);

        final LwM2mNode objectNode = new LwM2mObject(GOOD_OBJECT_ID,
                new LwM2mObjectInstance[] { new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID,
                        new LwM2mResource[] {
                                                new LwM2mResource(FIRST_RESOURCE_ID, Value.newBinaryValue(HELLO
                                                        .getBytes())),
                                                new LwM2mResource(SECOND_RESOURCE_ID, Value.newBinaryValue(GOODBYE
                                                        .getBytes())) }) });
        assertResponse(helper.sendRead(GOOD_OBJECT_ID), ResponseCode.CONTENT, objectNode);
    }

    @Test
    public void can_read_object_instance() {
        helper.register();
        helper.sendCreate(createGoodObjectInstance(HELLO, GOODBYE), GOOD_OBJECT_ID);

        final LwM2mObjectInstance objectInstanceNode = new LwM2mObjectInstance(
                GOOD_OBJECT_INSTANCE_ID,
                new LwM2mResource[] { new LwM2mResource(FIRST_RESOURCE_ID, Value.newBinaryValue(HELLO.getBytes())),
                                        new LwM2mResource(SECOND_RESOURCE_ID, Value.newBinaryValue(GOODBYE.getBytes())) });
        assertResponse(helper.sendRead(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID), ResponseCode.CONTENT,
                objectInstanceNode);
    }

    @Test
    public void can_read_resource() {
        helper.register();
        helper.sendCreate(createGoodObjectInstance(HELLO, GOODBYE), GOOD_OBJECT_ID);

        assertResponse(helper.sendRead(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, FIRST_RESOURCE_ID),
                ResponseCode.CONTENT, new LwM2mResource(FIRST_RESOURCE_ID, Value.newStringValue(HELLO)));
        assertResponse(helper.sendRead(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, SECOND_RESOURCE_ID),
                ResponseCode.CONTENT, new LwM2mResource(SECOND_RESOURCE_ID, Value.newStringValue(GOODBYE)));
    }

    @Test
    public void cannot_read_non_readable_resource() {
        helper.register();
        helper.sendCreate(createGoodObjectInstance(HELLO, GOODBYE), GOOD_OBJECT_ID);

        assertEmptyResponse(helper.sendRead(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, EXECUTABLE_RESOURCE_ID),
                ResponseCode.METHOD_NOT_ALLOWED);
    }

    @Test
    public void cannot_read_non_existent_resource() {
        helper.register();
        helper.sendCreate(createGoodObjectInstance(HELLO, GOODBYE), GOOD_OBJECT_ID);

        assertEmptyResponse(helper.sendRead(GOOD_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, INVALID_RESOURCE_ID),
                ResponseCode.NOT_FOUND);
    }

    @Test
    public void can_read_multiple_resource() {
        helper.register();
        helper.sendCreate(new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]), MULTIPLE_OBJECT_ID);

        final Map<Integer, byte[]> map = new HashMap<Integer, byte[]>();
        map.put(0, HELLO.getBytes());
        map.put(1, GOODBYE.getBytes());
        helper.multipleResource.setValue(map);

        // This encoding is required because the LwM2mNodeParser doesn't have a way
        // of recognizing the multiple-versus-single resource-ness for the response
        // of reading a resource.
        final byte[] tlvBytes = TlvEncoder.encode(
                new Tlv[] { new Tlv(TlvType.RESOURCE_INSTANCE, null, HELLO.getBytes(), 0),
                                        new Tlv(TlvType.RESOURCE_INSTANCE, null, GOODBYE.getBytes(), 1) }).array();
        final LwM2mNode resource = new LwM2mResource(MULTIPLE_RESOURCE_ID, Value.newStringValue(new String(tlvBytes)));

        assertResponse(helper.sendRead(MULTIPLE_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID, MULTIPLE_RESOURCE_ID),
                ResponseCode.CONTENT, resource);
    }

    @Test
    public void can_read_object_instance_with_multiple_resource() {
        helper.register();
        helper.sendCreate(new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID, new LwM2mResource[0]), MULTIPLE_OBJECT_ID);

        final Map<Integer, byte[]> map = new HashMap<Integer, byte[]>();
        map.put(0, HELLO.getBytes());
        map.put(1, GOODBYE.getBytes());

        helper.multipleResource.setValue(map);

        final LwM2mObjectInstance objectInstance = new LwM2mObjectInstance(GOOD_OBJECT_INSTANCE_ID,
                new LwM2mResource[] { new LwM2mResource(MULTIPLE_RESOURCE_ID, new Value<?>[] {
                                        Value.newBinaryValue(HELLO.getBytes()),
                                        Value.newBinaryValue(GOODBYE.getBytes()) }) });

        assertResponse(helper.sendRead(MULTIPLE_OBJECT_ID, GOOD_OBJECT_INSTANCE_ID), ResponseCode.CONTENT,
                objectInstance);
    }

}

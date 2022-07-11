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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for read security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.read;

import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.TestUtil.assertContentFormat;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ReadMultiValueTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Parameters(name = "{0}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                { ContentFormat.TLV }, //
                { ContentFormat.JSON }, //
                { ContentFormat.SENML_JSON }, //
                { ContentFormat.SENML_CBOR } });
    }

    private ContentFormat contentFormat;

    public ReadMultiValueTest(ContentFormat contentFormat) {
        this.contentFormat = contentFormat;
    }

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void can_read_empty_object() throws InterruptedException {
        // read ACL object
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(contentFormat, 2));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mObject object = (LwM2mObject) response.getContent();
        assertEquals(2, object.getId());
        assertTrue(object.getInstances().isEmpty());

    }

    @Test
    public void can_read_object() throws InterruptedException {
        // read device object
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(contentFormat, 3));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mObject object = (LwM2mObject) response.getContent();
        assertEquals(3, object.getId());

        LwM2mObjectInstance instance = object.getInstance(0);
        assertEquals(0, instance.getId());
    }

    @Test
    public void can_read_object_instance() throws InterruptedException {
        // read device single instance
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, 3, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mObjectInstance instance = (LwM2mObjectInstance) response.getContent();
        assertEquals(0, instance.getId());
    }
}

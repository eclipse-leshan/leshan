/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.read;

import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.TestUtil.assertContentFormat;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ReadCompositeTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Parameters(name = "{0}{1}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                                // {request content format, response content format}
                                { ContentFormat.SENML_JSON, ContentFormat.SENML_JSON }, //
                                { ContentFormat.SENML_CBOR, ContentFormat.SENML_CBOR } });
    }

    private ContentFormat requestContentFormat;
    private ContentFormat responseContentFormat;

    public ReadCompositeTest(ContentFormat requestContentFormat, ContentFormat responseContentFormat) {
        this.requestContentFormat = requestContentFormat;
        this.responseContentFormat = responseContentFormat;
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
    public void can_read_resources() throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1/0/1"));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(responseContentFormat, response);

        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
        assertEquals(0, resource.getId());
        assertEquals(Type.STRING, resource.getType());

        resource = (LwM2mSingleResource) response.getContent("/1/0/1");
        assertEquals(1, resource.getId());
        assertEquals(Type.INTEGER, resource.getType());

    }

    @Test
    public void can_read_resource_instance() throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/2000/0/10/1"));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(responseContentFormat, response);

        LwM2mResourceInstance resource = (LwM2mResourceInstance) response.getContent("/2000/0/10/1");
        assertEquals(1, resource.getId());
        assertEquals(Type.STRING, resource.getType());

    }

    @Test
    public void can_read_resource_and_instance() throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1"));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(responseContentFormat, response);

        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
        assertEquals(0, resource.getId());
        assertEquals(Type.STRING, resource.getType());

        LwM2mObject object = (LwM2mObject) response.getContent("/1");
        assertEquals(1, object.getId());
        assertEquals(1, object.getInstances().size());
    }
}

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
package org.eclipse.leshan.integration.tests.send;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.integration.tests.util.Callback;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.integration.tests.util.SynchronousSendListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SendTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper() {
        @Override
        protected ObjectsInitializer createObjectsInitializer() {
            return new ObjectsInitializer(new StaticModel(createObjectModels()));
        };
    };

    @Parameters(name = "{0}{1}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                                // {content format}
                                { ContentFormat.SENML_JSON }, //
                                { ContentFormat.SENML_CBOR } });
    }

    private ContentFormat contentformat;

    public SendTest(ContentFormat contentformat) {
        this.contentformat = contentformat;
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
    public void can_send_resources() throws InterruptedException, TimeoutException {
        // Define send listener
        SynchronousSendListener listener = new SynchronousSendListener();
        helper.server.getSendService().addListener(listener);

        // Send Data
        helper.waitForRegistrationAtClientSide(1);
        ServerIdentity server = helper.client.getRegisteredServers().values().iterator().next();
        SendResponse response = helper.client.sendData(server, contentformat, Arrays.asList("/3/0/1", "/3/0/2"), 1000);
        assertTrue(response.isSuccess());

        // wait for data and check result
        listener.waitForData(1, TimeUnit.SECONDS);
        assertNotNull(listener.getRegistration());
        Map<String, LwM2mNode> data = listener.getData();
        LwM2mResource modelnumber = (LwM2mResource) data.get("/3/0/1");
        assertEquals(modelnumber.getId(), 1);
        assertEquals(modelnumber.getValue(), "IT-TEST-123");

        LwM2mResource serialnumber = (LwM2mResource) data.get("/3/0/2");
        assertEquals(serialnumber.getId(), 2);
        assertEquals(serialnumber.getValue(), "12345");
    }

    @Test
    public void can_send_resources_asynchronously() throws InterruptedException, TimeoutException {
        // Define send listener
        SynchronousSendListener listener = new SynchronousSendListener();
        helper.server.getSendService().addListener(listener);

        // Send Data
        helper.waitForRegistrationAtClientSide(1);
        Callback<SendResponse> callback = new Callback<>();
        ServerIdentity server = helper.client.getRegisteredServers().values().iterator().next();
        helper.client.sendData(server, contentformat, Arrays.asList("/3/0/1", "/3/0/2"), 1000, callback, callback);
        callback.waitForResponse(1000);
        assertTrue(callback.getResponse().isSuccess());

        // wait for data and check result
        listener.waitForData(1, TimeUnit.SECONDS);
        assertNotNull(listener.getRegistration());
        Map<String, LwM2mNode> data = listener.getData();
        LwM2mResource modelnumber = (LwM2mResource) data.get("/3/0/1");
        assertEquals(modelnumber.getId(), 1);
        assertEquals(modelnumber.getValue(), "IT-TEST-123");

        LwM2mResource serialnumber = (LwM2mResource) data.get("/3/0/2");
        assertEquals(serialnumber.getId(), 2);
        assertEquals(serialnumber.getValue(), "12345");
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.integration.tests.util.Callback;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.integration.tests.util.SynchronousSendListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SendTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentFormats")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllContentFormat {
    }

    static Stream<ContentFormat> contentFormats() {
        return Stream.of(//
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR);
    }

    protected IntegrationTestHelper helper = new IntegrationTestHelper() {
        @Override
        protected ObjectsInitializer createObjectsInitializer() {
            return new ObjectsInitializer(new StaticModel(createObjectModels()));
        };
    };

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
    public void can_send_resources(ContentFormat contentformat) throws InterruptedException, TimeoutException {
        // Define send listener
        SynchronousSendListener listener = new SynchronousSendListener();
        helper.server.getSendService().addListener(listener);

        // Send Data
        helper.waitForRegistrationAtClientSide(1);
        ServerIdentity server = helper.client.getRegisteredServers().values().iterator().next();
        SendResponse response = helper.client.getSendService().sendData(server, contentformat,
                Arrays.asList("/3/0/1", "/3/0/2"), 1000);
        assertTrue(response.isSuccess());

        // wait for data and check result
        listener.waitForData(1, TimeUnit.SECONDS);
        assertNotNull(listener.getRegistration());
        Map<LwM2mPath, LwM2mNode> data = listener.getNodes();
        LwM2mResource modelnumber = (LwM2mResource) data.get(new LwM2mPath("/3/0/1"));
        assertEquals(modelnumber.getId(), 1);
        assertEquals(modelnumber.getValue(), "IT-TEST-123");

        LwM2mResource serialnumber = (LwM2mResource) data.get(new LwM2mPath("/3/0/2"));
        assertEquals(serialnumber.getId(), 2);
        assertEquals(serialnumber.getValue(), "12345");
    }

    @TestAllContentFormat
    public void can_send_resources_asynchronously(ContentFormat contentformat)
            throws InterruptedException, TimeoutException {
        // Define send listener
        SynchronousSendListener listener = new SynchronousSendListener();
        helper.server.getSendService().addListener(listener);

        // Send Data
        helper.waitForRegistrationAtClientSide(1);
        Callback<SendResponse> callback = new Callback<>();
        ServerIdentity server = helper.client.getRegisteredServers().values().iterator().next();
        helper.client.getSendService().sendData(server, contentformat, Arrays.asList("/3/0/1", "/3/0/2"), 1000,
                callback, callback);
        callback.waitForResponse(1000);
        assertTrue(callback.getResponse().isSuccess());

        // wait for data and check result
        listener.waitForData(1, TimeUnit.SECONDS);
        assertNotNull(listener.getRegistration());
        Map<LwM2mPath, LwM2mNode> data = listener.getNodes();
        LwM2mResource modelnumber = (LwM2mResource) data.get(new LwM2mPath("/3/0/1"));
        assertEquals(modelnumber.getId(), 1);
        assertEquals(modelnumber.getValue(), "IT-TEST-123");

        LwM2mResource serialnumber = (LwM2mResource) data.get(new LwM2mPath("/3/0/2"));
        assertEquals(serialnumber.getId(), 2);
        assertEquals(serialnumber.getValue(), "12345");
    }
}

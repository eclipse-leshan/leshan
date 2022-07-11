/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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

import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.linkParser;
import static org.junit.Assert.assertNotNull;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.integration.tests.lockstep.LockStepLwM2mClient;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.integration.tests.util.SynchronousSendListener;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LockStepSendTest {

    public IntegrationTestHelper helper = new IntegrationTestHelper() {
        @Override
        protected LeshanServerBuilder createServerBuilder() {
            Configuration coapConfig = LeshanServerBuilder.createDefaultCoapConfiguration();

            // configure retransmission, with this configuration a request without ACK should timeout in ~200*5ms
            coapConfig.set(CoapConfig.ACK_TIMEOUT, 200, TimeUnit.MILLISECONDS) //
                    .set(CoapConfig.ACK_INIT_RANDOM, 1f) //
                    .set(CoapConfig.ACK_TIMEOUT_SCALE, 1f) //
                    .set(CoapConfig.MAX_RETRANSMIT, 4);

            LeshanServerBuilder builder = super.createServerBuilder();
            builder.setCoapConfig(coapConfig);

            return builder;
        };
    };

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
    }

    @After
    public void stop() {
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void register_send_with_invalid_payload() throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getUnsecuredAddress());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        helper.waitForRegistrationAtServerSide(1);

        // Define send listener
        SynchronousSendListener listener = new SynchronousSendListener();
        helper.server.getSendService().addListener(listener);

        // Send "Send Request" with invalid payload
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("/3/0/1"), LwM2mSingleResource.newStringResource(1, "data"));
        Request sendRequest = client.createCoapRequest(new SendRequest(ContentFormat.SENML_CBOR, nodes));
        sendRequest.setPayload(new byte[] { 0x00, 0x10 });
        client.sendCoapRequest(sendRequest);

        // wait for error
        listener.waitForError(1, TimeUnit.SECONDS);
        assertNotNull(listener.getError());
    }
}

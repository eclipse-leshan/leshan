/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.lockstep;

import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.linkParser;
import static org.junit.Assert.assertEquals;

import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.Callback;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider.Builder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LockStepTest {

    public IntegrationTestHelper helper = new IntegrationTestHelper() {

        @Override
        protected Builder createEndpointsProviderBuilder() {
            Builder builder = super.createEndpointsProviderBuilder();
            Configuration coapConfig = builder.createDefaultConfiguration();

            // configure retransmission, with this configuration a request without ACK should timeout in ~200*5ms
            coapConfig.set(CoapConfig.ACK_TIMEOUT, 200, TimeUnit.MILLISECONDS) //
                    .set(CoapConfig.ACK_INIT_RANDOM, 1f) //
                    .set(CoapConfig.ACK_TIMEOUT_SCALE, 1f) //
                    .set(CoapConfig.MAX_RETRANSMIT, 4);

            builder.setConfiguration(coapConfig);
            return builder;
        }
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
    public void register_with_uq_binding_in_lw_1_0() throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.0", EnumSet.of(BindingMode.U, BindingMode.Q),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        helper.waitForRegistrationAtServerSide(1);
    }

    @Test
    public void register_with_ut_binding_in_lw_1_1() throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.1", EnumSet.of(BindingMode.U, BindingMode.T),
                        null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        helper.waitForRegistrationAtServerSide(1);
    }

    @Test
    public void register_update_with_invalid_binding_for_lw_1_1() throws Exception {
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getEndpoint(Protocol.COAP).getURI());

        // register with valid binding for 1.1
        RegisterRequest validRegisterRequest = new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.1",
                EnumSet.of(BindingMode.U), null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()),
                null);
        Token token = client.sendLwM2mRequest(validRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        helper.waitForRegistrationAtServerSide(1);

        // update with valid binding for 1.1
        UpdateRequest validUpdateRequest = new UpdateRequest("/rd/" + helper.getLastRegistration().getId(), 60l, null,
                EnumSet.of(BindingMode.U), null, null);
        token = client.sendLwM2mRequest(validUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.CHANGED).go();

        // register with invalid binding for 1.1
        Request invalidRegisterRequest = client.createCoapRequest(validRegisterRequest);
        invalidRegisterRequest.getOptions().removeUriQuery("b=U");
        invalidRegisterRequest.getOptions().addUriQuery("b=UQ");
        token = client.sendCoapRequest(invalidRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();

        // update with invalid binding for 1.1
        Request invalidUpdateRequest = client.createCoapRequest(validRegisterRequest);
        invalidUpdateRequest.getOptions().removeUriQuery("b=U");
        invalidUpdateRequest.getOptions().addUriQuery("b=UQ");
        token = client.sendCoapRequest(invalidUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();
    }

    @Test
    public void register_update_with_invalid_binding_for_lw_1_0() throws Exception {
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getEndpoint(Protocol.COAP).getURI());

        // register with valid binding for 1.0
        RegisterRequest validRegisterRequest = new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.0",
                EnumSet.of(BindingMode.U), null, null, linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()),
                null);
        Token token = client.sendLwM2mRequest(validRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.CREATED).go();
        helper.waitForRegistrationAtServerSide(1);

        // update with valid binding for 1.0
        UpdateRequest validUpdateRequest = new UpdateRequest("/rd/" + helper.getLastRegistration().getId(), 60l, null,
                EnumSet.of(BindingMode.U), null, null);
        token = client.sendLwM2mRequest(validUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.CHANGED).go();

        // register with invalid binding for 1.0
        Request invalidRegisterRequest = client.createCoapRequest(validRegisterRequest);
        invalidRegisterRequest.getOptions().removeUriQuery("b=U");
        invalidRegisterRequest.getOptions().addUriQuery("b=UT");
        token = client.sendCoapRequest(invalidRegisterRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();

        // update with invalid binding for 1.0
        Request invalidUpdateRequest = client.createCoapRequest(validRegisterRequest);
        invalidUpdateRequest.getOptions().removeUriQuery("b=U");
        invalidUpdateRequest.getOptions().addUriQuery("b=UT");
        token = client.sendCoapRequest(invalidUpdateRequest);
        client.expectResponse().token(token).code(ResponseCode.BAD_REQUEST).go();
    }

    @Test
    public void sync_send_without_acknowleged() throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        helper.waitForRegistrationAtServerSide(1);

        // Send read
        Future<ReadResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<ReadResponse>() {
            @Override
            public ReadResponse call() throws Exception {
                // send a request with 3 seconds timeout
                return helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3), 3000);
            }
        });
        // Request should timedout in ~1s we don't send ACK
        ReadResponse response = future.get(1500, TimeUnit.MILLISECONDS);
        Assert.assertNull("we should timeout", response);
    }

    @Test
    public void sync_send_with_acknowleged_request_without_response() throws Exception {
        // Register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        helper.waitForRegistrationAtServerSide(1);

        // Send read
        Future<ReadResponse> future = Executors.newSingleThreadExecutor().submit(new Callable<ReadResponse>() {
            @Override
            public ReadResponse call() throws Exception {
                // send a request with 3 seconds timeout
                return helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3), 3000);
            }
        });

        // Acknowledge the response
        client.expectRequest().storeMID("R").go();
        client.sendEmpty(Type.ACK).loadMID("R").go();

        // Request should timedout in ~3s as we send the ACK
        Thread.sleep(1500);
        Assert.assertFalse("we should still wait for response", future.isDone());
        ReadResponse response = future.get(2000, TimeUnit.MILLISECONDS);
        Assert.assertNull("we should timeout", response);
    }

    @Test
    public void async_send_without_acknowleged() throws Exception {
        // register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        helper.waitForRegistrationAtServerSide(1);

        // send read
        Callback<ReadResponse> callback = new Callback<ReadResponse>();
        helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3), 3000l, callback, callback);

        // Request should timedout in ~1s we don't send ACK
        callback.waitForResponse(1500);
        Assert.assertTrue("we should timeout", callback.getException() instanceof TimeoutException);
        assertEquals(TimeoutException.Type.COAP_TIMEOUT, ((TimeoutException) callback.getException()).getType());
    }

    @Test
    public void async_send_with_acknowleged_request_without_response() throws Exception {
        // register client
        LockStepLwM2mClient client = new LockStepLwM2mClient(helper.server.getEndpoint(Protocol.COAP).getURI());
        Token token = client.sendLwM2mRequest(
                new RegisterRequest(helper.getCurrentEndpoint(), 60l, "1.1", EnumSet.of(BindingMode.U), null, null,
                        linkParser.parseCoreLinkFormat("</1>,</2>,</3>".getBytes()), null));
        client.expectResponse().token(token).go();
        helper.waitForRegistrationAtServerSide(1);

        // send read
        Callback<ReadResponse> callback = new Callback<ReadResponse>();
        helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3), 3000l, callback, callback);

        // Acknowledge the response
        client.expectRequest().storeMID("R").go();
        client.sendEmpty(Type.ACK).loadMID("R").go();

        // Request should timedout in ~3s as we send a ack
        Thread.sleep(1500);
        Assert.assertTrue("we should still wait for response", callback.getException() == null);
        callback.waitForResponse(2000);
        Assert.assertTrue("we should timeout", callback.getException() instanceof TimeoutException);
        assertEquals(TimeoutException.Type.RESPONSE_TIMEOUT, ((TimeoutException) callback.getException()).getType());
    }
}

/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein (Bosch Software Innovations GmbH)
 *                     - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.integration.tests.util.QueuedModeLeshanClient;
import org.eclipse.leshan.integration.tests.util.QueuedModeLeshanClient.OnGetCallback;
import org.eclipse.leshan.server.queue.RequestState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for Queue Mode feature.
 */
public class QueueModeTest {

    private QueueModeIntegrationTestHelper helper = new QueueModeIntegrationTestHelper();
    private CountDownLatch countDownLatch;
    private final OnGetCallback doNothingOnGet = new OnGetCallback() {
        @Override
        public boolean handleGet(final CoapExchange coapExchange) {
            return false;
        }
    };

    @Before
    public void start() {
        countDownLatch = new CountDownLatch(1);
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.getQueueReactor().start();
        helper.client.start();
    }

    @After
    public void stop() {
        helper.client.stop();
        helper.server.stop();
        helper.getQueueReactor().stop(200, TimeUnit.MILLISECONDS);
    }

    @Test
    public void verifyRequestIsSentImmediatelyIfPossible() throws Exception {
        // client registration
        RegisterResponse registerResponse = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER, 10000L, null,
                BindingMode.UQ, null, null));

        assertEquals("client was not registered", ResponseCode.CREATED, registerResponse.getCode());

        helper.server.send(helper.getClient(), new ReadRequest(3, 0), new ResponseCallback<ValueResponse>() {
            @Override
            public void onResponse(final ValueResponse response) {
                countDownLatch.countDown();
            }
        }, new ErrorCallback() {
            @Override
            public void onError(final Exception e) {
                throw new IllegalStateException("unexpected exception occurred: ", e);
            }
        });
        if (!countDownLatch.await(2, TimeUnit.SECONDS)) {
            fail("response from client was not received within timeout");
        }
    }

    @Test
    public void verifyRequestIsDeferredIfClientDoesNotRespond() throws Exception {
        // client registration
        RegisterResponse registerResponse = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER, 10000L, null,
                BindingMode.UQ, null, null));

        assertEquals("client was not registered", ResponseCode.CREATED, registerResponse.getCode());

        // client is set up to "sleep", i.e. not to respond
        QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;

        client.setOnGetCallback(doNothingOnGet);

        helper.server.send(helper.getClient(), new ReadRequest(3, 0), new ResponseCallback<ValueResponse>() {
            @Override
            public void onResponse(final ValueResponse response) {
                countDownLatch.countDown();
            }
        }, new ErrorCallback() {
            @Override
            public void onError(final Exception e) {
                throw new IllegalStateException("unexpected exception occurred: ", e);
            }
        });
        if (countDownLatch.await(3, TimeUnit.SECONDS)) {
            fail("response from client is expected to time out");
        }
        // assert that queue has one deferred request
        assertEquals(1, helper.getRequestQueue().getRequests(helper.getClient().getEndpoint()).size());
        assertEquals(RequestState.DEFERRED, helper.getRequestQueue().getRequests(helper.getClient().getEndpoint())
                .iterator().next().getRequestState());
    }

    @Test
    public void verifyPendingRequestIsSentAfterClientsUpdate() throws Exception {
        // client registration
        RegisterResponse registerResponse = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER, 10000L, null,
                BindingMode.UQ, null, null));

        assertEquals("client was not registered", ResponseCode.CREATED, registerResponse.getCode());

        // client is set up to not respond the next request, but to respond afterwards
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        final CountDownLatch acceptCountDownLatch = new CountDownLatch(1);

        client.setOnGetCallback(new OnGetCallback() {
            @Override
            public boolean handleGet(final CoapExchange coapExchange) {
                client.setOnGetCallback(new OnGetCallback() {
                    @Override
                    public boolean handleGet(final CoapExchange coapExchange) {
                        return true;
                    }
                });
                countDownLatch.countDown();
                return false;
            }
        });

        helper.server.send(helper.getClient(), new ReadRequest(3, 0), new ResponseCallback<ValueResponse>() {
            @Override
            public void onResponse(final ValueResponse response) {
                acceptCountDownLatch.countDown();
            }
        }, new ErrorCallback() {
            @Override
            public void onError(final Exception e) {
                throw new IllegalStateException("unexpected exception occurred: ", e);
            }
        });

        // after first (unresponded) request
        if (!countDownLatch.await(1, TimeUnit.SECONDS)) {
            fail("request was not properly processed");
        }

        // Needed to ensure that the request has been DEFERRED, otherwise
        // update would not have the expected effect here.
        Thread.sleep(700);
        helper.client.send(new UpdateRequest(registerResponse.getRegistrationID(), null, null, null, null));

        if (!acceptCountDownLatch.await(2, TimeUnit.SECONDS)) {
            fail("server never received the response");
        }

        // assert that queue has one executed request
        assertEquals(1, helper.getRequestQueue().getRequests(helper.getClient().getEndpoint()).size());
        assertEquals(RequestState.EXECUTED, helper.getRequestQueue().getRequests(helper.getClient().getEndpoint())
                .iterator().next().getRequestState());
    }

    @Test
    public void verifyRequestIsElapsedAfterSendExpiration() throws Exception {
        // client registration
        RegisterResponse registerResponse = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER, 10000L, null,
                BindingMode.UQ, null, null));

        assertEquals("client was not registered", ResponseCode.CREATED, registerResponse.getCode());

        // client is set up to not respond the first request, then to respond
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;

        helper.getQueueRequestSender().setSendExpirationInterval(10, TimeUnit.MILLISECONDS);
        helper.getQueueRequestSender().setKeepExpirationInterval(10, TimeUnit.SECONDS);

        client.setOnGetCallback(doNothingOnGet);

        helper.server.send(helper.getClient(), new ReadRequest(3, 0), new ResponseCallback<ValueResponse>() {

            @Override
            public void onResponse(final ValueResponse response) {
                countDownLatch.countDown();
            }
        }, new ErrorCallback() {
            @Override
            public void onError(final Exception e) {
                throw new IllegalStateException("unexpected exception occurred: ", e);
            }
        });

        if (countDownLatch.await(1, TimeUnit.SECONDS)) {
            fail("unexpected response from client");
        }

        helper.client.send(new UpdateRequest(registerResponse.getRegistrationID(), null, null, null, null));

        // assert that queue has one executed request
        assertEquals(1, helper.getRequestQueue().getRequests(helper.getClient().getEndpoint()).size());
        assertEquals(RequestState.TTL_ELAPSED, helper.getRequestQueue().getRequests(helper.getClient().getEndpoint())
                .iterator().next().getRequestState());
    }

    @Test
    public void verifyRequestIsUnqueuedAfterKeepExpiration() throws Exception {
        // client registration
        RegisterResponse registerResponse = helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER, 10000L, null,
                BindingMode.UQ, null, null));

        assertEquals("client was not registered", ResponseCode.CREATED, registerResponse.getCode());

        // client is set up to not respond the first request, then to respond
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;

        helper.getQueueRequestSender().setSendExpirationInterval(10, TimeUnit.MILLISECONDS);
        helper.getQueueRequestSender().setKeepExpirationInterval(11, TimeUnit.MILLISECONDS);

        client.setOnGetCallback(doNothingOnGet);

        helper.server.send(helper.getClient(), new ReadRequest(3, 0), new ResponseCallback<ValueResponse>() {
            @Override
            public void onResponse(final ValueResponse response) {
                countDownLatch.countDown();
            }
        }, new ErrorCallback() {
            @Override
            public void onError(final Exception e) {
                throw new IllegalStateException("unexpected exception occurred: ", e);
            }
        });

        if (countDownLatch.await(1, TimeUnit.SECONDS)) {
            fail("unexpected response from client");
        }

        helper.client.send(new UpdateRequest(registerResponse.getRegistrationID(), null, null, null, null));

        // assert that queue has no requests
        assertEquals(0, helper.getRequestQueue().getRequests(helper.getClient().getEndpoint()).size());
    }
}

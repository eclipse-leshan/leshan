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
 *     Daniel Maier (Bosch Software Innovations GmbH)
 *                     - initial API and implementation
 *     Michael Kremser (Bosch Software Innovations GmbH)
 *                     - extended tests
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.integration.tests.util.QueueModeLeshanServer;
import org.eclipse.leshan.integration.tests.util.QueuedModeLeshanClient;
import org.eclipse.leshan.integration.tests.util.QueuedModeLeshanClient.OnGetCallback;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.eclipse.leshan.server.queue.impl.InMemoryMessageStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Integration tests for Queue Mode feature.
 */
public class QueueModeTest {
    private final static Logger LOG = LoggerFactory.getLogger(QueueModeTest.class);
    private final static Long TIMEOUT = QueueModeIntegrationTestHelper.ACK_TIMEOUT+1000;
    public static final boolean DEREGISTER = true;
    private CountDownLatch countDownLatch;
    private QueueModeIntegrationTestHelper helper;

    private final OnGetCallback doNothingOnGet = new OnGetCallback() {
        @Override
        public boolean handleGet(final CoapExchange coapExchange) {
            LOG.trace("Setup client not to respond");
            return false;
        }
    };
    private final OnGetCallback respondOnGet = new OnGetCallback() {
        @Override
        public boolean handleGet(final CoapExchange coapExchange) {
            LOG.trace("Received coapExchange: {}", coapExchange.getRequestOptions().getUriPathString());
            LOG.trace("Setup client to respond");
            return true;
        }
    };

    @Before
    public void start() {
        helper = new QueueModeIntegrationTestHelper();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        countDownLatch = new CountDownLatch(1);
        helper.waitForRegistration(1);
    }

    @After
    public void stop() {
        helper.client.stop(DEREGISTER);
        helper.server.stop();
    }

    @Test
    public void verifyRequestIsNotRemovedFromQueueIfClientDoesNotRespond() throws Exception {
        // client is set up to "sleep", i.e. not to respond
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        client.setOnGetCallback(doNothingOnGet);

        helper.server.send(helper.getClient(), new ReadRequest(3, 0), newReadResponseCallback(countDownLatch),
                newErrorCallback());
        // assert that queue has one request left in processing state still
        assertQueueHasMessageCount(1, 5000);
    }

    @Test
    public void verifyRequestIsSentImmediatelyIfPossible() throws Exception {
        helper.server.send(helper.getClient(), new ReadRequest(3, 0), newReadResponseCallback(countDownLatch),
                newErrorCallback());
        if (!countDownLatch.await(2, TimeUnit.SECONDS)) {
            fail("response from client was not received within timeout");
        }
    }

    @Test
    public void verifyRequestIsNotRemovedFromQueueAndSubsequentRequestIsNotSentIfClientDoesNotRespond() throws Exception {
        final CountDownLatch acceptCountDownLatch = new CountDownLatch(1);

        // client is set up to do not respond, i.e. to cause request timeout
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        client.setOnGetCallback(doNothingOnGet);

        final ResponseCallback<ReadResponse> responseCallback = newReadResponseCallback(acceptCountDownLatch);
        final ErrorCallback errorCallback = newErrorCallback();

        // send request but no response should be received
        helper.server.send(helper.getClient(), new ReadRequest(3, 0, 1), responseCallback, errorCallback);
        // assert that queue has one request left in processing state still
        assertQueueHasMessageCount(1, 3000);

        // as client has not yet sent any register-update or notify, sending next request
        // request should only be queued and not sent
        helper.server.send(helper.getClient(), new ReadRequest(3, 0, 2), responseCallback, errorCallback);
        if (acceptCountDownLatch.await(3, TimeUnit.SECONDS)) {
            fail("response from client after first request timed out is unexpected");
        }

        // assert that queue has one additional new request
        final List<QueuedRequest> requests = ((InMemoryMessageStore)
                ((QueueModeLeshanServer)helper.server).getMessageStore())
                .retrieveAll(helper.getClient().getEndpoint());
        assertEquals(2, requests.size());
    }

    @Test
    public void verifyPendingRequestIsSentAfterClientUpdate() throws Exception {
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        final CountDownLatch acceptCountDownLatch = new CountDownLatch(1);

        // client is set up not to respond the next request, but to respond afterwards
        client.setOnGetCallback(new OnGetCallback() {
            @Override
            public boolean handleGet(final CoapExchange coapExchange) {
                //second GET request client is set to respond back.
                client.setOnGetCallback(new OnGetCallback() {
                    @Override
                    public boolean handleGet(final CoapExchange coapExchange) {
                        return true;
                    }
                });
                countDownLatch.countDown();
                return false; //client doesn't respond.
            }
        });

        //Send a read request. Will be sent immediately as it is the first message in the queue.
        helper.server.send(helper.getClient(), new ReadRequest(3, 0, 1),
                newReadResponseCallback(acceptCountDownLatch),
                newErrorCallback());

        // after first (unresponded) request, coundDown should be zero.
        if (!countDownLatch.await(1, TimeUnit.SECONDS)) {
            fail("request was not properly processed");
        }

        // Needed to ensure that the request is left in Queue
        // update would not have the expected effect here.
        assertQueueHasMessageCount(1, 5000);

        //Registration engine sends a registration-update automatically after(CUSTOM_LIFETIME (3) - 10%) = 2.7 seconds

        //when server has received a response for the retry, acceptCoundDown should be zero.
        if (!acceptCountDownLatch.await(7, TimeUnit.SECONDS)) {
            fail("server never received the response");
        }

        final List<QueuedRequest> queuedRequests = ((InMemoryMessageStore)
                ((QueueModeLeshanServer)helper.server).getMessageStore())
                .retrieveAll(helper.getClient().getEndpoint());
        assertEquals(0, queuedRequests.size());
    }

    @Test
    public void verifyAllMessagesSentOneAfterAnotherIfClientIsReachable() throws Exception {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationRegistry().addListener(listener);
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;

        //set client to respond to GET
        client.setOnGetCallback(respondOnGet);
        //Send an Observe request. Will be sent immediately as it is the first message in the queue.
        ObserveResponse observeResponse = helper.server.send(helper.getClient(), new ObserveRequest(3, 0, 15));
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getClient().getRegistrationId(), observation.getRegistrationId());

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getClient(),
                new WriteRequest(3, 0, 15, "Europe/Berlin"));

        waitForInterval(TIMEOUT);
        assertQueueIsEmpty(5000L);

        // wait for notify
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receievedNotify().get());
    }

    @Test
    public void verifyAllMessagesRemovedOnClientDeregister() throws Exception {
        // client is set up to do not respond, i.e. to cause request timeout
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        client.setOnGetCallback(doNothingOnGet);

        //now send some read requests
        final CountDownLatch acceptCountDownLatch = new CountDownLatch(1);
        //Send a read request. Will be sent immediately as it is the first message in the queue.
        helper.server.send(helper.getClient(), new ReadRequest(3, 0, 1),
                newReadResponseCallback(acceptCountDownLatch),
                newErrorCallback());

        helper.server.send(helper.getClient(), new ReadRequest(3, 0, 15),
                newReadResponseCallback(acceptCountDownLatch),
                newErrorCallback());

        waitForInterval(TIMEOUT);
        assertQueueHasMessageCount(2, 5000);

        //Send de-register
        helper.client.stop(DEREGISTER);
        assertQueueIsEmpty(3000);
    }

    @Test
    public void verifyMessageSentAfterClientNotify() throws Exception {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationRegistry().addListener(listener);
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;

        //set client to respond to GET
        client.setOnGetCallback(respondOnGet);
        //Send an Observe request. Will be sent immediately as it is the first message in the queue.
        ObserveResponse observeResponse = helper.server.send(helper.getClient(), new ObserveRequest(3, 0, 15));
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getClient().getRegistrationId(), observation.getRegistrationId());

        CountDownLatch acceptCountDownLatch = new CountDownLatch(1);
        //set client NOT to respond to GET
        client.setOnGetCallback(doNothingOnGet);
        helper.server.send(helper.getClient(), new ReadRequest(3, 0, 15),
                newReadResponseCallback(acceptCountDownLatch),
                newErrorCallback());
        //read request should be left in Queue still.
        assertQueueHasMessageCount(1, 5000);
        //Wait for read request timeout
        waitForInterval(TIMEOUT);

        //set client to respond to GET
        client.setOnGetCallback(respondOnGet);
        // write device timezone which is sent using sync call of LwM2mRequestSender
        helper.server.send(helper.getClient(),
                new WriteRequest(3, 0, 15, "Europe/Berlin"));

        // wait for notify
        listener.waitForNotification(2000);

        //client could take sometime to send notify()
        // wait for Read request response
        waitForInterval(TIMEOUT);
        assertQueueIsEmpty(3000L);
    }

    private ResponseCallback<ReadResponse> newReadResponseCallback(final CountDownLatch acceptCountDownLatch) {
        return new ResponseCallback<ReadResponse>() {
            @Override
            public void onResponse(final ReadResponse response) {
                acceptCountDownLatch.countDown();
            }
        };
    }

    private ResponseCallback<WriteResponse> newWriteResponseCallback(final CountDownLatch acceptCountDownLatch) {
        return new ResponseCallback<WriteResponse>() {
            @Override
            public void onResponse(final WriteResponse response) {
                acceptCountDownLatch.countDown();
            }
        };
    }

    private ErrorCallback newErrorCallback() {
        return new ErrorCallback() {
            @Override
            public void onError(final Exception e) {
                throw new IllegalStateException("unexpected exception occurred: ", e);
            }
        };
    }

    private void assertQueueHasMessageCount(int count, long timeout)
            throws InterruptedException {
        long interval = 100;
        long duration = 0;
        int queuedRequestCount = 0;
        do {
            Thread.sleep(interval);
            duration += interval;
            InMemoryMessageStore messageStore = (InMemoryMessageStore)
                    ((QueueModeLeshanServer) helper.server).getMessageStore();
            queuedRequestCount = messageStore.retrieveAll(helper.getClient().getEndpoint()).size();
        } while (queuedRequestCount < count && duration <= timeout);
        assertTrue("Expected to have at least "+ count +" queued request in queue but have ["+queuedRequestCount +"]",
                queuedRequestCount == count);
    }

    private void assertQueueIsEmpty(long timeout)
            throws InterruptedException {
        long interval = 100;
        long duration = 0;
        boolean empty = false;
        MessageStore messageStore = ((QueueModeLeshanServer) helper.server).getMessageStore();
        do {
            Thread.sleep(interval);
            duration += interval;
            empty = helper.getClient() == null ||
                    (helper.getClient() !=null &&
                    messageStore
                    .isEmpty(helper.getClient().getEndpoint()));
        } while (!empty && duration <= timeout);
        assertTrue("Expected an empty queue but has some messages", empty);
    }

    private boolean waitForInterval(long timeout) throws InterruptedException {
        Thread.sleep(timeout);
        return true;
    }

    private final class TestObservationListener implements ObservationRegistryListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean receivedNotify = new AtomicBoolean();

        @Override
        public void newValue(final Observation observation, final LwM2mNode value) {
            receivedNotify.set(true);
            latch.countDown();
        }

        @Override
        public void cancelled(final Observation observation) {
            latch.countDown();
        }

        @Override
        public void newObservation(final Observation observation) {
        }

        public AtomicBoolean receievedNotify() {
            return receivedNotify;
        }

        public LwM2mNode getContent() {
            return null;
        }

        public void waitForNotification(long timeout) throws InterruptedException {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        }
    }
}

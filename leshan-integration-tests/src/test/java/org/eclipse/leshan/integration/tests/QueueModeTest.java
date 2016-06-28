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

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.integration.tests.util.QueuedModeLeshanClient;
import org.eclipse.leshan.integration.tests.util.QueuedModeLeshanClient.OnGetCallback;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.eclipse.leshan.server.queue.MessageStore;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.eclipse.leshan.server.queue.impl.InMemoryMessageStore;
import org.eclipse.leshan.server.response.ResponseListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for Queue Mode feature.
 */
public class QueueModeTest {
    private final static Logger LOG = LoggerFactory.getLogger(QueueModeTest.class);
    private final static Long TIMEOUT = QueueModeIntegrationTestHelper.ACK_TIMEOUT + 1000;
    private final static String TEST_REQUEST_TICKET = "TestRequestTicket_";
    public static final boolean DEREGISTER = true;
    private CountDownLatch countDownLatch;
    private QueueModeIntegrationTestHelper helper;
    private ResponseListener responseListener;

    private final OnGetCallback doNothingOnGet = new OnGetCallback() {
        @Override
        public boolean handleGet(CoapExchange coapExchange) {
            LOG.trace("Setup client NOT to respond");
            return false;
        }
    };
    private final OnGetCallback respondOnGet = new OnGetCallback() {
        @Override
        public boolean handleGet(CoapExchange coapExchange) {
            LOG.trace("Received coapExchange: {}", coapExchange.getRequestOptions().getUriPathString());
            LOG.trace("Setup client TO respond");
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
        helper.server.getLwM2mRequestSender().removeResponseListener(responseListener);
        helper.server.stop();
    }

    @Test
    public void first_request_sent_immediately() throws Exception {
        createAndAddResponseListener(countDownLatch);
        helper.server.getLwM2mRequestSender()
                .send(helper.getClient(), TEST_REQUEST_TICKET + "1", new ReadRequest(3, 0));
        if (!countDownLatch.await(2, TimeUnit.SECONDS)) {
            fail("response from client was not received within timeout");
        }
    }

    @Test
    public void request_is_not_removed_from_queue_on_client_timeout() throws Exception {
        // client is set up to "sleep", i.e. not to respond
        QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        client.setOnGetCallback(doNothingOnGet);

        createAndAddResponseListener(countDownLatch);

        helper.server.getLwM2mRequestSender()
                .send(helper.getClient(), TEST_REQUEST_TICKET + "1", new ReadRequest(3, 0));
        // assert that queue has one request left in processing state still
        assertQueueHasMessageCount(1, 5000);
    }

    @Test
    public void subsequent_request_not_send_on_client_timeout() throws Exception {
        CountDownLatch acceptCountDownLatch = new CountDownLatch(1);

        // client is set up to do not respond, i.e. to cause request timeout
        QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        client.setOnGetCallback(doNothingOnGet);

        createAndAddResponseListener(acceptCountDownLatch);

        // send request but no response should be received
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "1",
                new ReadRequest(3, 0, 1));

        // assert that queue has one request left in processing state still
        assertQueueHasMessageCount(1, 3000);

        // as client has not yet sent any register-update or notify, sending
        // next request
        // request should only be queued and not sent
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "2",
                new ReadRequest(3, 0, 2));
        if (acceptCountDownLatch.await(3, TimeUnit.SECONDS)) {
            fail("response from client after first request timed out is unexpected");
        }

        // assert that queue has one additional new request
        List<QueuedRequest> requests = ((InMemoryMessageStore) helper.server.getMessageStore()).retrieveAll(helper
                .getClient().getEndpoint());
        assertEquals(2, requests.size());
    }

    @Test
    public void request_sent_after_client_update() throws Exception {
        final QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        CountDownLatch acceptCountDownLatch = new CountDownLatch(1);

        // client is set up not to respond the next request, but to respond
        // afterwards
        client.setOnGetCallback(new OnGetCallback() {
            @Override
            public boolean handleGet(CoapExchange coapExchange) {
                // second GET request client is set to respond back.
                client.setOnGetCallback(new OnGetCallback() {
                    @Override
                    public boolean handleGet(CoapExchange coapExchange) {
                        return true;
                    }
                });
                countDownLatch.countDown();
                return false; // client doesn't respond.
            }
        });

        createAndAddResponseListener(acceptCountDownLatch);

        // Send a read request. Will be sent immediately as it is the first
        // message in the queue.
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "1",
                new ReadRequest(3, 0, 1));

        // after first (not responded) request, coundDown should be zero.
        if (!countDownLatch.await(1, TimeUnit.SECONDS)) {
            fail("request was not properly processed");
        }

        // Needed to ensure that the request is left in Queue
        // update would not have the expected effect here.
        assertQueueHasMessageCount(1, 5000);

        // Registration engine sends a registration-update automatically
        // after(CUSTOM_LIFETIME (3) - 10%) = 2.7 seconds

        // when server has received a response for the retry, acceptCoundDown
        // should be zero.
        if (!acceptCountDownLatch.await(7, TimeUnit.SECONDS)) {
            fail("server never received the response");
        }

        List<QueuedRequest> queuedRequests = ((InMemoryMessageStore) helper.server.getMessageStore())
                .retrieveAll(helper.getClient().getEndpoint());
        assertEquals(0, queuedRequests.size());
    }

    @Test
    public void request_sent_after_client_notify() throws Exception {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationRegistry().addListener(listener);
        // stop default client as we need client with a custom life time.
        helper.client.stop(false);

        QueuedModeLeshanClient client = helper.createClient(2);
        // set client to respond to GET
        client.setOnGetCallback(respondOnGet);
        client.start();

        responseListener = new ResponseListener() {
            @Override
            public void onResponse(String clientEndpoint, String requestTicket, LwM2mResponse response) {
                if (response instanceof ObserveResponse) {
                    Observation observation = ((ObserveResponse) response).getObservation();
                    assertEquals("/3/0/15", observation.getPath().toString());
                    assertEquals(helper.getClient().getRegistrationId(), observation.getRegistrationId());
                }
            }

            @Override
            public void onError(String clientEndpoint, String requestTicket, Exception exception) {
                throw new IllegalStateException("unexpected exception occurred: ", exception);
            }
        };
        helper.server.getLwM2mRequestSender().addResponseListener(responseListener);
        // Send an Observe request. Will be sent immediately as it is the first
        // message in the queue. Response will bre processed by the
        // ResponseListener defined above.
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "1",
                new ObserveRequest(3, 0, 15));
        // Wait for read request timeout
        waitForInterval(TIMEOUT);
        // now remove the observe response listener.
        helper.server.getLwM2mRequestSender().removeResponseListener(responseListener);

        CountDownLatch acceptCountDownLatch = new CountDownLatch(1);
        // set client NOT to respond to GET
        client.setOnGetCallback(doNothingOnGet);
        // add a read response listener.
        createAndAddResponseListener(acceptCountDownLatch);
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "2",
                new ReadRequest(3, 0, 15));
        // read request should be left in Queue still.
        assertQueueHasMessageCount(1, 5000);

        // set client to respond to GET
        client.setOnGetCallback(respondOnGet);
        // write device timezone
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "3",
                new WriteRequest(3, 0, 15, "Europe/Berlin"));

        // wait for client update.
        listener.waitForNotification(3000);

        // wait for Read and Write request responses
        waitForInterval(TIMEOUT);
        assertQueueIsEmpty(3000L);
        client.stop(true);
    }

    @Test
    public void all_requests_sent_if_client_reachable() throws Exception {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationRegistry().addListener(listener);
        QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;

        responseListener = new ResponseListener() {
            @Override
            public void onResponse(String clientEndpoint, String requestTicket, LwM2mResponse response) {
                if (response instanceof ObserveResponse) {
                    LOG.trace("Received observe response for ticket {} from LWM2M client {}", requestTicket, response);
                    Observation observation = ((ObserveResponse) response).getObservation();
                    assertEquals("/3/0/15", observation.getPath().toString());
                    assertEquals(helper.getClient().getRegistrationId(), observation.getRegistrationId());
                }
            }

            @Override
            public void onError(String clientEndpoint, String requestTicket, Exception exception) {
                throw new IllegalStateException("unexpected exception occurred: ", exception);
            }
        };
        helper.server.getLwM2mRequestSender().addResponseListener(responseListener);
        // set client to respond to GET
        client.setOnGetCallback(respondOnGet);
        // Send an Observe request. Will be sent immediately as it is the first
        // message in the queue.
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "1",
                new ObserveRequest(3, 0, 15));
        waitForInterval(TIMEOUT);
        // remove observe response listener
        helper.server.getLwM2mRequestSender().removeResponseListener(responseListener);

        // write device timezone.
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "2",
                new WriteRequest(3, 0, 15, "Europe/Berlin"));

        // Read request should be sent immediately.
        final CountDownLatch acceptCountDownLatch = new CountDownLatch(1);
        // add write response listener
        createAndAddResponseListener(acceptCountDownLatch);
        responseListener = new ResponseListener() {
            @Override
            public void onResponse(String clientEndpoint, String requestTicket, LwM2mResponse response) {
                if (response instanceof WriteResponse) {
                    LOG.trace("Received write response for ticket {} from LWM2M client {}", requestTicket, response);
                    acceptCountDownLatch.countDown();
                    assertEquals(ResponseCode.CHANGED, response.getCode());
                }
            }

            @Override
            public void onError(String clientEndpoint, String requestTicket, Exception exception) {
                throw new IllegalStateException("unexpected exception occurred: ", exception);
            }
        };
        helper.server.getLwM2mRequestSender().addResponseListener(responseListener);
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "3",
                new ReadRequest(3, 0, 1));

        waitForInterval(TIMEOUT);
        assertQueueIsEmpty(5000L);

        // wait for notify
        listener.waitForNotification(2000);
        assertTrue(listener.receievedNotify().get());
    }

    @Test
    public void no_duplicate_send_on_consecutive_notifies() throws Exception {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationRegistry().addListener(listener);
        // stop default client as we need client with a custom life time.
        helper.client.stop(false);

        // create client with ~ 2 seconds lifetime
        QueuedModeLeshanClient client = helper.createClient(2);
        // set client to respond to GET
        client.setOnGetCallback(respondOnGet);
        client.start();
        waitForInterval(1000);

        final CountDownLatch acceptCountDownLatch = new CountDownLatch(2);
        responseListener = new ResponseListener() {
            @Override
            public void onResponse(String clientEndpoint, String requestTicket, LwM2mResponse response) {
                if (response instanceof ObserveResponse) {
                    acceptCountDownLatch.countDown();
                    LOG.trace("Received observe response for ticket {} from LWM2M client {}", requestTicket, response);
                    Observation observation = ((ObserveResponse) response).getObservation();
                    assertEquals("/3/0/15", observation.getPath().toString());
                    assertEquals(helper.getClient().getRegistrationId(), observation.getRegistrationId());
                }
            }

            @Override
            public void onError(String clientEndpoint, String requestTicket, Exception exception) {
                throw new IllegalStateException("unexpected exception occurred: ", exception);
            }
        };
        helper.server.getLwM2mRequestSender().addResponseListener(responseListener);
        // Send an Observe request. Will be sent immediately as it is the first
        // message in the queue.
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "1",
                new ObserveRequest(3, 0, 15));
        waitForInterval(TIMEOUT);

        // Now set the client NOT to respond to GET and send a request
        client.setOnGetCallback(doNothingOnGet);
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "2",
                new ReadRequest(3, 0, 1));

        waitForInterval(TIMEOUT);
        assertQueueHasMessageCount(1, 5000L);

        final CountDownLatch clientCountDownLatch = new CountDownLatch(2);
        client.setOnGetCallback(new OnGetCallback() {
            @Override
            public boolean handleGet(CoapExchange coapExchange) {
                LOG.trace("Received again coapExchange: {}", coapExchange.getRequestOptions().getUriPathString());
                LOG.trace("Setup client again TO respond");
                if (coapExchange.getRequestOptions().getUriPath().toString().equals("[3, 0, 1]")) {
                    clientCountDownLatch.countDown();
                }
                return true;
            }
        });

        // write device timezone
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "3",
                new WriteRequest(3, 0, 15, "Europe/Amsterdam"));
        // write device timezone
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "4",
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        assertQueueIsEmpty(3000);
        // check server received only one response to the above read request
        // duplicate send means countDown is zero
        assertTrue("SERVER: Expected only one response received (count=1) and no duplicates. CountDown reached" + "["
                + acceptCountDownLatch.getCount() + "]", acceptCountDownLatch.getCount() == 1);
        // duplicate send means countDown is zero
        assertTrue("CLIENT: Expected only one message received (count=1) and no duplicates. CountDown reached" + "["
                + clientCountDownLatch.getCount() + "]", clientCountDownLatch.getCount() == 1);
        client.stop(true);
    }

    @Test
    public void messages_removed_on_client_deregister() throws Exception {
        // client is set up to do not respond, i.e. to cause request timeout
        QueuedModeLeshanClient client = (QueuedModeLeshanClient) helper.client;
        client.setOnGetCallback(doNothingOnGet);

        // now send some read requests
        CountDownLatch acceptCountDownLatch = new CountDownLatch(1);
        createAndAddResponseListener(acceptCountDownLatch);
        // Send a read request. Will be sent immediately as it is the first
        // message in the queue.
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "1",
                new ReadRequest(3, 0, 1));
        helper.server.getLwM2mRequestSender().send(helper.getClient(), TEST_REQUEST_TICKET + "2",
                new ReadRequest(3, 0, 15));

        waitForInterval(TIMEOUT);
        assertQueueHasMessageCount(2, 5000);

        // Send de-register
        helper.client.stop(DEREGISTER);
        assertQueueIsEmpty(3000);
    }

    /**
     * @param acceptCountDownLatch
     */
    private void createAndAddResponseListener(final CountDownLatch acceptCountDownLatch) {
        responseListener = new ResponseListener() {

            @Override
            public void onResponse(String clientEndpoint, String requestTicket, LwM2mResponse response) {
                acceptCountDownLatch.countDown();
            }

            @Override
            public void onError(String clientEndpoint, String requestTicket, Exception exception) {
                throw new IllegalStateException("unexpected exception occurred: ", exception);
            }
        };
        helper.server.getLwM2mRequestSender().addResponseListener(responseListener);
    }

    private void assertQueueHasMessageCount(int count, long timeout) throws InterruptedException {
        long interval = 100;
        long duration = 0;
        int queuedRequestCount = 0;
        do {
            Thread.sleep(interval);
            duration += interval;
            InMemoryMessageStore messageStore = (InMemoryMessageStore) helper.server.getMessageStore();
            queuedRequestCount = messageStore.retrieveAll(helper.getClient().getEndpoint()).size();
        } while (queuedRequestCount < count && duration <= timeout);
        assertTrue("Expected to have at least " + count + " queued request in queue but have [" + queuedRequestCount
                + "]", queuedRequestCount == count);
    }

    private void assertQueueIsEmpty(long timeout) throws InterruptedException {
        long interval = 100;
        long duration = 0;
        boolean empty = false;
        MessageStore messageStore = helper.server.getMessageStore();
        do {
            Thread.sleep(interval);
            duration += interval;
            empty = helper.getClient() == null
                    || (helper.getClient() != null && messageStore.isEmpty(helper.getClient().getEndpoint()));
        } while (!empty && duration <= timeout);
        assertTrue("Expected an empty queue but has some messages", empty);
    }

    private boolean waitForInterval(long timeout) throws InterruptedException {
        Thread.sleep(timeout);
        return true;
    }

    private final class TestObservationListener implements ObservationRegistryListener {

        private CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean receivedNotify = new AtomicBoolean();

        @Override
        public void newValue(Observation observation, LwM2mNode mostRecentvalue,
                List<TimestampedLwM2mNode> timestampedValues) {
            receivedNotify.set(true);
            latch.countDown();
        }

        @Override
        public void cancelled(Observation observation) {
            latch.countDown();
        }

        @Override
        public void newObservation(Observation observation) {
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

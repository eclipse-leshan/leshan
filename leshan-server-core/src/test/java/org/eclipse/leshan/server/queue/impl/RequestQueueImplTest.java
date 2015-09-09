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
package org.eclipse.leshan.server.queue.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.queue.QueueReactor;
import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.RequestState;
import org.eclipse.leshan.server.queue.SequenceId;
import org.eclipse.leshan.server.queue.impl.QueueRequestFactoryImpl.QueueRequestImpl;
import org.eclipse.leshan.server.queue.reactor.QueueReactorImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestQueueImplTest {
    private static final String ENDPOINT_ID = "myEndpoint";
    @Mock
    private Client clientMock;
    @Mock
    private QueueRequestImpl queueRequestMock;
    @Mock
    private DownlinkRequest<ValueResponse> downlinkRequestMock;

    private QueueRequestFactoryImpl queueRequestFactory;
    private QueueReactor queueReactor = new QueueReactorImpl(1);

    private static final Long RESPONSE_ID = 123L;
    private final long sendExpiration = 10000L;
    private final long keepExpiration = 12000L;

    @Before
    public void before() {
        Mockito.when(clientMock.getEndpoint()).thenReturn(ENDPOINT_ID);
        queueRequestFactory = new QueueRequestFactoryImpl();
        queueReactor.start();
    }

    @Test
    public void verifySequenceIsSetAfterEnqueueRequest() throws Exception {
        QueueRequest queueRequest = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        SequenceId sequenceId = requestQueueUnderTest.enqueueRequest(queueRequest);

        assertNotEquals("SequenceID was not set", SequenceId.NONE, sequenceId);
    }

    @Test(expected = IllegalStateException.class)
    public void verifyThatIsNotPossibleToEnqueueTwice() throws Exception {
        QueueRequest queueRequest = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        requestQueueUnderTest.enqueueRequest(queueRequest);
        requestQueueUnderTest.enqueueRequest(queueRequest);
    }

    @Test
    public void verifyStateIsChangedAfterEnqueueRequest() throws Exception {
        QueueRequest queueRequest = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);
        requestQueueUnderTest.enqueueRequest(queueRequest);

        assertEquals("State was not changed", RequestState.ENQUEUED, queueRequest.getRequestState());
    }

    @Test
    public void verifyStateIsChangedAfterProcessingRequest() throws Exception {
        QueueRequest queueRequest = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);
        requestQueueUnderTest.enqueueRequest(queueRequest);
        requestQueueUnderTest.processingRequest(queueRequest);

        assertEquals("State was not changed", RequestState.PROCESSING, queueRequest.getRequestState());
    }

    @Test
    public void verifyStateIsChangedAfterDeferringRequest() throws Exception {
        QueueRequest queueRequest = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);
        requestQueueUnderTest.enqueueRequest(queueRequest);
        requestQueueUnderTest.deferRequest(queueRequest);

        assertEquals("State was not changed", RequestState.DEFERRED, queueRequest.getRequestState());
    }

    @Test
    public void verifyStateIsChangedAfterTtlElapsingRequest() throws Exception {
        QueueRequest queueRequest = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);
        requestQueueUnderTest.enqueueRequest(queueRequest);
        requestQueueUnderTest.ttlElapsedRequest(queueRequest);

        assertEquals("State was not changed", RequestState.TTL_ELAPSED, queueRequest.getRequestState());
    }

    @Test
    public void verifyStateIsChangedAfterExecutedRequest() throws Exception {
        QueueRequest queueRequest = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);
        requestQueueUnderTest.enqueueRequest(queueRequest);
        requestQueueUnderTest.executedRequest(queueRequest);

        assertEquals("State was not changed", RequestState.EXECUTED, queueRequest.getRequestState());
    }

    @Test
    public void testGetSeparatedQueuedRequests() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        // create 2 queue requests
        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest2);

        Collection<QueueRequest> requests = requestQueueUnderTest.getRequests(clientMock.getEndpoint());

        assertEquals("request queue has not the expected size", 2, requests.size());
        Iterator<QueueRequest> iterator = requests.iterator();
        assertEquals("first sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
    }

    @Test
    public void testGetQueuedRequestsFromSameSequenceInRightOrder() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        // create 3 queue requests
        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest3);
        requestQueueUnderTest.enqueueRequest(queueRequest2, sequenceId1);

        Collection<QueueRequest> requests = requestQueueUnderTest.getRequests(clientMock.getEndpoint());

        assertEquals("request queue has not the expected size", 3, requests.size());
        Iterator<QueueRequest> iterator = requests.iterator();
        assertEquals("first sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
        assertEquals("third sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
    }

    @Test
    public void moveRequestUpOverRequest() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest2);

        requestQueueUnderTest.moveUp(queueRequest2);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("first sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
    }

    @Test
    public void moveRequestUpOverSequence() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2, sequenceId1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest3);

        requestQueueUnderTest.moveUp(queueRequest3);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("first sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
        assertEquals("third sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
    }

    @Test
    public void moveSequenceUpOverRequest() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest2);
        requestQueueUnderTest.enqueueRequest(queueRequest3, sequenceId2);

        requestQueueUnderTest.moveSequenceUp(clientMock.getEndpoint(), sequenceId2);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("first sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("third sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
    }

    @Test
    public void moveSequenceUpOverSequence() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest4 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2, sequenceId1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest3);
        requestQueueUnderTest.enqueueRequest(queueRequest4, sequenceId2);

        requestQueueUnderTest.moveSequenceUp(clientMock.getEndpoint(), sequenceId2);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("first sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("third sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
        assertEquals("fourth sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
    }

    @Test
    public void moveRequestDownOverRequest() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest2);

        requestQueueUnderTest.moveDown(queueRequest1);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("first sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
    }

    @Test
    public void moveRequestDownOverSequence() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest2);
        requestQueueUnderTest.enqueueRequest(queueRequest3, sequenceId2);

        requestQueueUnderTest.moveDown(queueRequest1);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("first sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("third sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
    }

    @Test
    public void moveSequenceDownOverRequest() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2, sequenceId1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest3);

        requestQueueUnderTest.moveSequenceDown(clientMock.getEndpoint(), sequenceId1);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("first sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
        assertEquals("third sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
    }

    @Test
    public void moveSequenceDownOverSequence() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest4 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2, sequenceId1);
        SequenceId sequenceId2 = requestQueueUnderTest.enqueueRequest(queueRequest3);
        requestQueueUnderTest.enqueueRequest(queueRequest4, sequenceId2);

        requestQueueUnderTest.moveSequenceDown(clientMock.getEndpoint(), sequenceId1);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("first sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("second sequence ID did not match", sequenceId2, iterator.next().getSequenceId());
        assertEquals("third sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
        assertEquals("fourth sequence ID did not match", sequenceId1, iterator.next().getSequenceId());
    }

    @Test
    public void moveRequestTop() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2);
        SequenceId sequenceId3 = requestQueueUnderTest.enqueueRequest(queueRequest3);

        requestQueueUnderTest.moveTop(queueRequest3);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("request is not on top of the queue", sequenceId3, iterator.next().getSequenceId());
    }

    @Test
    public void moveRequestBottom() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId1 = requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2);
        requestQueueUnderTest.enqueueRequest(queueRequest3);

        requestQueueUnderTest.moveBottom(queueRequest1);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

       Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
       QueueRequest last = null;
       while(iterator.hasNext()) {
           last = iterator.next();
       }
        assertEquals("request is not on bottom of the queue", sequenceId1, last.getSequenceId());
    }

    @Test
    public void moveSequenceTop() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest4 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2);
        SequenceId sequenceId = requestQueueUnderTest.enqueueRequest(queueRequest3);
        requestQueueUnderTest.enqueueRequest(queueRequest4, sequenceId);

        requestQueueUnderTest.moveSequenceTop(clientMock.getEndpoint(), sequenceId);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        Iterator<QueueRequest> iterator = requestQueueUnderTest.getRequests(clientMock.getEndpoint()).iterator();
        assertEquals("sequence is not on top of the queue", sequenceId, iterator.next().getSequenceId());
    }

    @Test
    public void moveSequenceBottom() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest4 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        SequenceId sequenceId = requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2, sequenceId);
        requestQueueUnderTest.enqueueRequest(queueRequest3);
        requestQueueUnderTest.enqueueRequest(queueRequest4);

        requestQueueUnderTest.moveSequenceBottom(clientMock.getEndpoint(), sequenceId);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        assertEquals("sequence is not on bottom of the queue", sequenceId, requestQueueUnderTest
                .getRequests(clientMock.getEndpoint()).toArray(new QueueRequest[] {})[3].getSequenceId());
    }

    @Test
    public void dropRequest() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2);

        requestQueueUnderTest.dropRequest(queueRequest1);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        assertFalse("request was not removed", requestQueueUnderTest.getRequests(clientMock.getEndpoint()).contains(queueRequest1));
    }

    @Test
    public void dropSequence() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);

        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest2 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest3 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        QueueRequest queueRequest4 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.enqueueRequest(queueRequest2);
        SequenceId sequenceId = requestQueueUnderTest.enqueueRequest(queueRequest3);
        requestQueueUnderTest.enqueueRequest(queueRequest4, sequenceId);

        requestQueueUnderTest.dropSequence(clientMock.getEndpoint(), sequenceId);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        assertEquals("queue has a wrong size", 2, requestQueueUnderTest.getRequests(clientMock.getEndpoint()).size());
        assertFalse("first queue request is not removed",
                requestQueueUnderTest.getRequests(clientMock.getEndpoint()).contains(queueRequest3));
        assertFalse("second queue request is not removed",
                requestQueueUnderTest.getRequests(clientMock.getEndpoint()).contains(queueRequest4));
    }

    @Test
    public void unqueueRequest() throws Exception {
        RequestQueueImpl requestQueueUnderTest = new RequestQueueImpl(queueRequestFactory, queueReactor);
        QueueRequest queueRequest1 = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        requestQueueUnderTest.enqueueRequest(queueRequest1);
        requestQueueUnderTest.unqueueRequest(queueRequest1);
        queueReactor.stop(100, TimeUnit.MILLISECONDS);

        assertEquals("queue has a wrong size", 0, requestQueueUnderTest.getRequests(clientMock.getEndpoint()).size());
    }

}

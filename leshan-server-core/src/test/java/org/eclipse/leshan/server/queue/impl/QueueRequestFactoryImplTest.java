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

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.RequestState;
import org.eclipse.leshan.server.queue.SequenceId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueueRequestFactoryImplTest {
    @Mock
    private Client clientMock;
    @Mock
    private DownlinkRequest<ValueResponse> downlinkRequestMock;

    private long sendExpiration = 10000L;
    private long keepExpiration = 12000L;
    private static final long RESPONSE_ID = 123L;

    @Test
    public void testCreateRequest() throws Exception {
        QueueRequestFactoryImpl factoryUnderTest = new QueueRequestFactoryImpl();

        QueueRequest queueRequest = factoryUnderTest.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);

        assertEquals("client is not the same", clientMock, queueRequest.getClient());
        assertEquals("request is not the same", downlinkRequestMock, queueRequest.getDownlinkRequest());
        assertEquals("request state is not as expected", RequestState.UNKNOWN, queueRequest.getRequestState());
        assertEquals("sequence ID is not as expected", SequenceId.NONE, queueRequest.getSequenceId());
    }

    @Test
    public void testTransformRequest() throws Exception {
        QueueRequestFactoryImpl factoryUnderTest = new QueueRequestFactoryImpl();
        QueueRequest queueRequest = factoryUnderTest.createQueuedRequest(clientMock, downlinkRequestMock,
                sendExpiration, keepExpiration, RESPONSE_ID);
        SequenceId sequenceId = new SequenceId(12345);

        factoryUnderTest.transformRequest(queueRequest, sequenceId, RequestState.ENQUEUED);

        assertEquals("sequence ID is not as expected", queueRequest.getSequenceId(), sequenceId);
        assertEquals("request state is not as expected", queueRequest.getRequestState(), RequestState.ENQUEUED);
    }
}

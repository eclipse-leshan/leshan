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
package org.eclipse.leshan.server.queue;

import static org.junit.Assert.assertEquals;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.queue.impl.QueueRequestFactoryImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueueRequestTest {

    private static final long ONE_DAY_NANOS = 86400000000L;

    @Mock
    private Client clientMock;
    @Mock
    private DownlinkRequest<ValueResponse> downlinkRequestMock;

    @Test
    public void verifyCreatedRequestHasNoSequenceIdAssigned() throws Exception {
        QueueRequestFactory queueRequestFactory = new QueueRequestFactoryImpl();

        QueueRequest queueRequestUnderTest = queueRequestFactory.createQueuedRequest(clientMock, downlinkRequestMock,
                ONE_DAY_NANOS, ONE_DAY_NANOS, 123L);

        assertEquals("sequence ID should be NONE", queueRequestUnderTest.getSequenceId(), SequenceId.NONE);
    }
}

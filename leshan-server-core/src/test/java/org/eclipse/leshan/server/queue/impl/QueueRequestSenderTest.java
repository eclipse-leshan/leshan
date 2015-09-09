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

import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.queue.QueueReactor;
import org.eclipse.leshan.server.queue.QueueRequestFactory;
import org.eclipse.leshan.server.queue.RequestQueue;
import org.eclipse.leshan.server.queue.reactor.QueueReactorImpl;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueueRequestSenderTest {
    @Mock
    private Client clientMock;
    @Mock
    private LwM2mRequestSender requestSenderMock;
    @Mock
    private RequestQueue requestQueueMock;
    @Mock
    private QueueRequestFactory queueRequestFactoryMock;
    @Mock
    private DownlinkRequest<ValueResponse> downlinkRequestMock;
    @Mock
    private ResponseCallback<ValueResponse> responseCallbackMock;
    @Mock
    private ErrorCallback errorCallbackMock;
    @Mock
    private ClientRegistry clientRegistryMock;
    @Mock
    private ObservationRegistry observationRegistryMock;

    private QueueReactor queueReactor = new QueueReactorImpl(2);

    @Test(expected = UnsupportedOperationException.class)
    public void verifySynchronousSendThrows() throws Exception {
        QueueRequestSenderImpl requestSenderUnderTest = new QueueRequestSenderImpl(queueReactor, requestQueueMock,
                requestSenderMock, queueRequestFactoryMock, clientRegistryMock, observationRegistryMock, 2,
                TimeUnit.MINUTES, 0L);
        requestSenderUnderTest.send(clientMock, downlinkRequestMock, 0L);
    }

    @Test
    public void verifySendForClientInNonQueueModeDelegates() throws Exception {

        QueueRequestSenderImpl requestSenderUnderTest = new QueueRequestSenderImpl(queueReactor, requestQueueMock,
                requestSenderMock, queueRequestFactoryMock, clientRegistryMock, observationRegistryMock, 2,
                TimeUnit.MINUTES, 0L);
        Mockito.when(clientMock.getBindingMode()).thenReturn(BindingMode.U);

        requestSenderUnderTest.send(clientMock, downlinkRequestMock, responseCallbackMock, errorCallbackMock);

        Mockito.verify(requestSenderMock)
                .send(clientMock, downlinkRequestMock, responseCallbackMock, errorCallbackMock);
    }
}

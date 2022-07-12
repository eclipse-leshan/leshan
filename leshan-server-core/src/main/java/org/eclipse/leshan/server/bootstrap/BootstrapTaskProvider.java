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
package org.eclipse.leshan.server.bootstrap;

import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A class responsible to return tasks to do during a {@link BootstrapSession}
 * <p>
 * This class is used by {@link DefaultBootstrapSessionManager}.
 * <p>
 * {@link #getTasks(BootstrapSession, List)} must return requests to send to the client. During 1 session,
 * {@link #getTasks(BootstrapSession, List)} can be called several time. Responses received for first batch of requests
 * can be used to determine next request to send. E.g. first batch of Requests could be a
 * {@link BootstrapDiscoverRequest} or a BootstrapReadRequest and response can be used to determine which
 * {@link BootstrapWriteRequest} to Send.
 *
 * @see BootstrapConfigStoreTaskProvider
 */
public interface BootstrapTaskProvider {

    /**
     * a batch of requests to send.
     *
     */
    public class Tasks {
        /**
         * the list of request to send
         */
        public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsToSend;

        /**
         * Object (with version) supported by the client. {@link LwM2mModel} needed to encode/decode payload of request
         * will be created from this data
         */
        public Map<Integer, String> supportedObjects;

        /**
         * if true {@link BootstrapTaskProvider#getTasks(BootstrapSession, List)} will not be called again for this
         * session
         */
        public boolean last = true;
    }

    /**
     * @param previousResponses a list of {@link LwM2mResponse} received from previous executed {@link Tasks}. It can be
     *        <code>null</code> if this is the first call for this session.
     * @return next tasks to do (next requests to send), returning <code>null</code> means there is nothing to do with
     *         this client.
     */
    Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponses);
}

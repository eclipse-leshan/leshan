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
package org.eclipse.leshan.server.endpoint;

import java.net.InetSocketAddress;
import java.net.URI;

import org.eclipse.leshan.core.endpoint.LowerLayerSetting;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

public interface LwM2mEndpoint {

    Protocol getProtocol();

    URI getURI();

    InetSocketAddress getInetSocketAddress();

    <T extends LwM2mResponse> T send(PeerProfile destination, LwM2mRequest<T> request,
            LowerLayerSetting lowerLayerConfig, long timeoutInMs) throws InterruptedException;

    <T extends LwM2mResponse> void send(PeerProfile destination, LwM2mRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, LowerLayerSetting lowerLayerSetting,
            long timeoutInMs);

    void cancelRequests(String sessionID);

    void cancelObservation(Observation observation);
}

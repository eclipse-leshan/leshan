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

import java.net.URI;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.request.LowerLayerConfig;

public interface LwM2mServerEndpoint {

    Protocol getProtocol();

    URI getURI();

    <T extends LwM2mResponse> T send(ClientProfile destination, DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs) throws InterruptedException;

    <T extends LwM2mResponse> void send(ClientProfile destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, LowerLayerConfig lowerLayerConfig,
            long timeoutInMs);

    void cancelRequests(String sessionID);

    void cancelObservation(Observation observation);
}

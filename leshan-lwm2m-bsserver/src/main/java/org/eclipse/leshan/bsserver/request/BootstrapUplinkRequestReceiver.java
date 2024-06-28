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
package org.eclipse.leshan.bsserver.request;

import java.net.URI;

import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.SendableResponse;

public interface BootstrapUplinkRequestReceiver {

    <T extends LwM2mResponse> SendableResponse<T> requestReceived(LwM2mPeer sender, UplinkRequest<T> request,
            URI serverEndpointUri);

    void onError(LwM2mPeer sender, Exception exception,
            Class<? extends UplinkRequest<? extends LwM2mResponse>> requestType, URI serverEndpointUri);
}

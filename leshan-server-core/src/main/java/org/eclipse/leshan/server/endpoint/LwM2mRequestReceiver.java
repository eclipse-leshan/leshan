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

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.SendableResponse;

public interface LwM2mRequestReceiver {

    <T extends LwM2mResponse> SendableResponse<T> requestReceived(Identity identity, PeerProfile profile,
            LwM2mRequest<T> request, URI lwm2mEndpoint);

    void onError(Identity identity, PeerProfile profile, Exception e,
            Class<? extends LwM2mRequest<? extends LwM2mResponse>> requestType, URI lwm2mEndpoint);
}

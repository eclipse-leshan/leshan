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
package org.eclipse.leshan.server.request;

import java.net.URI;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.profile.ClientProfile;

public interface UplinkRequestReceiver {

    <T extends LwM2mResponse> SendableResponse<T> requestReceived(Identity senderIdentity, ClientProfile senderProfile,
            UplinkRequest<T> request, URI serverEndpointUri);

    void onError(Identity senderIdentity, ClientProfile senderProfile, Exception exception,
            Class<? extends UplinkRequest<? extends LwM2mResponse>> requestType, URI serverEndpointUri);
}

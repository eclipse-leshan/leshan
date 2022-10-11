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
package org.eclipse.leshan.client.request;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.SendableResponse;

public interface DownlinkRequestReceiver {

    <T extends LwM2mResponse> SendableResponse<T> requestReceived(ServerIdentity identity, DownlinkRequest<T> request);

    void onError(ServerIdentity identity, Exception e,
            Class<? extends LwM2mRequest<? extends LwM2mResponse>> requestType);
}

/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

public interface LwM2mBootstrapRequestSender {
    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote server.
     * 
     * @return the LWM2M response. The response can be <code>null</code> if the timeout (given parameter or CoAP
     *         timeout) expires.
     */
    <T extends LwM2mResponse> T send(BootstrapSession destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException;

    /**
     * Send a Lightweight M2M request asynchronously.
     */
    <T extends LwM2mResponse> void send(BootstrapSession destination, DownlinkRequest<T> request, long timeout,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback);

    /**
     * cancel all ongoing requests for a given bootstrap session.
     * 
     * @param session the bootstrap session for which we need to cancel requests.
     */
    void cancelOngoingRequests(BootstrapSession session);
}

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

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;

public interface BootstrapSessionListener {

    /**
     * Called when a client try to initiate a session
     */
    void sessionInitiated(BootstrapRequest request, Identity clientIdentity);

    /**
     * Called if client is not authorized to start a bootstrap session
     */
    void unAuthorized(BootstrapRequest request, Identity clientIdentity);

    /**
     * Called if client is authorized to start a bootstrap session
     */
    void authorized(BootstrapSession session);

    /**
     * Called if there is no configuration to apply for this client
     */
    void noConfig(BootstrapSession session);

    /**
     * Called when a request is sent
     */
    void sendRequest(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request);

    /**
     * Called when we receive a successful response to a request.
     *
     * @param session the bootstrap session concerned.
     * @param request The request for which we get a successful response.
     * @param response The response received.
     */
    void onResponseSuccess(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            LwM2mResponse response);

    /**
     * Called when we receive a error response to a request.
     *
     * @param session the bootstrap session concerned.
     * @param request The request for which we get a error response.
     * @param response The response received.
     *
     */
    public void onResponseError(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            LwM2mResponse response);

    /**
     * Called when a request failed to be sent.
     *
     * @param session the bootstrap session concerned.
     * @param request The request which failed to be sent.
     * @param cause The cause of the failure. Can be null.
     *
     */
    public void onRequestFailure(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            Throwable cause);

    /**
     * Called when bootstrap session finished successfully
     */
    void end(BootstrapSession session);

    /**
     * Called when bootstrap session failed
     */
    void failed(BootstrapSession session, BootstrapFailureCause cause);
}

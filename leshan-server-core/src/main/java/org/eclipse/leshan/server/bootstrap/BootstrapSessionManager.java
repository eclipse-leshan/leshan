/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * Manages life cycle of a bootstrap process.
 * <p>
 * This class is responsible to accept or refuse to start new {@link BootstrapSession}. The session also contain the
 * ContentFormat which will be used to send Bootstrap Write Request.
 * 
 * @see DefaultBootstrapSessionManager
 * @see BootstrapSession
 */
public interface BootstrapSessionManager {

    public enum BootstrapPolicy {
        /**
         * Continue bootstrap session ignoring failure.
         */
        CONTINUE,
        /**
         * Try to resend the failed request.
         */
        RETRY,
        /**
         * Retry the bootstrap session from the beginning generally.
         */
        RETRYALL,
        /**
         * Stop to try to write bootstrap config and directly send a "Bootstrap Finished" request.
         */
        SEND_FINISHED,
        /**
         * Stop the bootstrap session. The session is considered as failure.
         */
        STOP
    }

    /**
     * Starts a bootstrapping session for an endpoint. In particular, this is responsible for authorizing the endpoint
     * if applicable.
     * 
     * @param request the bootstrap request which initiates the session.
     * @param clientIdentity the {@link Identity} of the client.
     * 
     * @return a BootstrapSession, possibly authorized.
     */
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity);

    /**
     * Called when we receive a successful response to a request.
     * 
     * @param bsSession the bootstrap session concerned.
     * @param request The request for which we get a successfull response.
     */
    public void onResponseSuccess(BootstrapSession bsSession, DownlinkRequest<? extends LwM2mResponse> request);

    /**
     * Called when we receive a error response to a request.
     * 
     * @param bsSession the bootstrap session concerned.
     * @param request The request for which we get a error response.
     * @param response The response received.
     * 
     * @return a {@link BootstrapPolicy} given the way to continue the bootstrap session.
     */
    public BootstrapPolicy onResponseError(BootstrapSession bsSession, DownlinkRequest<? extends LwM2mResponse> request,
            LwM2mResponse response);

    /**
     * Called when a request failed to be sent.
     * 
     * @param bsSession the bootstrap session concerned.
     * @param request The request which failed to be sent.
     * @param cause The cause of the failure. Can be null.
     * 
     * @return a {@link BootstrapPolicy} given the way to continue the bootstrap session.
     */
    public BootstrapPolicy onRequestFailure(BootstrapSession bsSession,
            DownlinkRequest<? extends LwM2mResponse> request, Throwable cause);

    /**
     * Performs any housekeeping related to the successful ending of a Bootstrapping session.
     * 
     * @param bsSession the bootstrap session which ends successfully.
     */
    public void end(BootstrapSession bsSession);

    /**
     * Performs any housekeeping related to the failure of a Bootstrapping session.
     * 
     * @param bsSession the bootstrap session which failed.
     * @param cause why the bootstrap failed.
     */
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause);
}

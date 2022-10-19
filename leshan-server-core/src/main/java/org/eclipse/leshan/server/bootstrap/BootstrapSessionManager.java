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

import java.net.URI;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.util.Validate;

/**
 * Manages life cycle of a bootstrap process.
 * <p>
 * This class is responsible to accept or refuse to start new {@link BootstrapSession}, then to provide request to send.
 * It also decide when session should continue, finished or failed.
 *
 * @see DefaultBootstrapSessionManager
 * @see BootstrapSession
 */
public interface BootstrapSessionManager {

    /**
     * Define if the session should continue, finished or failed.
     * <p>
     * If the session continue, it also define which request must be send next.
     */
    public class BootstrapPolicy {

        private final boolean failed;
        private final BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest;

        protected BootstrapPolicy(boolean stop, BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest) {
            this.failed = stop;
            this.nextRequest = nextRequest;
        }

        /**
         * @return true if the session should continue with another request to send
         */
        public boolean shouldContinue() {
            return !failed && nextRequest != null;
        }

        /**
         * @return true if the session should stop with success
         */
        public boolean shouldFinish() {
            return !failed && nextRequest == null;
        }

        /**
         * @return true if session should stop with failure
         */
        public boolean shouldfail() {
            return failed;
        }

        public BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest() {
            return nextRequest;
        }

        public static BootstrapPolicy continueWith(BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest) {
            Validate.notNull(nextRequest);
            return new BootstrapPolicy(false, nextRequest);
        }

        public static BootstrapPolicy finished() {
            return new BootstrapPolicy(false, null);
        }

        public static BootstrapPolicy failed() {
            return new BootstrapPolicy(true, null);
        }

        @Override
        public String toString() {
            return String.format("BootstrapPolicy [failed=%s, nextRequest=%s]", failed, nextRequest);
        }
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
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity, URI endpointUsed);

    /**
     * Generally called after {@link #begin(BootstrapRequest, Identity, URI)} to know if there is something to do on
     * this device.
     *
     * @param bsSession the bootstrap session concerned.
     * @return true if there is a bootstrap requests to send for this client.
     */
    public boolean hasConfigFor(BootstrapSession bsSession);

    /**
     * Generally called after {@link #hasConfigFor(BootstrapSession)} to know the first request to send.
     *
     * @param bsSession the bootstrap session concerned.
     * @return the first request to send.
     */
    public BootstrapDownlinkRequest<? extends LwM2mResponse> getFirstRequest(BootstrapSession bsSession);

    /**
     * Called when we receive a successful response to a request.
     *
     * @param bsSession the bootstrap session concerned.
     * @param request The request for which we get a successful response.
     * @param response The response received.
     * @return a {@link BootstrapPolicy} given the way to continue the bootstrap session.
     */
    public BootstrapPolicy onResponseSuccess(BootstrapSession bsSession,
            BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response);

    /**
     * Called when we receive a error response to a request.
     *
     * @param bsSession the bootstrap session concerned.
     * @param request The request for which we get a error response.
     * @param response The response received.
     *
     * @return a {@link BootstrapPolicy} given the way to continue the bootstrap session.
     */
    public BootstrapPolicy onResponseError(BootstrapSession bsSession,
            BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response);

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
            BootstrapDownlinkRequest<? extends LwM2mResponse> request, Throwable cause);

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

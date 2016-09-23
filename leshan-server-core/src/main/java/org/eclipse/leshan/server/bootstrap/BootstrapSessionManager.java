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
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * Manages boundaries of bootstrap process.
 */
public interface BootstrapSessionManager {

    /**
     * Starts a bootstrapping session for an endpoint. In particular, this is responsible for authorizing the endpoint
     * if applicable.
     * 
     * @param endpoint
     * @param clientIdentity
     * 
     * @return a BootstrapSession, possibly authorized.
     */
    public BootstrapSession begin(String endpoint, Identity clientIdentity);

    /**
     * Performs any housekeeping related to the successful ending of a Boostraping session.
     * 
     * @param bsSession
     */
    public void end(BootstrapSession bsSession);

    /**
     * Performs any housekeeping related to the failure of a Boostraping session.
     * 
     * @param bsSession
     * @param cause why the bootstrap failed
     * @param request last request sent to the device. Can be null.
     */
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause,
            DownlinkRequest<? extends LwM2mResponse> request);

}

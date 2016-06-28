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

import org.eclipse.leshan.core.request.Identity;

/**
 * Manages boundaries of bootstrap process.
 */
public interface BootstrapSessionManager {

    /**
     * Starts a bootstrapping session for an endpoint. In particular, this is responsible for authenticating the
     * endpoint if applicable.
     * 
     * @param endpoint
     * @param clientIdentity
     * 
     * @return a BootstrapSession, possibly authenticated.
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
     */
    public void failed(BootstrapSession bsSession);

}

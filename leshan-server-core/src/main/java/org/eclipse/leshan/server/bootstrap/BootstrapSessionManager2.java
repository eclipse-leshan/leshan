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
import org.eclipse.leshan.core.request.Identity;

/**
 * A new version of {@link BootstrapSessionManager}.
 * 
 * @since 1.1
 */
public interface BootstrapSessionManager2 extends BootstrapSessionManager {

    @Override
    @Deprecated
    BootstrapSession begin(String endpoint, Identity clientIdentity);

    /**
     * Starts a bootstrapping session for an endpoint. In particular, this is responsible for authorizing the endpoint
     * if applicable.
     * 
     * @param request the bootstrap request which initiates the session.
     * @param clientIdentity the {@link Identity} of the client.
     * 
     * @return a BootstrapSession, possibly authorized.
     * 
     * @since 1.1
     */
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity);
}

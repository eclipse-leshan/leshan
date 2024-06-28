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
 *     Orange - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.bsserver.security;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.servers.security.Authorization;

public interface BootstrapAuthorizer {

    /**
     * Return an approved {@link Authorization}, if a bootstrap session can be started for this client. when a
     * declined{@link Authorization} is returned the LWM2M Bootstrap Server will stop the Bootstrap session and will
     * respond with {@link ResponseCode#BAD_REQUEST}.
     * <p>
     * Some Application Data could be attached to on approved {@link Authorization} using :
     *
     * <pre>
     * return Authorization.approved(myAppData);
     * </pre>
     *
     * @param request the request received
     * @param client the transport information about client which sent the request.
     * @return an {@link Authorization} status.
     */
    Authorization isAuthorized(BootstrapRequest request, LwM2mPeer client);
}

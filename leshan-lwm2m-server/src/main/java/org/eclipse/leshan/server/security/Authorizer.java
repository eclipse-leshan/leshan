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
package org.eclipse.leshan.server.security;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.servers.security.Authorization;

/**
 * This class is responsible to authorize up-link request from LWM2M client.
 */
public interface Authorizer {

    /**
     * Return an approved {@link Authorization}, if this request should be handled by the LWM2M Server. when a
     * declined{@link Authorization} is returned the LWM2M server will stop to handle this request and will respond with
     * a {@link ResponseCode#FORBIDDEN} or {@link ResponseCode#BAD_REQUEST}.
     * <p>
     * Some Custom Data could be attached to on approved {@link Authorization} using :
     *
     * <pre>
     * return Authorization.approved(myCustomData);
     * </pre>
     *
     * @param request the request received
     * @param registration the registration linked to the received request.<br>
     *        For register request this is the registration which will be created<br>
     *        For update request this is the registration before the update was done.
     * @param sender the {@link LwM2mPeer} which sent the request.
     * @param endpointUri the endpoint URI which receive the request.
     *
     * @return an {@link Authorization} status.
     */
    Authorization isAuthorized(UplinkRequest<?> request, Registration registration, LwM2mPeer sender,
            EndpointUri endpointUri);
}

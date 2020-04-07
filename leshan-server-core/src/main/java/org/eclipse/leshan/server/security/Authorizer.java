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
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;

/**
 * This class is responsible to authorize up-link request from LWM2M client.
 */
public interface Authorizer {

    /**
     * Return the registration if this request should be handle by the LWM2M Server. When <code>null</code> is returned
     * the LWM2M server will stop to handle this request and will respond with a {@link ResponseCode#FORBIDDEN} or
     * {@link ResponseCode#BAD_REQUEST}.
     * 
     * @param request the request received
     * @param registration the registration linked to the received request.<br>
     *        For register request this is the registration which will be created<br>
     *        For update request this is the registration before the update was done.
     * @param senderIdentity the {@link Identity} used to send the request.
     * 
     * @return the registration if this request is authorized or <code>null</code> it is not authorized.
     */
    Registration isAuthorized(UplinkRequest<?> request, Registration registration, Identity senderIdentity);
}

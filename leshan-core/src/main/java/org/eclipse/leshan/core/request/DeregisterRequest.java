/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.DeregisterResponse;

/**
 * A Lightweight M2M request for removing the registration information from the LWM2M Server.
 */
public class DeregisterRequest implements UplinkRequest<DeregisterResponse> {

    private String registrationId = null;

    /**
     * Creates a request for removing the registration information from the LWM2M Server.
     * 
     * @param registrationId the registration Id to remove
     * @exception InvalidRequestException if registrationId is empty.
     */
    public DeregisterRequest(String registrationId) throws InvalidRequestException {
        if (registrationId == null || registrationId.isEmpty())
            throw new InvalidRequestException("registrationId is mandatory");

        this.registrationId = registrationId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }
}

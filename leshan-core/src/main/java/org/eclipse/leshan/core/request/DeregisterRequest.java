/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((registrationId == null) ? 0 : registrationId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeregisterRequest other = (DeregisterRequest) obj;
        if (registrationId == null) {
            if (other.registrationId != null)
                return false;
        } else if (!registrationId.equals(other.registrationId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("DeregisterRequest [registrationId=%s]", registrationId);
    }
}

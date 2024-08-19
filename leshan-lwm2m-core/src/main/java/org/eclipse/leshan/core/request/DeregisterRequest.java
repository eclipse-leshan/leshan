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

import java.util.Objects;

import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.DeregisterResponse;

/**
 * A Lightweight M2M request for removing the registration information from the LWM2M Server.
 */
public class DeregisterRequest extends AbstractLwM2mRequest<DeregisterResponse>
        implements UplinkDeviceManagementRequest<DeregisterResponse> {

    private final String registrationId;

    /**
     * Creates a request for removing the registration information from the LWM2M Server.
     *
     * @param registrationId the registration Id to remove
     * @exception InvalidRequestException if registrationId is empty.
     */
    public DeregisterRequest(String registrationId) throws InvalidRequestException {
        this(registrationId, null);
    }

    /**
     * Creates a request for removing the registration information from the LWM2M Server.
     *
     * @param registrationId the registration Id to remove
     * @param coapRequest the underlying request
     *
     * @exception InvalidRequestException if registrationId is empty.
     */
    public DeregisterRequest(String registrationId, Object coapRequest) throws InvalidRequestException {
        super(coapRequest);
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
    public void accept(UplinkDeviceManagementRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("DeregisterRequest [registrationId=%s]", registrationId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DeregisterRequest))
            return false;
        DeregisterRequest that = (DeregisterRequest) o;
        return that.canEqual(this) && Objects.equals(registrationId, that.registrationId);
    }

    public boolean canEqual(Object o) {
        return (o instanceof DeregisterRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(registrationId);
    }
}

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

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.CancelObservationResponse;

/**
 * A Lightweight M2M request for actively cancel an observation.
 * <p>
 * At server side this will not remove the observation from the observation store, to do it you need to use
 * {@code ObservationService#cancelObservation()}
 * </p>
 */
public class CancelObservationRequest extends AbstractSimpleDownlinkRequest<CancelObservationResponse>
        implements DownlinkDeviceManagementRequest<CancelObservationResponse> {

    private final SingleObservation observation;

    /**
     * @param observation the observation to cancel actively
     */
    public CancelObservationRequest(SingleObservation observation) {
        super(observation.getPath(), null);
        if (getPath().isRoot())
            throw new InvalidRequestException("Observe request cannot target root path");
        this.observation = observation;
    }

    public Observation getObservation() {
        return observation;
    }

    public ContentFormat getContentFormat() {
        return observation.getContentFormat();
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(DownlinkDeviceManagementRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("CancelObservation [path=%s token=%s]", getPath(), observation.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CancelObservationRequest))
            return false;
        if (!super.equals(o))
            return false;
        CancelObservationRequest that = (CancelObservationRequest) o;
        return that.canEqual(this) && Objects.equals(observation, that.observation);
    }

    public boolean canEqual(Object o) {
        return (o instanceof CancelObservationRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), observation);
    }
}

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
public class CancelObservationRequest extends AbstractSimpleDownlinkRequest<CancelObservationResponse> {

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
    public final String toString() {
        return String.format("CancelObservation [path=%s token=%s]", getPath(), observation.getId());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((observation == null) ? 0 : observation.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        CancelObservationRequest other = (CancelObservationRequest) obj;
        if (observation == null) {
            if (other.observation != null)
                return false;
        } else if (!observation.equals(other.observation))
            return false;
        return true;
    }
}

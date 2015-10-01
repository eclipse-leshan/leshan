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
package org.eclipse.leshan.server.response;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.observation.Observation;

/**
 * Specialized ValueResponse to a Observe request, with the corresponding Observation.
 *
 * This can be useful to listen to updates on the specific Observation.
 */
public class ObserveResponse extends ValueResponse {

    private Observation observation;

    public ObserveResponse(ResponseCode code, Observation observation) {
        this(code, null, observation);
    }

    public ObserveResponse(ResponseCode code, LwM2mNode content, Observation observation) {
        super(code, content);
        this.observation = observation;
    }

    @Override
    public String toString() {
        return String.format("ObserveResponse [content=%s, code=%s, observation=%s]", this.getContent(), this.getCode(),
                observation);
    }

    public Observation getObservation() {
        return observation;
    }
}

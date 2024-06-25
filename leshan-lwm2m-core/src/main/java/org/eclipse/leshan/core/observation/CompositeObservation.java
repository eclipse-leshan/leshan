/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *     Michał Wadowski (Orange) - Add Cancel Composite-Observation feature.
 *******************************************************************************/
package org.eclipse.leshan.core.observation;

import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * An composite-observation of a resource provided by a LWM2M Client.
 */
public class CompositeObservation extends Observation {

    private final List<LwM2mPath> paths;
    private final ContentFormat requestContentFormat;
    private final ContentFormat responseContentFormat;

    /**
     * Instantiates an {@link CompositeObservation} for the given node paths.
     *
     * @param id token identifier of the observation
     * @param registrationId client's unique registration identifier.
     * @param paths resources paths for which the composite-observation is set.
     * @param requestContentFormat The {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     * @param responseContentFormat The {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     * @param context additional information relative to this observation.
     */
    public CompositeObservation(ObservationIdentifier id, String registrationId, List<LwM2mPath> paths,
            ContentFormat requestContentFormat, ContentFormat responseContentFormat, Map<String, String> context,
            Map<String, String> protocolData) {
        super(id, registrationId, context, protocolData);
        this.requestContentFormat = requestContentFormat;
        this.responseContentFormat = responseContentFormat;
        this.paths = paths;
    }

    /**
     * @return the {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     */
    public ContentFormat getRequestContentFormat() {
        return requestContentFormat;
    }

    /**
     * @return the {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     */
    public ContentFormat getResponseContentFormat() {
        return responseContentFormat;
    }

    /**
     * Gets the observed resources paths.
     *
     * @return the resources paths
     */
    public List<LwM2mPath> getPaths() {
        return paths;
    }

    @Override
    public String toString() {
        return String.format(
                "CompositeObservation [paths=%s, id=%s, requestContentFormat=%s, responseContentFormat=%s, registrationId=%s, context=%s]",
                paths, id, requestContentFormat, responseContentFormat, registrationId, context);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((paths == null) ? 0 : paths.hashCode());
        result = prime * result + ((requestContentFormat == null) ? 0 : requestContentFormat.hashCode());
        result = prime * result + ((responseContentFormat == null) ? 0 : responseContentFormat.hashCode());
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
        CompositeObservation other = (CompositeObservation) obj;
        if (paths == null) {
            if (other.paths != null)
                return false;
        } else if (!paths.equals(other.paths))
            return false;
        if (requestContentFormat == null) {
            if (other.requestContentFormat != null)
                return false;
        } else if (!requestContentFormat.equals(other.requestContentFormat))
            return false;
        if (responseContentFormat == null) {
            if (other.responseContentFormat != null)
                return false;
        } else if (!responseContentFormat.equals(other.responseContentFormat))
            return false;
        return true;
    }

}

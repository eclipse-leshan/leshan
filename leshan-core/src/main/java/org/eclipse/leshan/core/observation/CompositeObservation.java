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
 *******************************************************************************/
package org.eclipse.leshan.core.observation;

import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Hex;

/**
 * An composite-observation of a resource provided by a LWM2M Client.
 */
public class CompositeObservation extends Observation {

    private final List<LwM2mPath> paths;

    /**
     * Instantiates an {@link CompositeObservation} for the given node paths.
     *
     * @param id token identifier of the observation
     * @param registrationId client's unique registration identifier.
     * @param paths resources paths for which the composite-observation is set.
     * @param contentFormat contentFormat used to read the resource (could be null).
     * @param context additional information relative to this observation.
     */
    public CompositeObservation(byte[] id, String registrationId, List<LwM2mPath> paths, ContentFormat contentFormat,
            Map<String, String> context) {
        super(id, registrationId, contentFormat, context);
        this.paths = paths;
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
        return String.format("CompositeObservation [paths=%s, id=%s, contentFormat=%s, registrationId=%s, context=%s]",
                paths, Hex.encodeHexString(id), contentFormat, registrationId, context);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((paths == null) ? 0 : paths.hashCode());
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
        return true;
    }
}

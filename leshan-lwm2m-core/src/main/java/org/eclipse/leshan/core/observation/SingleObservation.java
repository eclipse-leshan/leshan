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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *     Michał Wadowski (Orange) - Add Cancel Composite-Observation feature.
 *******************************************************************************/
package org.eclipse.leshan.core.observation;

import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * An observation of a resource provided by a LWM2M Client.
 */
public class SingleObservation extends Observation {

    private final LwM2mPath path;

    protected final ContentFormat contentFormat;

    /**
     * Instantiates an {@link SingleObservation} for the given node path.
     *
     * @param id token identifier of the observation
     * @param registrationId client's unique registration identifier.
     * @param path resource path for which the observation is set.
     * @param contentFormat contentFormat used to read the resource (could be null).
     * @param context additional information relative to this observation.
     */
    public SingleObservation(ObservationIdentifier id, String registrationId, LwM2mPath path,
            ContentFormat contentFormat, Map<String, String> context, Map<String, String> protocolData) {
        super(id, registrationId, context, protocolData);
        this.path = path;
        this.contentFormat = contentFormat;
    }

    /**
     * Gets the observed resource path.
     *
     * @return the resource path
     */
    public LwM2mPath getPath() {
        return path;
    }

    /**
     * Gets the requested contentFormat (could be null).
     *
     * @return the resource path
     */
    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    @Override
    public String toString() {
        return String.format("SingleObservation [path=%s, id=%s, contentFormat=%s, registrationId=%s, context=%s]",
                path, id, contentFormat, registrationId, context);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((contentFormat == null) ? 0 : contentFormat.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
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
        SingleObservation other = (SingleObservation) obj;
        if (contentFormat == null) {
            if (other.contentFormat != null)
                return false;
        } else if (!contentFormat.equals(other.contentFormat))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }
}

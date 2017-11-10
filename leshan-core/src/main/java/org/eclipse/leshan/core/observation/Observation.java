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
package org.eclipse.leshan.core.observation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.util.Hex;

/**
 * An observation of a resource provided by a LWM2M Client.
 */
public class Observation {

    private final byte[] id;
    private final LwM2mPath path;
    private final String registrationId;
    private final Map<String, String> context;

    /**
     * Instantiates an {@link Observation} for the given node path.
     * 
     * @param id token identifier of the observation
     * @param registrationId client's unique registration identifier.
     * @param path resource path for which the observation is set.
     * @param context additional information relative to this observation.
     */
    public Observation(byte[] id, String registrationId, LwM2mPath path, Map<String, String> context) {
        this.id = id;
        this.path = path;
        this.registrationId = registrationId;
        if (context != null)
            this.context = Collections.unmodifiableMap(new HashMap<>(context));
        else
            this.context = Collections.emptyMap();
    }

    /**
     * Get the id of this observation.
     * 
     */
    public byte[] getId() {
        return id;
    }

    /**
     * Get the registration ID link to this observation.
     * 
     * @return the registration ID
     */
    public String getRegistrationId() {
        return registrationId;
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
     * @return the contextual information relative to this observation.
     */
    public Map<String, String> getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("Observation [id=%s, path=%s, registrationId=%s, context=%s]", Hex.encodeHexString(id),
                path, registrationId, context);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + Arrays.hashCode(id);
        result = prime * result + ((path == null) ? 0 : path.hashCode());
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
        Observation other = (Observation) obj;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (!Arrays.equals(id, other.id))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (registrationId == null) {
            if (other.registrationId != null)
                return false;
        } else if (!registrationId.equals(other.registrationId))
            return false;
        return true;
    }

}

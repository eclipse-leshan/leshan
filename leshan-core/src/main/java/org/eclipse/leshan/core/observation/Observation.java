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

import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * An observation of a resource provided by a LWM2M Client.
 */
public class Observation {

    private byte[] id;
    private LwM2mPath path;
    private String registrationId;
    private Map<String, String> context;

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
        this.context = context;
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

}

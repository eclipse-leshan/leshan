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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An abstract class for observation of a resource provided by a LWM2M Client.
 */
public abstract class Observation {

    protected final byte[] id;
    protected final String registrationId;
    protected final Map<String, String> context;

    /**
     * An abstract constructor for {@link Observation}.
     *
     * @param id token identifier of the observation
     * @param registrationId client's unique registration identifier.
     * @param context additional information relative to this observation.
     */
    public Observation(byte[] id, String registrationId, Map<String, String> context) {
        this.id = id;
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
     * @return the contextual information relative to this observation.
     */
    public Map<String, String> getContext() {
        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Observation)) return false;
        Observation that = (Observation) o;
        return Arrays.equals(id, that.id) &&
                Objects.equals(registrationId, that.registrationId) &&
                Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(registrationId, context);
        result = 31 * result + Arrays.hashCode(id);
        return result;
    }
}

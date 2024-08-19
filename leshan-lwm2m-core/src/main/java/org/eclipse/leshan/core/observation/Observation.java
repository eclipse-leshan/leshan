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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An abstract class for observation of a resource provided by a LWM2M Client.
 */
public abstract class Observation {

    protected final ObservationIdentifier id;
    protected final String registrationId;
    protected final Map<String, String> context;
    protected final Map<String, String> protocolData;

    /**
     * An abstract constructor for {@link Observation}.
     *
     * @param id token identifier of the observation
     * @param registrationId client's unique registration identifier.
     * @param context additional information relative to this observation.
     */
    public Observation(ObservationIdentifier id, String registrationId, Map<String, String> context,
            Map<String, String> protocolData) {
        this.id = id;
        this.registrationId = registrationId;
        if (context != null)
            this.context = Collections.unmodifiableMap(new HashMap<>(context));
        else
            this.context = Collections.emptyMap();
        if (protocolData != null)
            this.protocolData = Collections.unmodifiableMap(new HashMap<>(protocolData));
        else
            this.protocolData = Collections.emptyMap();
    }

    /**
     * Get the id of this observation.
     *
     */
    public ObservationIdentifier getId() {
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

    /**
     * @return internal data specific to LwM2mEndpointsProvider
     */
    public Map<String, String> getProtocolData() {
        return protocolData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Observation))
            return false;
        Observation that = (Observation) o;
        return that.canEqual(this) && Objects.equals(id, that.id) && Objects.equals(registrationId, that.registrationId)
                && Objects.equals(context, that.context);
    }

    public boolean canEqual(Object o) {
        return (o instanceof Observation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, registrationId, context);
    }
}

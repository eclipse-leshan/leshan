/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.registration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.observation.Observation;

/**
 * A Deregistration contains all informations which are removed after a client was unregistered.
 *
 * @see RegistrationStore
 */
public class Deregistration implements RegistrationModification {
    private final Registration registration;
    private final Collection<Observation> observations;
    private final List<Deregistration> childrenDeregistration;

    public Deregistration(Registration registration, Collection<Observation> observations) {
        this(registration, observations, null);
    }

    public Deregistration(Registration registration, Collection<Observation> observations,
            List<Deregistration> childrenDeregistration) {
        this.registration = registration;
        if (observations == null) {
            this.observations = Collections.emptyList();
        } else {
            this.observations = observations;
        }
        if (childrenDeregistration == null) {
            this.childrenDeregistration = Collections.emptyList();
        } else {
            this.childrenDeregistration = childrenDeregistration;
        }
    }

    public Registration getRegistration() {
        return registration;
    }

    public Collection<Observation> getObservations() {
        return observations;
    }

    /**
     * if {@link #getRegistration()} is a gateway then return all de-registration about its children.
     */
    public List<Deregistration> getChildrenDeRegistration() {
        return childrenDeregistration;
    }
}

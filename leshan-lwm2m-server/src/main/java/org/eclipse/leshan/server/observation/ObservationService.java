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
package org.eclipse.leshan.server.observation;

import java.util.Set;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.registration.IRegistration;

/**
 * A service keeping track observation. Can be used for finding observations and cancel them.
 */
public interface ObservationService {

    /**
     * Cancels all active observations of resource(s) implemented by a particular LWM2M registration.
     *
     * As a consequence the LWM2M Client will stop sending notifications about updated values of resources in scope of
     * the canceled observation.
     *
     * @param registration the LWM2M Client to cancel observations for
     * @return the number of canceled observations
     */
    int cancelObservations(IRegistration registration);

    /**
     * Cancels all active observations for the given resource of a given registration.
     *
     * As a consequence the LWM2M Client will stop sending notifications about updated values of resources in scope of
     * the canceled observation.
     *
     * @param registration the LWM2M Client to cancel observation for
     * @param nodePath LWM2M node path to cancel observation for
     * @return the number of canceled observations
     */
    int cancelObservations(IRegistration registration, String nodePath);

    /**
     * Cancels all active Composite-Observations for the given resources of a given registration.
     *
     * As a consequence the LWM2M Client will stop sending notifications about updated values of resources in scope of
     * the canceled observation.
     *
     * @param registration the LWM2M Client to cancel composite-observation for
     * @param nodePaths LWM2M node paths to cancel composite-observation for
     * @return the number of canceled observations
     */
    int cancelCompositeObservations(IRegistration registration, String[] nodePaths);

    /**
     * Cancels an observation.
     *
     * As a consequence the LWM2M Client will stop sending notifications about updated values of resources in scope of
     * the canceled observation.
     *
     * @param observation the observation to cancel.
     */
    void cancelObservation(Observation observation);

    /**
     * Get all running observation for a given registration
     *
     * @return an unmodifiable set of observation
     */
    Set<Observation> getObservations(IRegistration registration);

    void addListener(ObservationListener listener);

    void removeListener(ObservationListener listener);
}

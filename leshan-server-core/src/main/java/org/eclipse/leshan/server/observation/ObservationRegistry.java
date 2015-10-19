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
package org.eclipse.leshan.server.observation;

import java.util.Set;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.client.Client;

/**
 * A registry for keeping track of observed resources implemented by LWM2M Clients.
 * 
 */
public interface ObservationRegistry {

    /**
     * Adds an observation of resource(s) to the registry.
     * 
     * @param observation the observation
     */
    void addObservation(Observation observation);

    /**
     * Cancels all active observations of resource(s) implemented by a particular LWM2M Client.
     * 
     * As a consequence the LWM2M Client will stop sending notifications about updated values of resources in scope of
     * the canceled observation.
     * 
     * @param client the LWM2M Client to cancel observations for
     * @return the number of canceled observations
     */
    int cancelObservations(Client client);

    /**
     * Cancels the active observations for the given resource.
     * 
     * As a consequence the LWM2M Client will stop sending notifications about updated values of resources in scope of
     * the canceled observation.
     * 
     * @param client the LWM2M Client to cancel observation for
     * @param resourcepath resource to cancel observation for
     */
    void cancelObservation(Client client, String resourcepath);

    /**
     * Get all running observation for a given client
     * 
     * @return an unmodifiable set of observation
     */
    Set<Observation> getObservations(Client client);

    void addListener(ObservationRegistryListener listener);

    void removeListener(ObservationRegistryListener listener);
}

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
package org.eclipse.leshan.server.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationListener;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>Map</code> based registry for keeping track of this server's observed resources on LWM2M Clients.
 * 
 */
public class ObservationRegistryImpl implements ObservationRegistry, ObservationListener {

    private final Logger LOG = LoggerFactory.getLogger(ObservationRegistryImpl.class);
    private final Map<String /* registration id */, Map<LwM2mPath /* resource path */, Observation>> observationsByClientAndResource;

    private final List<ObservationRegistryListener> listeners = new CopyOnWriteArrayList<>();

    public ObservationRegistryImpl() {
        observationsByClientAndResource = new ConcurrentHashMap<String, Map<LwM2mPath, Observation>>();
    }

    @Override
    public synchronized void addObservation(Observation observation) {

        if (observation != null) {
            String registrationID = observation.getRegistrationId();

            Map<LwM2mPath, Observation> clientObservations = observationsByClientAndResource.get(registrationID);
            if (clientObservations == null) {
                clientObservations = new ConcurrentHashMap<LwM2mPath, Observation>();
                observationsByClientAndResource.put(registrationID, clientObservations);
            }

            Observation oldObservation = clientObservations.get(observation.getPath());
            if (oldObservation != null) {
                oldObservation.cancel();
            }
            clientObservations.put(observation.getPath(), observation);
            for (ObservationRegistryListener listener : listeners) {
                listener.newObservation(observation);
            }
            observation.addListener(this);
        }
    }

    @Override
    public synchronized int cancelObservations(Client client) {
        int count = 0;
        if (client != null) {
            Map<LwM2mPath, Observation> clientObservations = observationsByClientAndResource.get(client
                    .getRegistrationId());

            if (clientObservations != null) {
                count = clientObservations.size();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Canceling {} observations of client {}", count, client.getEndpoint());
                }
                for (Observation obs : clientObservations.values()) {
                    obs.cancel();
                }
                clientObservations.clear();
                observationsByClientAndResource.remove(client.getRegistrationId());
            }
        }
        return count;
    }

    @Override
    public synchronized void cancelObservation(Client client, String resourcepath) {
        if (client != null && resourcepath != null) {
            Map<LwM2mPath, Observation> clientObservations = observationsByClientAndResource.get(client
                    .getRegistrationId());

            if (clientObservations != null) {
                LwM2mPath lwM2mResourcePath = new LwM2mPath(resourcepath);
                Observation observation = clientObservations.get(lwM2mResourcePath);
                if (observation != null) {
                    // observationsByClientAndResource will be cleaned in ObservationRegistryImpl.cancelled()
                    observation.cancel();
                }
            }
        }
    }

    @Override
    public Set<Observation> getObservations(Client client) {
        Map<LwM2mPath, Observation> observations = observationsByClientAndResource.get(client.getRegistrationId());
        if (observations == null)
            return Collections.emptySet();
        else
            return Collections.unmodifiableSet(new HashSet<Observation>(observations.values()));
    }

    @Override
    public void addListener(ObservationRegistryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ObservationRegistryListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void cancelled(Observation observation) {
        // fire cancelled event
        for (ObservationRegistryListener listener : listeners) {
            listener.cancelled(observation);
        }

        synchronized (this) {
            // clear the observationsByClientAndResource map
            Map<LwM2mPath, Observation> observations = observationsByClientAndResource.get(observation
                    .getRegistrationId());
            if (observations != null) {
                LwM2mPath path = observation.getPath();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Canceling {} observation of registration {}", path, observation.getRegistrationId());
                }
                observations.remove(path);
                if (observations.isEmpty()) {
                    observationsByClientAndResource.remove(observation.getRegistrationId());
                }
            }
        }
    }

    @Override
    public void newValue(Observation observation, LwM2mNode value) {
        for (ObservationRegistryListener listener : listeners) {
            listener.newValue(observation, value);
        }
    }
}

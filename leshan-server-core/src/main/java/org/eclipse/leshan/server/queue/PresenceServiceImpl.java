/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *     Bosch Software Innovations GmbH - initial API
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.server.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the status of each LWM2M client registered with Queue mode binding. Also ensures that the
 * {@link PresenceListener} are notified on state changes only for those LWM2M clients registered using Queue mode
 * binding.
 *      
 * @see Presence
 */
public final class PresenceServiceImpl implements PresenceService {
    private static final Logger LOG = LoggerFactory.getLogger(PresenceServiceImpl.class);
    private final ConcurrentMap<String, Presence> clientStatus = new ConcurrentHashMap<>();
    private final List<PresenceListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(PresenceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PresenceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isOnline(Registration registration) {
        return Presence.ONLINE.equals(clientStatus.get(registration.getEndpoint()));
    }

    /**
     * Sets the client identified by the given registration in {@link Presence#OFFLINE} mode if the current state is
     * {@link Presence#ONLINE}. If the state is changed successfully, then the corresponding
     * {@link PresenceListener} are notified.
     * 
     * @param registration lwm2m client registration data
     */
    public void setOffline(Registration registration) {
        if (registration.usesQueueMode()
                && transitState(registration.getEndpoint(), Presence.ONLINE, Presence.OFFLINE)) {
            for (PresenceListener listener : listeners) {
                listener.onOffline(registration);
            }
        }

    }

    /**
     * Sets the client identified by the given registration in {@link Presence.ONLINE} mode if the current state is
     * either {@link Presence#OFFLINE} or <code>null</code>. If the state is changed successfully, then the
     * corresponding {@link PresenceListener} are notified.
     * 
     * @param registration lwm2m client registration data
     */
    public void setOnline(Registration registration) {
        if (registration.usesQueueMode()
                && transitState(registration.getEndpoint(), Presence.OFFLINE, Presence.ONLINE)) {
            for (PresenceListener listener : listeners) {
                listener.onOnline(registration);
            }
        }
    }

    private boolean transitState(String endpoint, Presence from, Presence to) {
        boolean updated = clientStatus.replace(endpoint, from, to);
        if (updated) {
            LOG.debug("Client {} state update from {} -> {}", endpoint, from, to);
        } else if (clientStatus.putIfAbsent(endpoint, to) == null) {
            LOG.debug("Client {} state update to -> {}", endpoint, to);
            updated = true;
        } else {
            LOG.trace("Cannot update Client {} state {} -> {}. Current state is {}", endpoint, from, to,
                    clientStatus.get(endpoint));
        }
        return updated;
    }
}

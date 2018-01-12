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
 *     RISE SICS AB - added more features 
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.server.registration.Registration;

/**
 * Tracks the status of each LWM2M client registered with Queue mode binding. Also ensures that the
 * {@link PresenceListener} are notified on state changes only for those LWM2M clients registered using Queue mode
 * binding.
 * 
 * @see Presence
 */
public final class PresenceServiceImpl implements PresenceService {
    private final ConcurrentMap<String, LwM2mQueue> clientStatusList = new ConcurrentHashMap<>();
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
    public boolean isClientSleeping(Registration registration) {
        LwM2mQueue queue = clientStatusList.get(registration.getEndpoint());
        return queue.isClientSleeping();
    }

    @Override
    public void setAwake(Registration registration) {
        if (registration.usesQueueMode()) {
            clientStatusList.get(registration.getEndpoint()).setAwake();
            for (PresenceListener listener : listeners) {
                listener.onAwake(registration);
            }
        }
    }

    @Override
    public void notifySleeping(Registration registration) {
        if (registration.usesQueueMode()) {
            for (PresenceListener listener : listeners) {
                listener.onSleeping(registration);
            }
        }

    }

    @Override
    public void createQueueObject(Registration reg) {
        clientStatusList.put(reg.getEndpoint(), new LwM2mQueue(reg, this));
    }

    /**
     * Returns the {@link LwM2mQueue} object associated with the given endpoint name.
     * 
     * @param reg The client's registration object.
     * @return The {@link LwM2mQueue} object.
     */
    public LwM2mQueue getQueueObject(Registration reg) {
        return clientStatusList.get(reg.getEndpoint());
    }
}

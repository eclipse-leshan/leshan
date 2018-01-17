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
import java.util.Timer;
import java.util.TimerTask;
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
    private final ConcurrentMap<String, PresenceStatus> clientStatusList = new ConcurrentHashMap<>();
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
        PresenceStatus presenceStatus = clientStatusList.get(registration.getEndpoint());
        return presenceStatus.isClientSleeping();
    }

    /**
     * Set the state of the client identified by registration as {@link Presence#AWAKE}
     * 
     * @param registration the client's registration object
     */
    public void setAwake(Registration reg) {
        if (reg.usesQueueMode()) {
            clientStatusList.get(reg.getEndpoint()).setAwake();
            startClientAwakeTimer(reg);
            for (PresenceListener listener : listeners) {
                listener.onAwake(reg);
            }
        }
    }

    /**
     * Notify the listeners that the client state changed to {@link Presence#SLEEPING}. The state changes is produced
     * inside {@link PresenceStatus} when the timer expires or when the client doesn't respond to a request.
     * 
     * @param registration the client's registration object
     */
    public void setSleeping(Registration reg) {
        if (reg.usesQueueMode()) {
            clientStatusList.get(reg.getEndpoint()).setSleeping();
            stopClientAwakeTimer(reg);
            for (PresenceListener listener : listeners) {
                listener.onSleeping(reg);
            }
        }
    }

    /**
     * Creates a new {@link PresenceStatus} object associated with the client.
     * 
     * @param registration the client's registration object.
     */
    public void createPresenceStatusObject(Registration reg) {
        clientStatusList.put(reg.getEndpoint(), new PresenceStatus());
    }

    /**
     * Creates a new {@link PresenceStatus} object associated with the client, with a specific awake time.
     * 
     * @param registration the client's registration object.
     */
    public void createPresenceStatusObject(Registration reg, int clientAwakeTime) {
        clientStatusList.put(reg.getEndpoint(), new PresenceStatus(clientAwakeTime));
    }

    /**
     * Returns the {@link PresenceStatus} object associated with the given endpoint name.
     * 
     * @param reg The client's registration object.
     * @return The {@link PresenceStatus} object.
     */
    public PresenceStatus getPresenceStatusObject(Registration reg) {

        return clientStatusList.get(reg.getEndpoint());
    }

    /**
     * Start or restart (if already started) the timer that handles the client wait before sleep time.
     */
    public void startClientAwakeTimer(final Registration reg) {

        final PresenceStatus clientPresenceStatus = getPresenceStatusObject(reg);
        int clientAwakeTime = clientPresenceStatus.getClientAwakeTime();
        Timer clientAwakeTimer = clientPresenceStatus.getClientTimer();

        if (clientAwakeTime != 0) {
            if (clientAwakeTimer != null) {
                clientAwakeTimer.cancel();
                clientAwakeTimer.purge();
            }
            clientAwakeTimer = new Timer();
            clientPresenceStatus.setClientTimer(clientAwakeTimer);
            TimerTask clientAwakeTask = new TimerTask() {

                @Override
                public void run() {
                    if (!isClientSleeping(reg)) {
                        setSleeping(reg);
                    }
                }
            };
            clientAwakeTimer.schedule(clientAwakeTask, clientAwakeTime);
        }

    }

    /**
     * Stop the timer that handles the client wait before sleep time.
     */
    private void stopClientAwakeTimer(Registration reg) {
        PresenceStatus clientPresenceStatus = getPresenceStatusObject(reg);
        Timer clientAwakeTimer = clientPresenceStatus.getClientTimer();
        if (clientAwakeTimer != null) {
            clientAwakeTimer.cancel();
            clientAwakeTimer.purge();
        }
    }

    /**
     * Called when the client doesn't respond to a request, for changing its state to SLEEPING
     */
    public void clientNotResponding(Registration reg) {
        if (!isClientSleeping(reg)) {
            setSleeping(reg);
        }
    }
}

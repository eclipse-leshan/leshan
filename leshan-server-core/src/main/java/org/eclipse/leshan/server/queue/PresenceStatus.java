/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.Timer;

/**
 * Class that contains all the necessary elements to handle the queue mode. Every registration object that uses Queue
 * Mode has a PresenceStatus object linked to it for handling this mode.
 */

public class PresenceStatus {

    /* The state of the client: Awake or Sleeping */
    private Presence state;

    /* Elements to handle the time the client waits before going to sleep */
    private Timer clientAwakeTimer;

    private int clientAwakeTime;

    public PresenceStatus() {
        this.state = Presence.SLEEPING;
        this.clientAwakeTime = 93000; /* ms, default CoAP value */
    }

    public PresenceStatus(int clientAwakeTime) {
        this.state = Presence.SLEEPING;
        this.clientAwakeTime = clientAwakeTime; /* ms */
    }

    /* Client State Control */

    /**
     * Set the client state to awake. This should be called when an update message is received from the server. It
     * starts the client awake timer.
     */
    public void setAwake() {
        if (state == Presence.SLEEPING) {
            state = Presence.AWAKE;
        }
    }

    /**
     * Set the client state to sleeping. This should be called when the the time the client waits before going to sleep
     * expires, or when the client is not responding. It also notifies the listeners inside the {@link PresenceService}.
     * It stops the client awake timer.
     */
    public void setSleeping() {
        if (state == Presence.AWAKE) {
            state = Presence.SLEEPING;
        }
    }

    /**
     * Tells if the client is sleeping or not
     * 
     * @return true if client is sleeping
     */
    public boolean isClientSleeping() {
        return state == Presence.SLEEPING;
    }

    /* Control of the time the client waits before going to sleep */
    /**
     * Get the time that the client stays awake after an update message or the last received request.
     * 
     * @return The client awake time.
     */
    public int getClientAwakeTime() {
        return clientAwakeTime;
    }

    /**
     * Sets the client awake time, in case it wants to be modified during run time.
     * 
     * @param clientAwakeTime
     */
    public void setClientAwakeTime(int clientAwakeTime) {
        this.clientAwakeTime = clientAwakeTime;
    }

    public Timer getClientTimer() {
        return this.clientAwakeTimer;
    }

    public void setClientTimer(Timer clientAwakeTimer) {
        this.clientAwakeTimer = clientAwakeTimer;
    }

}

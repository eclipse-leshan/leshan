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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.util.Validate;

/**
 * Class that contains all the necessary elements to handle the queue mode. Every registration object that uses Queue
 * Mode has a LwM2mQueue object for handling it.
 */

public class LwM2mQueue {

    /* The state of the client: Awake or Sleeping */
    private Presence state;

    /* Elements to handle the time the client waits before going to sleep */
    private Timer clientAwakeTimer;

    private TimerTask clientAwakeTask;

    private int clientAwakeTime;

    /* Registration and Presence Service for notifying sleeping */
    private Registration registration;

    private PresenceService presenceService;

    public LwM2mQueue(Registration registration, PresenceService presenceService) {
        Validate.notNull(registration);
        Validate.notNull(presenceService);

        this.registration = registration;
        this.presenceService = presenceService;
        this.state = Presence.SLEEPING;
        this.clientAwakeTime = readCoapMaxTransmitWait(); /* ms */
    }

    public LwM2mQueue(Registration registration, PresenceService presenceService, int clientAwakeTime) {
        Validate.notNull(registration);
        Validate.notNull(presenceService);

        this.registration = registration;
        this.presenceService = presenceService;
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
        startClientAwakeTimer();
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
        stopClientAwakeTimer();
        presenceService.notifySleeping(registration);
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

    /**
     * Start or restart (if already started) the timer that handles the client wait before sleep time.
     */
    public void startClientAwakeTimer() {

        if (clientAwakeTime != 0) {
            if (clientAwakeTimer != null) {
                clientAwakeTimer.cancel();
                clientAwakeTimer.purge();
            }
            clientAwakeTimer = new Timer();
            clientAwakeTask = new TimerTask() {

                @Override
                public void run() {
                    if (!isClientSleeping()) {
                        setSleeping();
                    }
                }
            };
            clientAwakeTimer.schedule(clientAwakeTask, clientAwakeTime);
        }

    }

    /**
     * Stop the timer that handles the client wait before sleep time.
     */
    private void stopClientAwakeTimer() {

        if (clientAwakeTimer != null) {
            clientAwakeTimer.cancel();
            clientAwakeTimer.purge();
        }

    }

    /**
     * Reads from the Californium.properties file the MAX_TRANSMIT_WAIT constant of the CoAP Protocol
     * 
     * @return MAX_TRANSMIT_WAIT
     */
    public int readCoapMaxTransmitWait() {

        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream("Californium.properties");
            prop.load(input);

            return Integer.valueOf(prop.getProperty("MAX_TRANSMIT_WAIT"));
        } catch (IOException ex) {
            return 93000; /* Default Value */
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Called when the client doesn't respond to a request, for changing its state to SLEEPING
     */
    public void clientNotResponding() {
        if (!isClientSleeping()) {
            setSleeping();
        }
    }

}

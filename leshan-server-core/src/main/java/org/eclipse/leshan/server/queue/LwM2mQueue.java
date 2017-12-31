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
 *     Carlos Gonzalo Peces - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that contains all the necessary elements to handle the queue mode. Every registration object that uses Queue
 * Mode has a LwM2mQueue object for handling it.
 */

public class LwM2mQueue {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mQueue.class);

    /* The state of the client: Awake or Sleeping */
    private ClientState state;

    /* Elements to handle the time the client waits before going to sleep */
    private Timer clientAwakeTimer;

    private TimerTask clientAwakeTask;

    private int clientAwakeTime;

    private Semaphore timerSemaphore;

    /* Elements for notifying the listeners */
    private Registration registration;

    private PresenceService presenceServ;

    public LwM2mQueue(Registration registration) {
        Validate.notNull(registration);

        this.registration = registration;
        this.state = ClientState.SLEEPING;
        timerSemaphore = new Semaphore(1);
        this.clientAwakeTime = readCoapMaxTransmitWait(); /* ms */
    }

    public LwM2mQueue(Registration registration, int clientAwakeTime) {
        Validate.notNull(registration);

        this.registration = registration;
        this.state = ClientState.SLEEPING;
        timerSemaphore = new Semaphore(1);
        this.clientAwakeTime = clientAwakeTime; /* ms */
    }

    /* Client State Control */

    /**
     * Set the client state to awake. This should be called when an update message is received from the server.
     */
    public void setAwake() {
        if (state == ClientState.SLEEPING) {
            state = ClientState.AWAKE;
        }
        presenceServ.notifyAwake(registration);
    }

    /**
     * Set the client state to sleeping. This should be called when the the time the client waits before going to sleep
     * expires, or when the client is not responding
     */
    public void setSleeping() {
        if (state == ClientState.AWAKE) {
            state = ClientState.SLEEPING;
            presenceServ.notifySleeping(registration);
        }
    }

    /**
     * Tells if the client is sleeping or not
     * 
     * @return true if client is sleeping
     */
    public boolean isClientSleeping() {
        return state == ClientState.SLEEPING;
    }

    /* Getter and setter for the Queue Mode Service */

    public PresenceService getPresenceServ() {
        return presenceServ;
    }

    public void setPresenceServ(PresenceService presenceServ) {
        this.presenceServ = presenceServ;
    }

    /* Control of the time the client waits before going to sleep */

    public int getClientAwakeTime() {
        return clientAwakeTime;
    }

    public void setClientAwakeTime(int clientAwakeTime) {
        this.clientAwakeTime = clientAwakeTime;
    }

    /**
     * Start or restart (if already started) the timer that handles the client wait before sleep time.
     */
    public void startClientAwakeTimer() {
        try {
            timerSemaphore.acquire();
            try {
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
            } finally {
                timerSemaphore.release();
            }
        } catch (InterruptedException e) {
            LOG.warn("Unable to restart the client timer due to semaphore exception", e);
        }
    }

    /**
     * Stop the timer that handles the client wait before sleep time.
     */
    private void stopClientAwakeTimer() {
        try {
            try {
                timerSemaphore.acquire();

                if (clientAwakeTimer != null) {
                    clientAwakeTimer.cancel();
                    clientAwakeTimer.purge();
                }
            } finally {
                timerSemaphore.release();
            }
        } catch (InterruptedException e) {
            LOG.warn("Unable to stop the client timer due to semaphore exception", e);
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
     * Called when the client not responses for changing its state to SLEEPING
     */
    public void clientNotResponding() {
        if (!isClientSleeping()) {
            setSleeping();
            stopClientAwakeTimer();
        }
    }

}

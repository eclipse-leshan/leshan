/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions;

import java.util.Random;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message delayer extensions. Delays processing of messages to simulate processing under stress condition. Used for
 * robustness test of clients. The extension toggles between delayed and undelayed processing to see, if the client is
 * able to work under "normal" condition when it was "stressed before.
 */
public class MessageDelayerExtension implements MessageInterceptor, LeshanServerExtension {
    /**
     * Configuration key for maximum number of undelayed processed messages.
     */
    public static final String CONFIG_MAX_UNDELAYED = "MAX_UNDELAYED_MESSAGES";
    /**
     * Configuration key for maximum number of delayed processed messages.
     */
    public static final String CONFIG_MAX_DELAYED = "MAX_DELAYED_MESSAGES";
    /**
     * Configuration key for maximum delay in milliseconds.
     */
    public static final String CONFIG_MAX_DELAY_IN_MS = "MAX_DELAY_IN_MS";

    private static final Logger LOG = LoggerFactory.getLogger(MessageDelayerExtension.class);

    /**
     * Default value for maximum number of undelayed processed messages.
     */
    static private final int DEF_MAX_UNDELAYED_MESSAGES = 2000;
    /**
     * Default value for maximum number of delayed processed messages.
     */
    static private final int DEF_MAX_DELAYED_MESSAGES = 4000;
    /**
     * Default value for maximum delay in milliseconds.
     */
    static private final int DEF_MAX_DELAY_IN_MS = 4000;

    private final Random rand = new Random();
    private volatile boolean enabled;

    /**
     * Maximum number of undelayed processed messages.
     */
    private int maxUndelayedMessage = DEF_MAX_UNDELAYED_MESSAGES;
    /**
     * Maximum number of delayed processed messages.
     */
    private int maxDelayedMessage = DEF_MAX_DELAYED_MESSAGES;
    /**
     * Maximum delay in milliseconds.
     */
    private int maxDelayInMs = DEF_MAX_DELAY_IN_MS;

    /**
     * Number of undelayed messages for this run.
     */
    private int start;
    /**
     * Number of messages to stop delayed processing for this run.
     */
    private int stop;
    /**
     * Number of messages in this run.
     */
    private int counter;

    public MessageDelayerExtension() {
    }

    @Override
    public void setup(LeshanServer lwServer, ExtensionConfig configuration, LeshanServerExtensionsManager manager) {
        int maxUndelayedMessage = configuration.get(CONFIG_MAX_UNDELAYED, DEF_MAX_UNDELAYED_MESSAGES);
        int maxDelayedMessage = configuration.get(CONFIG_MAX_DELAYED, DEF_MAX_DELAYED_MESSAGES);
        int maxDelayInMs = configuration.get(CONFIG_MAX_DELAY_IN_MS, DEF_MAX_DELAY_IN_MS);
        synchronized (this) {
            this.maxUndelayedMessage = maxUndelayedMessage;
            this.maxDelayedMessage = maxDelayedMessage;
            this.maxDelayInMs = maxDelayInMs;
            this.start = 0;
            this.stop = rand.nextInt(maxDelayedMessage);
            this.counter = 1;
        }
        for (Endpoint endpoint : lwServer.getCoapServer().getEndpoints()) {
            endpoint.addInterceptor(this);
        }
        LOG.info("delayInMs message processing " + maxDelayInMs + "[ms] [" + maxUndelayedMessage + " - "
                + maxDelayedMessage + "]");
    }

    @Override
    public void start() {
        enabled = true;
        LOG.info("Extension message delayer enabled");
    }

    @Override
    public void stop() {
        enabled = false;
        LOG.info("Extension message delayer disabled");
    }

    @Override
    public void sendRequest(Request request) {
    }

    @Override
    public void sendResponse(Response response) {
    }

    @Override
    public void sendEmptyMessage(EmptyMessage message) {
    }

    @Override
    public void receiveRequest(Request request) {
        randomDelay();
    }

    @Override
    public void receiveResponse(Response response) {
        randomDelay();
    }

    @Override
    public void receiveEmptyMessage(EmptyMessage message) {
        randomDelay();
    }

    /**
     * Randomly delay message processing. Delay message processing randomly, if {@link #enabled} and {@link #counter} is
     * between {@link #start} and {@link #stop}. Restarts, if {@link #counter} reaches {@link #stop} with new random
     * values for {@link #start} and {@link #stop} according the maximum values
     */
    private void randomDelay() {
        if (!enabled)
            return;

        int delay = 0;
        int counter;
        int start;
        int stop;
        synchronized (this) {
            if (this.stop <= this.counter) {
                this.counter = 0;
            }
            if (0 == this.counter) {
                this.start = rand.nextInt(maxUndelayedMessage);
                this.stop = this.start + rand.nextInt(maxDelayedMessage);
            }
            counter = ++this.counter;
            start = this.start;
            stop = this.stop;
            if (start < counter) {
                delay = rand.nextInt(maxDelayInMs);
            }
        }

        if (start < counter) {
            if (0 < delay) {
                try {
                    LOG.info("delayInMs message processing " + delay + "ms " + counter + " to [" + start + " - " + stop
                            + "]");
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                }
            } else {
                LOG.info("undelayed message processing " + counter + " to [" + start + " - " + stop + "]");
            }
        } else {
            LOG.info("direct message processing " + counter + " to [" + start + " - " + stop + "]");
        }
    }
}

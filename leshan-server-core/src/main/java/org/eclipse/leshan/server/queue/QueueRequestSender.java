/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein (Bosch Software Innovations GmbH)
 *                     - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.server.request.LwM2mRequestSender;

/**
 * A queue request sender is a special sender which is aware of LwM2M client's binding mode. If a client supports "Q"
 * (queue) mode, the sender queues the request automatically and sends it when the client is back online.
 */
public interface QueueRequestSender extends LwM2mRequestSender {

    /**
     * Sets the time period in which the request being queued will be expired and will not be sent anymore.
     *
     * @param expirationInterval interval amount
     * @param expirationIntervalTimeUnit interval time unit
     */
    void setSendExpirationInterval(long sendExpirationInterval, TimeUnit sendExpirationIntervalTimeUnit);

    /**
     * Sets the time period in which the request being queued will not be kept in queue anymore.
     *
     * @param keepExpirationInterval interval amount
     * @param keepExpirationIntervalTimeUnit interval time unit
     */
    void setKeepExpirationInterval(long keepExpirationInterval, TimeUnit keepExpirationIntervalTimeUnit);
}
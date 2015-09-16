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

/**
 * This queue reactor allows concurrent processing of the requests without changing the order of execution. Because of a
 * highly concurrent nature of the Queue Mode (responses from clients may arrive any time, changing the state of the
 * pending requests, as well as the user can do via queue management interface), this reactor serves as a scheduler for
 * queue tasks, which can run in the same thread of execution (non-blocking tasks) or in a separate worker thread
 * (blocking tasks).
 *
 * @see QueueTask
 */
public interface QueueReactor {

    /**
     * Schedules a queue task to the reactor.
     *
     * @param task task to be scheduled.
     */
    void addTask(QueueTask task);

    /**
     * Starts the reactor's processing.
     */
    void start();

    /**
     * Stops the reactor's processing.
     *
     * @param timeout time to wait for shutdown
     * @param timeUnit time unit to use
     */
    void stop(long timeout, TimeUnit timeUnit);
}

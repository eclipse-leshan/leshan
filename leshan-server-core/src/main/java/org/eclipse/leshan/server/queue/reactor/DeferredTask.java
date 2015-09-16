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
package org.eclipse.leshan.server.queue.reactor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.server.queue.QueueTask;

/**
 * A deferred task does not execute immediately, but schedules the task to be executed after the given delay.
 */
public class DeferredTask implements QueueTask {

    private static final ScheduledExecutorService DEFERRED_TASK_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private long period;
    private TimeUnit unit;
    private QueueTask task;

    /**
     * Creates a task, which execution is deferred after the given delay.
     *
     * @param period delay to wait until execution
     * @param unit time unit of delay
     * @param task task to be executed
     */
    public DeferredTask(long period, TimeUnit unit, QueueTask task) {
        this.period = period;
        this.unit = unit;
        this.task = task;
    }

    @Override
    public void run() {
        DEFERRED_TASK_EXECUTOR.schedule(task, period, unit);
    }

    @Override
    public boolean wouldBlock() {
        return false;
    }

}

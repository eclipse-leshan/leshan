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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.server.queue.QueueTask;
import org.eclipse.leshan.server.queue.QueueReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This simple reactor implementation allows concurrent processing of the requests without changing the order of
 * execution. Because of a highly concurrent nature of the Queue Mode (responses from clients may arrive anytime,
 * changing the state of the pending requests, as well as the queue management can do at the same time), this reactor
 * serves as a scheduler for queue tasks, which can run in the same thread of execution (non-blocking tasks) or in a
 * separate worker thread (blocking tasks).
 *
 * @see QueueTask
 */
public class QueueReactorImpl implements QueueReactor {

    private static final Logger LOG = LoggerFactory.getLogger(QueueReactorImpl.class);

    private final BlockingQueue<QueueTask> commands = new LinkedBlockingDeque<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ExecutorService workerExecutorService;

    /**
     * Creates a new queue reactor with a given size of pool workers.
     *
     * @param workerPoolSize size of pool workers. This size determines how many of the <i>blocking</i> tasks can be run
     *        in parallel. For instance, a RequestSendingTask is such a blocking task, because it blocks for ACK_TIMEOUT
     *        time.
     */
    public QueueReactorImpl(int workerPoolSize) {
        int workers = workerPoolSize <= 0 ? Runtime.getRuntime().availableProcessors() : workerPoolSize;
        workerExecutorService = Executors.newFixedThreadPool(workers);
    }

    @Override
    public void addTask(QueueTask task) {
        try {
            commands.put(task);
        } catch (InterruptedException e) {
            LOG.warn("queue task could not be added");
        }
    }

    @Override
    public void start() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        QueueTask task = commands.take();
                        if (task.wouldBlock()) {
                            workerExecutorService.submit(task);
                        } else {
                            task.run();
                        }
                    }
                } catch (InterruptedException e) {
                    workerExecutorService.shutdownNow();
                    LOG.info("queue reactor processing was interrupted");
                }
            }
        });
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        executorService.shutdown();
        try {
            boolean termination = executorService.awaitTermination(timeout, timeUnit);
            if (!termination) {
                LOG.warn("Queue reactor could not be terminated within expected timeout");
            }
        } catch (InterruptedException e) {
            LOG.warn("queue reactor was terminated while shutting down");
        }
    }

    /**
     * Stops using default grace period of 5 seconds.
     */
    public void stop() {
        stop(5, TimeUnit.SECONDS);
    }
}

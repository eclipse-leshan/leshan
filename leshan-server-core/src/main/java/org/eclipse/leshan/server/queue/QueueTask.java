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

/**
 * A queue task represents an piece of an execution run in the queue reactor. This execution which can be done either
 * within the same reactor thread (for a short-timed operation), or a task which would block the queue reactor - in this
 * case, the queue task should signal that it {@link #wouldBlock()} the reactor main thread and it will be executed in a
 * separate worker thread.
 *
 * @see QueueReactor
 */
public interface QueueTask extends Runnable {

    /**
     * @return true, if the execution of this task would block the execution of the subsequently queued tasks, otherwise
     *         false.
     */
    boolean wouldBlock();
}

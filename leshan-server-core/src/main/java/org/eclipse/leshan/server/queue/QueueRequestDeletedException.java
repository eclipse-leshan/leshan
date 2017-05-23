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
 *     Alexander Ellwein, Daniel Maier (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

/**
 * This special Exception is used to signal a kind of recoverable situation to the request sender. In case an
 * application has been send a request while an other thread deletes it, this exception is passed in the error callback.
 */
public class QueueRequestDeletedException extends Exception {

    /**
     * Creates a QueueRequestDeletedException.
     */
    public QueueRequestDeletedException() {
        super("queue request was deleted");
    }
}

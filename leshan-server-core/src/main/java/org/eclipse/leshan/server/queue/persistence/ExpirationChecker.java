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
package org.eclipse.leshan.server.queue.persistence;

import java.util.Calendar;

/**
 * Expiration checker is responsible to check expiration of a particular queue request (entity). An implementation of an
 * expiration checker may be specific to a particular QueuePersistence implementation.
 */
public interface ExpirationChecker {

    /**
     * Returns true, if a given queue request entity has expired, otherwise false.
     *
     * @param queueRequestEntity queue request entity to check
     * @return true, if expiration date is reached
     */
    boolean isSendExpirationReached(QueueRequestEntity queueRequestEntity);

    /**
     * Returns a calculated (absolute) timestamp for a queue request expiration.
     *
     * @param queueRequestEntity queue request entity
     * @return calculated timestamp of queue request expiration
     */
    Calendar getSendExpiration(QueueRequestEntity queueRequestEntity);
}

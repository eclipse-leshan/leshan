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

import org.eclipse.leshan.server.request.LwM2mRequestSender;

/**
 * QueueRequestSender is an extension to the LwM2mRequestSender, which is aware of LwM2M client's binding mode.
 * According to LWM2M specification, a client which supports Queue Mode, may go offline until its next registration
 * update or notification. Server will queue all the requests being given to QueueRequestSender and will send them upon
 * receiving an update/notify from client.
 */
public interface QueueRequestSender extends LwM2mRequestSender {

    /**
     * Returns QueueManagement for this request sender.
     *
     * @return queue management, which can be used for queue control/monitoring.
     */
    QueueManagement getQueueManagement();

}

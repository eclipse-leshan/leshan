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
 *     Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 *                              - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

/**
 * Possible states of a LWM2M client connected to a Server.
 *
 */
public enum ClientState {
    /** Client can be reached from server. */
    REACHABLE,
    /** Client is currently receiving queued messages */
    RECEIVING,
    /** Client cannot be reached from server */
    UNREACHABLE
}

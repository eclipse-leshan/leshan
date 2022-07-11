/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Bosch Software Innovations GmbH - initial API
 *     RISE SICS AB - added more features 
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import org.eclipse.leshan.server.registration.Registration;

/**
 * Tracks the status of each LWM2M client registered with Queue mode binding. Also ensures that the
 * {@link PresenceListener} are notified on state changes only for those LWM2M clients registered using Queue mode
 * binding.
 */
public interface PresenceService {

    /**
     * Add the listener to get notified when the LWM2M client goes online or offline.
     * 
     * @param listener target to notify
     */
    void addListener(PresenceListener listener);

    /**
     * Remove the listener previously added. This method has no effect if the given listener is not previously added.
     * 
     * @param listener target to be removed.
     */
    void removeListener(PresenceListener listener);

    /**
     * Returns the current state of a given LWM2M client registration.
     * 
     * @param registration the client's registration object.
     * @return true if the status is awake.
     */
    boolean isClientAwake(Registration registration);
}

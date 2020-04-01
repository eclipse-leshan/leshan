/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import java.util.Iterator;

/**
 * A service to access registered clients
 */
public interface RegistrationService {

    /**
     * Retrieves a registration by id.
     * 
     * @param id registration id
     * @return the matching registration or <code>null</code> if not found
     */
    Registration getById(String id);

    /**
     * Retrieves a registration by end-point.
     * 
     * @return the matching registration or <code>null</code> if not found
     */
    Registration getByEndpoint(String endpoint);

    /**
     * Returns an iterator over all registrations. There are no guarantees concerning the order in which the elements
     * are returned.
     *
     * @return an <tt>Iterator</tt> over registrations
     */
    Iterator<Registration> getAllRegistrations();

    /**
     * Adds a new listener to be notified with client registration events.
     * 
     * @param listener the listener to add
     */
    void addListener(RegistrationListener listener);

    /**
     * Removes a client registration listener.
     * 
     * @param listener the listener to be removed
     */
    void removeListener(RegistrationListener listener);
}

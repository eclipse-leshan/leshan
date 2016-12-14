/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.client;

/**
 * Listen for client registration events.
 */
public interface RegistrationListener {

    /**
     * Invoked when a new client has been registered on the server.
     *
     * @param registration
     */
    void registered(Registration registration);

    /**
     * Invoked when a client updated its registration.
     *
     * @param update the registration properties to update
     * @param updatedRegistration the registration after the update
     */
    void updated(RegistrationUpdate update, Registration updatedRegistration);

    /**
     * Invoked when a client has been unregistered from the server.
     *
     * @param registration
     */
    void unregistered(Registration registration);
}

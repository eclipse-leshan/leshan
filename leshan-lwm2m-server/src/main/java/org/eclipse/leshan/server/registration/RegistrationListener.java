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

/**
 * Listen for client registration events.
 * <p>
 * Those methods are called by the protocol stage thread pool, this means that execution MUST be done in a short delay,
 * if you need to do long time processing use a dedicated thread pool.
 */
public interface RegistrationListener {

    /**
     * Invoked when a new registration is created.
     *
     * @param registrationAddition registration added and eventually previous one replaced
     */
    void registered(RegistrationAddition registrationAddition);

    /**
     * Invoked when a client updates its registration.
     *
     * @param udaptedRegistration all registration updated.
     */
    void updated(UpdatedRegistration udaptedRegistration);

    /**
     * Invoked when a registration is removed from the server.
     *
     * @param deregistration all registration and observations which are removed.
     * @param expired <code>true</code> if the client has been unregistered because of its lifetime expiration and
     *        <code>false</code> otherwise
     * @param newReg the new registration when the registration is deleted because the client was already registered
     *        (same endpoint). <code>null</code> if the registration is deleted because of a Deregister request or an
     *        expiration.
     */
    void unregistered(Deregistration deregistration, boolean expired, Registration newReg);
}

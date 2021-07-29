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

import java.util.Collection;

import org.eclipse.leshan.core.observation.Observation;

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
     * @param registration the new registration
     * @param previousReg the previous registration if the client was already registered (same endpoint).
     *        <code>null</code> for a brand-new registration.
     * @param previousObsersations all the observations linked to the previous registration which have been passively
     *        cancelled. <code>null</code> for a brand-new registration.
     */
    void registered(Registration registration, Registration previousReg, Collection<Observation> previousObsersations);

    /**
     * Invoked when a client updates its registration.
     *
     * @param update the registration properties to update
     * @param updatedReg the registration after the update
     * @param previousReg the registration before the update
     */
    void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg);

    /**
     * Invoked when a registration is removed from the server.
     *
     * @param registration the deleted registration
     * @param observations all the observations linked to the deleted registration which has been passively cancelled
     * @param expired <code>true</code> if the client has been unregistered because of its lifetime expiration and
     *        <code>false</code> otherwise
     * @param newReg the new registration when the registration is deleted because the client was already registered
     *        (same endpoint). <code>null</code> if the registration is deleted because of a Deregister request or an
     *        expiration.
     */
    void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
            Registration newReg);
}

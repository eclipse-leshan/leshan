/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 * An UpdatedRegistration contains the registration before and after a registration update.
 *
 * @see RegistrationStore
 */
public class UpdatedRegistration {
    private final IRegistration previousRegistration;
    private final IRegistration updatedRegistration;

    public UpdatedRegistration(IRegistration previousRegistration, IRegistration updatedRegistration) {
        this.previousRegistration = previousRegistration;
        this.updatedRegistration = updatedRegistration;
    }

    /**
     * The registration before the update registration.
     */
    public IRegistration getPreviousRegistration() {
        return previousRegistration;
    }

    /**
     * The registration after the update registration.
     */
    public IRegistration getUpdatedRegistration() {
        return updatedRegistration;
    }
}

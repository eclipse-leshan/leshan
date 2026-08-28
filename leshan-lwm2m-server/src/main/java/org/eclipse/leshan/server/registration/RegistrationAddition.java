/*******************************************************************************
 * Copyright (c) 2026 Sierra Wireless and others.
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
 * An {@link RegistrationAddition} contains the news registration and eventually the previous one which has been
 * replaced.
 */
public class RegistrationAddition implements RegistrationModification {
    private final Registration newRegistration;
    private final Deregistration previousRegistration;

    public RegistrationAddition(Registration newRegistration) {
        this(newRegistration, null);
    }

    public RegistrationAddition(Registration newRegistration, Deregistration previousRegistration) {
        this.newRegistration = newRegistration;
        this.previousRegistration = previousRegistration;
    }

    public Registration getNewRegistration() {
        return newRegistration;
    }

    public Deregistration getPreviousRegistration() {
        return previousRegistration;
    }
}

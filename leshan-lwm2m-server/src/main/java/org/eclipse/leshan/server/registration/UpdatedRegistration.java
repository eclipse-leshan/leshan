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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An UpdatedRegistration contains the registration before and after a registration update.
 *
 * @see RegistrationStore
 */
public class UpdatedRegistration implements RegistrationModification {
    private final Registration previousRegistration;
    private final Registration newRegistration;
    private final List<UpdatedRegistration> updatedChildrenRegistration;
    private final RegistrationUpdate update;

    public UpdatedRegistration(Registration previousRegistration, Registration updatedRegistration,
            RegistrationUpdate update) {
        this(previousRegistration, updatedRegistration, null, update);
    }

    public UpdatedRegistration(Registration previousRegistration, Registration newRegistration,
            List<UpdatedRegistration> updatedChildrenRegistration, RegistrationUpdate update) {
        this.previousRegistration = previousRegistration;
        this.newRegistration = newRegistration;
        this.update = update;
        if (updatedChildrenRegistration == null || updatedChildrenRegistration.isEmpty()) {
            this.updatedChildrenRegistration = Collections.emptyList();
        } else {
            this.updatedChildrenRegistration = Collections
                    .unmodifiableList(new ArrayList<UpdatedRegistration>(updatedChildrenRegistration));
        }
    }

    /**
     * The registration before the update registration.
     */
    public Registration getPreviousRegistration() {
        return previousRegistration;
    }

    /**
     * The registration after the update registration.
     */
    public Registration getUpdatedRegistration() {
        return newRegistration;
    }

    /**
     * If the updated registration was a Gateway then this contains the list of children registration updated
     */
    public List<UpdatedRegistration> getChildrenUpdatedRegistration() {
        return updatedChildrenRegistration;
    }

    /**
     * @return {@link RegistrationUpdate} which trigger this modification or null if not trigger by a registration
     *         update
     */
    public RegistrationUpdate getUpdate() {
        return update;
    }
}

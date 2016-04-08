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
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.util.Validate;

/**
 * A Lightweight M2M request for removing the registration information from the LWM2M Server.
 */
public class DeregisterRequest implements UplinkRequest<DeregisterResponse> {

    private String registrationID = null;

    /**
     * Creates a request for removing the registration information from the LWM2M Server.
     *
     * @param registrationID the registration ID to remove
     */
    public DeregisterRequest(String registrationID) {
        Validate.notNull(registrationID);
        this.registrationID = registrationID;
    }

    public String getRegistrationID() {
        return registrationID;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }
}

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
package org.eclipse.leshan.core.response;

import org.eclipse.leshan.ResponseCode;

/**
 * Response to a client registration request
 */
public class RegisterResponse extends LwM2mResponse {

    private final String registrationID;

    public RegisterResponse(ResponseCode code) {
        super(code);
        this.registrationID = null;
    }

    public RegisterResponse(ResponseCode code, String registrationID) {
        super(code);
        this.registrationID = registrationID;
    }

    public String getRegistrationID() {
        return registrationID;
    }

    @Override
    public String toString() {
        return String.format("RegisterResponse [registrationID=%s, code=%s]", registrationID, code);
    }
}

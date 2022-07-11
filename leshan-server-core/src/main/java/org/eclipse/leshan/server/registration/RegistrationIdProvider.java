/*******************************************************************************
 * Copyright (c) 2018 NTELS and others.
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
 *     Rokwoon Kim (contracted with NTELS) - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import org.eclipse.leshan.core.request.RegisterRequest;

public interface RegistrationIdProvider {

    /**
     * Returns the registrationId using for location-path option values on response of Register operation.
     * 
     * @param registerRequest the client's registration request information.
     * @return registrationId
     */
    String getRegistrationId(RegisterRequest registerRequest);

}

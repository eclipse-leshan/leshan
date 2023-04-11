/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.server.gateway;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;

public interface GatewayService {

    /**
     * Register an "IoT device", a device communicating thru a gateway (see object 25 specification)
     *
     * @param gatewayRegId the registration identifier of the gateway used for communicating
     * @param endpoint this Iot device endpoint
     * @param prefix the prefix used for communicating on the gateway
     * @param objectLinks the list of supported object
     * @return <code>true</code> if the device is registered successfully
     */
    public boolean registerIotDevice(String gatewayRegId, String endpoint, String prefix, Link[] objectLinks);

    /**
     * Update the registration of an "IoT device", a device communicating thru a gateway (see object 25 specification)
     *
     * @param gatewayRegUpdate the registration of the gateway used for communicating
     * @param iotDeviceregistrationId the registration identifier of the registration to update
     * @param endpoint this Iot device endpoint
     * @param objectLinks the list of supported object (if updated)
     * @return the registration updated and the original one
     */
    public UpdatedRegistration updateIotDeviceRegistration(RegistrationUpdate gatewayRegUpdate,
            String iotDeviceregistrationId, String endpoint, Link[] objectLinks);
}

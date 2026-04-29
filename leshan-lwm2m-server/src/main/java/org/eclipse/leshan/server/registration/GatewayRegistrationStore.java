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

import java.util.List;

/**
 * A {@link RegistrationStore} which can handle LWM2M Gateway and so {@link EndDeviceRegistration}
 */
public interface GatewayRegistrationStore extends RegistrationStore {

    /**
     * Replace all current children of given gateway by the new list of {@link EndDeviceRegistration}.
     * <p>
     * Then return a list of modification (added, removed or updated registration).
     */
    List<RegistrationModification> replaceEndDeviceRegistrations(DeviceRegistration gateway,
            List<EndDeviceRegistration> endDevices);
}

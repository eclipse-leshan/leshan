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
package org.eclipse.leshan.server.bootstrap;

import org.eclipse.leshan.core.request.Identity;

/**
 * A store containing the bootstrap information to be sent to the devices.
 */
public interface BootstrapConfigStore {

    /**
     * Get the bootstrap configuration to apply to the device identified by the given parameters.
     * 
     * @param endpoint the endpoint of the device.
     * @param deviceIdentity the {@link Identity} the device.
     * @param session the current {@link BootstrapSession}.
     * @return the {@link BootstrapConfig} to apply.
     */
    BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session);
}

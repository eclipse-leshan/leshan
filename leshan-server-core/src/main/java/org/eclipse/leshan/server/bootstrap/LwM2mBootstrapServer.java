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
package org.eclipse.leshan.server.bootstrap;

import org.eclipse.leshan.server.security.BootstrapSecurityStore;

/**
 * 
 * A Lightweight M2M server in charge of handling device bootstrap on the /bs resource.
 *
 */
public interface LwM2mBootstrapServer {

    /**
     * Access to the bootstrap configuration store. It's used for sending configuration to the devices initiating a
     * bootstrap.
     * 
     * @return the store containing configuration to apply to each devices.
     */
    BootstrapConfigStore getBoostrapStore();

    /**
     * security store used for DTLS authentication on the bootstrap resource.
     * 
     * @return the security store containing data used to authenticate devices.
     */
    BootstrapSecurityStore getBootstrapSecurityStore();

    /**
     * Start the LWM2M bootstrap server.
     */
    void start();

    /**
     * Stop the LWM2M bootstrap server. Server should be restarted later. All resources are stopped but not released.
     */
    void stop();

    /**
     * Destroy the LWM2M bootstrap server. All resources must be released. Server can not be restarted.
     */
    void destroy();

}

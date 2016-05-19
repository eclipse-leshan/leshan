/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions;

import org.eclipse.leshan.server.californium.impl.LeshanServer;

/**
 * Interface for dynamic server extensions. A server extension must implement this interface.
 */
public interface LeshanServerExtension {
    /**
     * Setup server extension.
     * 
     * @param lwServer LWM2M server
     * @param configuration configuration parameters for extension
     * @param manager extensions manager
     */
    void setup(LeshanServer lwServer, ExtensionConfig configuration, LeshanServerExtensionsManager manager);

    /**
     * Start extension. Called after the LWM2M server is started.
     */
    void start();

    /**
     * Stop extension.
     */
    void stop();
}

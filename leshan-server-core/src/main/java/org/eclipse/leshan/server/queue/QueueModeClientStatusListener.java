/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *     Bosch Software Innovations GmbH - initial API
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

/**
 * A listener aware of the status of LWM2M Client using queue mode binding.
 *
 */
public interface QueueModeClientStatusListener {

    /**
     * this method is invoked when the LWM2M client with the given endpoint goes offline
     * 
     * @param endpoint of the client registered.
     */
    void onClientOffline(String endpoint);

    /**
     * this method is invoked when the LWM2M client with the given endpoint came online
     * 
     * @param endpoint of the client registered.
     */
    void onClientOnline(String endpoint);
}

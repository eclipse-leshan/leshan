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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client;

import java.util.Collection;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;

public interface LwM2mClient {

    /**
     * Starts the client (bind port, start to listen CoAP messages).
     */
    public void start();

    /**
     * Stops the client, i.e. unbinds it from all ports. Frees as much system resources as possible to still be able to
     * be started.
     */
    public void stop();

    /**
     * Destroys the client, i.e. unbinds from all ports and frees all system resources.
     */
    void destroy();

    Collection<LwM2mObjectEnabler> getObjectEnablers();

}
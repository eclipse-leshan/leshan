/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;

/**
 * Represent a single Bootstrapping session.
 * 
 * Should be created by {@link BootstrapSessionManager} implementations in
 * {@link BootstrapSessionManager#begin(String,Identity)}.
 */
public interface BootstrapSession {

    /**
     * @return the endpoint of the LwM2M client.
     */
    String getEndpoint();

    /**
     * @return the network identity of the LwM2M client.
     */
    Identity getIdentity();

    /**
     * @return true if the LwM2M client is authorized to start a bootstrap session.
     */
    boolean isAuthorized();

    /**
     * @return the content format to use on write request during this bootstrap session.
     */
    ContentFormat getContentFormat();

}

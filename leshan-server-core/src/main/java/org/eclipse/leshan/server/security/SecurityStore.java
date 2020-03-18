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
package org.eclipse.leshan.server.security;

/**
 * A store for {@link SecurityInfo}.
 */
public interface SecurityStore {

    /**
     * Returns the security information for a given end-point.
     * 
     * @param endpoint the client LWM2M end-point
     * @return the security information of <code>null</code> if not found.
     */
    SecurityInfo getByEndpoint(String endpoint);

    /**
     * Returns the security information for a PSK identity.
     * 
     * @param pskIdentity the PSK identity of the client
     * @return the security information of <code>null</code> if not found.
     */
    SecurityInfo getByIdentity(String pskIdentity);

}

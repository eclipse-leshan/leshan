/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import java.util.List;

/**
 * A store containing data needed to authenticate clients.
 */
public interface BootstrapSecurityStore {

    /**
     * Returns all acceptable security information for a given end-point.
     * <p>
     * Generally, only 1 securityInfo is returned for a given endpoint but there is rare use cases where severals
     * credentials could be associated to 1 client.
     * 
     * @param endpoint the client end-point
     * @return the security information of <code>null</code> if not found.
     */
    List<SecurityInfo> getAllByEndpoint(String endpoint);

    /**
     * Returns the security information for a PSK identity.
     * 
     * @param pskIdentity the PSK identity of the client
     * @return the security information of <code>null</code> if not found.
     */
    SecurityInfo getByIdentity(String pskIdentity);
}

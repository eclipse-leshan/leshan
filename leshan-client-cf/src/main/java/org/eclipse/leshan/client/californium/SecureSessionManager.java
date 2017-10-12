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
 *    Achim Kraus (Bosch Software Innovations GmbH) - initial implementation.
 ******************************************************************************/
package org.eclipse.leshan.client.californium;

import java.net.InetSocketAddress;

/**
 * Manage secure session.
 * 
 * Resume secure session or establish a new connection with a new handshake.
 */
public interface SecureSessionManager {

    /**
     * Force update secure session for provided peer.
     * 
     * @param peer
     *            address of peer
     */
    void forceResumeSessionFor(InetSocketAddress peer);

    /**
     * Get session ID for provided peer.
     * 
     * @param peer
     *            address of peer
     * @return session ID, or {@code null}, if not available.
     */
    String getSessionIdFor(InetSocketAddress peer);

}

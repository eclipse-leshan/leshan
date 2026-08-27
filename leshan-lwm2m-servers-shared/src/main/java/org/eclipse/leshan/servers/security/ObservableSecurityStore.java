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
package org.eclipse.leshan.servers.security;

/**
 * A {@link SecurityStore} which can be observed. Its is used by Leshan library to remove secure connection and so force
 * handshake when {@link SecurityInfo} is removed because it was compromised. See
 * {@link SecurityStoreListener#securityInfoRemoved(boolean, SecurityInfo...)}
 */
public interface ObservableSecurityStore extends SecurityStore {

    /**
     * Adds a new {@link SecurityStoreListener} to this store.
     */
    void addListener(SecurityStoreListener listener);

    /**
     * Removes the given {@link SecurityStoreListener} from the listeners of this store .
     */
    void removeListener(SecurityStoreListener listener);
}

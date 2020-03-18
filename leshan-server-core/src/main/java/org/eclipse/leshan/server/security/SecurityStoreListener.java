/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
 * A Listener for {@link SecurityStore}
 */
public interface SecurityStoreListener {
    /**
     * Called when {@link SecurityInfo} are removed.
     * 
     * @param infosAreCompromised True if info are compromised and should not be used immediately
     * @param infos Array of removed {@link SecurityInfo}
     */
    void securityInfoRemoved(boolean infosAreCompromised, SecurityInfo... infos);
}

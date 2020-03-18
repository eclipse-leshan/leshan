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

import java.util.Collection;

public interface EditableSecurityStore extends SecurityStore {

    /**
     * Returns the {@link SecurityInfo} for all end-points.
     * 
     * @return an unmodifiable collection of {@link SecurityInfo}
     */
    Collection<SecurityInfo> getAll();

    /**
     * Registers new security information for a client end-point.
     * 
     * @param info the new security information
     * @return the {@link SecurityInfo} previously stored for the end-point or <code>null</code> if there was no
     *         security information for the end-point.
     * @throws NonUniqueSecurityInfoException if some identifiers (PSK identity, RPK public key...) are not unique among
     *         all end-points.
     */
    SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException;

    /**
     * Removes the security information for a given end-point.
     * 
     * @param endpoint the client end-point
     * @param infosAreCompromised if the {@link SecurityInfo} removed should be considered as compromised and so must
     *        not be used anymore immediately.
     * @return the removed {@link SecurityInfo} or <code>null</code> if no info for the end-point.
     */
    SecurityInfo remove(String endpoint, boolean infosAreCompromised);

    /**
     * Set a Listener for this store.
     * 
     * @param listener the security store listener
     */
    void setListener(SecurityStoreListener listener);
}

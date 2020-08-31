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
package org.eclipse.leshan.server.bootstrap;

/**
 * An editable {@link BootstrapConfigurationStore}.
 */
public interface EditableBootstrapConfigurationStore extends BootstrapConfigurationStore {

    /**
     * Add a new bootstrap configuration for a client end-point.
     * 
     * @param endpoint The client endpoint name to which we want to set the given bootstrap configuration.
     * @param config The configuration to apply for the given client.
     * @throws InvalidConfigurationException if {@link BootstrapConfiguration} is not valid.
     */
    void add(String endpoint, BootstrapConfiguration config) throws InvalidConfigurationException;

    /**
     * Removes the bootstrap Configuration for a given end-point.
     * 
     * @param endpoint The client endpoint to which we want to remove the configuration.
     * @return the removed {@link BootstrapConfiguration} or <code>null</code> if no bootstrap configuration for this
     *         client.
     */
    BootstrapConfiguration remove(String endpoint);
}

/*******************************************************************************
 * Copyright (c) 2014-2015 Sierra Wireless and others.
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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.bsserver;

/**
 * Check a BootstrapConfig is correct. This is a complex process, we need to check if the different objects are in
 * coherence with each other.
 */
public interface ConfigurationChecker {

    /**
     * Verify if the {@link BootstrapConfig} is valid and consistent.
     * <p>
     * Raise a {@link InvalidConfigurationException} if config is not OK.
     *
     * @param config the bootstrap configuration to check.
     * @throws InvalidConfigurationException if bootstrap configuration is not invalid.
     */
    void verify(BootstrapConfig config) throws InvalidConfigurationException;
}

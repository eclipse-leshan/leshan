/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
package org.eclipse.leshan.client.californium.impl;

import java.net.InetSocketAddress;

import org.eclipse.californium.scandium.dtls.credentialsstore.CredentialsConfiguration;
import org.eclipse.californium.scandium.dtls.credentialsstore.CredentialsStore;
import org.eclipse.leshan.client.object.SecurityObjectUtil;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityObjectCredentialsStore implements CredentialsStore {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityObjectCredentialsStore.class);

    private final LwM2mObjectEnabler securityEnabler;

    /**
     * Warning : The securityEnabler should not contains 2 or more entries with the same identity. This is not a LWM2M
     * specification constraint but an implementation limitation.
     */
    public SecurityObjectCredentialsStore(LwM2mObjectEnabler securityEnabler) {
        this.securityEnabler = securityEnabler;
    }

    @Override
    public CredentialsConfiguration getCredentialsConfiguration(InetSocketAddress inetAddress) {
        // Get security Instance by address
        LwM2mObjectInstance securityInstance = SecurityObjectUtil.getSecurityInstance(securityEnabler, inetAddress);

        if (securityInstance == null) {
            LOG.warn("No secutiry object instance for {}", inetAddress);
            return null;
        }

        // Create credentialsConfig from security instance
        return new SecurityInstanceCredentialsConfig(securityInstance);
    }
}

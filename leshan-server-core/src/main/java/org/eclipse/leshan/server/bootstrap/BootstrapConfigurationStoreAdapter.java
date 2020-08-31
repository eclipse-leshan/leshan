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

import org.eclipse.leshan.core.request.Identity;

/**
 * It allows to adapt the deprecated {@link BootstrapConfigStore} to the new API {@link BootstrapConfigurationStore}.
 * You can use it to adapt but it is clearly recommanded to rather directly implements
 * {@link BootstrapConfigurationStore} and maybe used {@link BootstrapUtil} if you still want to handle
 * {@link BootstrapConfig}.
 */
@SuppressWarnings("deprecation")
public class BootstrapConfigurationStoreAdapter implements BootstrapConfigurationStore {

    private BootstrapConfigStore internalStore;

    public BootstrapConfigurationStoreAdapter(BootstrapConfigStore store) {
        this.internalStore = store;
    }

    @Override
    public BootstrapConfiguration get(String endpoint, Identity deviceIdentity, BootstrapSession session) {
        BootstrapConfig bootstrapConfig = internalStore.get(endpoint, deviceIdentity, session);
        if (bootstrapConfig == null)
            return null;

        return new BootstrapConfiguration(BootstrapUtil.toRequests(bootstrapConfig, session.getContentFormat()));
    }
}

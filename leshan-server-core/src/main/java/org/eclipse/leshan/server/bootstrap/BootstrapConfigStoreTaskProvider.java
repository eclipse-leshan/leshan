/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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

import java.util.HashMap;
import java.util.List;

import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * An implementation of {@link BootstrapTaskProvider} which use a {@link BootstrapConfigStore} to know which requests to
 * send during a {@link BootstrapSession}.
 */
public class BootstrapConfigStoreTaskProvider implements BootstrapTaskProvider {

    private BootstrapConfigStore store;

    public BootstrapConfigStoreTaskProvider(BootstrapConfigStore store) {
        this.store = store;
    }

    @Override
    public Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponse) {

        BootstrapConfig config = store.get(session.getEndpoint(), session.getIdentity(), session);
        if (config == null)
            return null;

        Tasks tasks = new Tasks();
        // create requests from config
        tasks.requestsToSend = BootstrapUtil.toRequests(config, session.getContentFormat());

        // We add model for Security(0), Server(0) and ACL(2) which are the only one supported by BootstrapConfig
        // We use default 1.0 model as currently BootstrapConfig support only this model version (which should be
        // compatible with models of LWM2M v1.1 but without new resources)
        tasks.supportedObjects = new HashMap<>();
        tasks.supportedObjects.put(0, "1.0");
        tasks.supportedObjects.put(1, "1.0");
        tasks.supportedObjects.put(2, "1.0");

        return tasks;
    }
}

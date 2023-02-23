/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStoreTaskProvider;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;

/**
 * A custom BootstrapConfigStoreTaskProvider which allow to start bootstrap session with a custom
 * {@link BootstrapDownlinkRequest}
 *
 */
public class TestBootstrapConfigStoreTaskProvider extends BootstrapConfigStoreTaskProvider {

    private BootstrapDownlinkRequest<?> firstCustomRequest;

    public TestBootstrapConfigStoreTaskProvider(BootstrapConfigStore configStore) {
        super(configStore);
    }

    @Override
    public Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponses) {
        if (previousResponses == null && firstCustomRequest != null) {
            Tasks tasks = new Tasks();
            tasks.requestsToSend = new ArrayList<>(1);
            tasks.requestsToSend.add(firstCustomRequest);
            tasks.last = false;
            tasks.supportedObjects = new HashMap<>();
            tasks.supportedObjects.put(0, "1.1");
            tasks.supportedObjects.put(1, "1.1");
            tasks.supportedObjects.put(2, "1.0");
            return tasks;
        } else {
            return super.getTasks(session, null);
        }
    }

    public void startBootstrapSessionWith(BootstrapDownlinkRequest<?> request) {
        this.firstCustomRequest = request;
    }
};

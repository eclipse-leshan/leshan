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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link BootstrapTaskProvider} which use a {@link BootstrapConfigStore} to know which requests to
 * send during a {@link BootstrapSession}.
 */
public class BootstrapConfigStoreTaskProvider implements BootstrapTaskProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapConfigStoreTaskProvider.class);

    private BootstrapConfigStore store;

    public BootstrapConfigStoreTaskProvider(BootstrapConfigStore store) {
        this.store = store;
    }

    @Override
    public Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponse) {

        BootstrapConfig config = store.get(session.getEndpoint(), session.getIdentity(), session);
        if (config == null)
            return null;

        if (previousResponse == null && shouldStartWithDiscover(config)) {
            Tasks tasks = new Tasks();
            tasks.requestsToSend = new ArrayList<>(1);
            tasks.requestsToSend.add(new BootstrapDiscoverRequest());
            tasks.last = false;
            return tasks;
        } else {
            Tasks tasks = new Tasks();

            // handle bootstrap discover response
            if (previousResponse != null) {
                BootstrapDiscoverResponse response = (BootstrapDiscoverResponse) previousResponse.get(0);
                if (!response.isSuccess()) {
                    LOG.warn(
                            "Bootstrap Discover return error {} : unable to continue bootstrap session with autoIdForSecurityObject mode. {}",
                            response, session);
                    return null;
                }

                Integer bootstrapServerInstanceId = findBootstrapServerInstanceId(response.getObjectLinks());
                if (bootstrapServerInstanceId == null) {
                    LOG.warn(
                            "Unable to find bootstrap server instance in Security Object (0) in response {}: unable to continue bootstrap session with autoIdForSecurityObject mode. {}",
                            response, session);
                    return null;
                }

                // create requests from config
                tasks.requestsToSend = BootstrapUtil.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat(),
                        bootstrapServerInstanceId);
            } else {
                // create requests from config
                tasks.requestsToSend = BootstrapUtil.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat());

            }

            // We add model for Security(0), Server(0) and ACL(2) which are the only one supported by BootstrapConfig
            tasks.supportedObjects = new HashMap<>();
            tasks.supportedObjects.put(0, "1.1");
            tasks.supportedObjects.put(1, "1.1");
            tasks.supportedObjects.put(2, "1.0");

            return tasks;
        }
    }

    protected boolean shouldStartWithDiscover(BootstrapConfig config) {
        return config.autoIdForSecurityObject;
    }

    protected Integer findBootstrapServerInstanceId(LwM2mLink[] objectLinks) {
        for (LwM2mLink link : objectLinks) {
            if (link.getPath().isObjectInstance() //
                    && link.getPath().getObjectId() == 0 //
                    && !link.getAttributes().contains(LwM2mAttributes.SHORT_SERVER_ID)) {
                // the instance without ssid associated is the bootstrap server one.
                return link.getPath().getObjectInstanceId();
            }
        }
        return null;
    }
}

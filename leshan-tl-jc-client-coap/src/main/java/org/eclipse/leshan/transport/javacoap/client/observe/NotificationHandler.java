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
package org.eclipse.leshan.transport.javacoap.client.observe;

import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.listener.ObjectsListener;
import org.eclipse.leshan.core.node.LwM2mPath;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Method;
import com.mbed.coap.utils.Service;

public class NotificationHandler implements ObjectsListener {

    private final Service<CoapRequest, CoapResponse> responseProvider;
    private final ObserversManager observersManager;

    public NotificationHandler(Service<CoapRequest, CoapResponse> responseProvider, ObserversManager observersManager) {
        this.observersManager = observersManager;
        this.responseProvider = responseProvider;
    }

    @Override
    public void resourceChanged(LwM2mPath... paths) {
        observersManager.sendObservation((observeRequest) -> {

            if (observeRequest.getMethod() == Method.GET) {
                // handle "single observation"
                LwM2mPath observedURI = new LwM2mPath(observeRequest.options().getUriPath());
                for (LwM2mPath resourceChangedURI : paths) {
                    if (resourceChangedURI.startWith(observedURI)) {
                        return true;
                    }
                }
            } else if (observeRequest.getMethod() == Method.FETCH) {
                // handle "composite observation"
                List<LwM2mPath> observerdURIs = observeRequest.getTransContext()
                        .getOrDefault(LwM2mKeys.LESHAN_OBSERVED_PATHS, Collections.emptyList());
                for (LwM2mPath observedURI : observerdURIs) {
                    for (LwM2mPath resourceChangedURI : paths) {
                        if (resourceChangedURI.startWith(observedURI)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }, responseProvider);
    }

    @Override
    public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {
    }

    @Override
    public void objectInstancesRemoved(LwM2mObjectEnabler object, int... instanceIds) {
    }

    @Override
    public void objectAdded(LwM2mObjectEnabler object) {
    }

    @Override
    public void objectRemoved(LwM2mObjectEnabler object) {
    }
}

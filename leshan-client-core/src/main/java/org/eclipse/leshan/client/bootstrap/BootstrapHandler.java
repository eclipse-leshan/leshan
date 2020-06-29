/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *******************************************************************************/
package org.eclipse.leshan.client.bootstrap;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.SendableResponse;

/**
 * Handle bootstrap session state.
 */
public class BootstrapHandler {

    private boolean bootstrapping = false;
    private CountDownLatch bootstrappingLatch = new CountDownLatch(1);

    private final Map<Integer, LwM2mObjectEnabler> objects;

    public BootstrapHandler(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        objects = objectEnablers;
    }

    public synchronized SendableResponse<BootstrapFinishResponse> finished(ServerIdentity server,
            BootstrapFinishRequest finishedRequest) {
        if (bootstrapping) {
            // only if the request is from the bootstrap server
            if (!server.isLwm2mBootstrapServer()) {
                return new SendableResponse<>(BootstrapFinishResponse.badRequest("not from a bootstrap server"));
            }
            // TODO delete bootstrap server (see 5.2.5.2 Bootstrap Delete)

            Runnable whenSent = new Runnable() {
                @Override
                public void run() {
                    bootstrappingLatch.countDown();
                }
            };

            return new SendableResponse<>(BootstrapFinishResponse.success(), whenSent);
        } else {
            return new SendableResponse<>(BootstrapFinishResponse.badRequest("no pending bootstrap session"));
        }
    }

    public synchronized BootstrapDeleteResponse delete(ServerIdentity server, BootstrapDeleteRequest deleteRequest) {
        if (bootstrapping) {
            // Only if the request is from the bootstrap server
            if (!server.isLwm2mBootstrapServer()) {
                return BootstrapDeleteResponse.badRequest("not from a bootstrap server");
            }

            // Delete all device management server
            for (LwM2mObjectEnabler enabler : objects.values()) {
                enabler.delete(server, new BootstrapDeleteRequest(enabler.getId()));
            }

            return BootstrapDeleteResponse.success();
        } else {
            return BootstrapDeleteResponse.badRequest("no pending bootstrap session");
        }
    }

    public synchronized boolean tryToInitSession() {
        if (!bootstrapping) {
            bootstrappingLatch = new CountDownLatch(1);
            bootstrapping = true;
            return true;
        }
        return false;
    }

    public synchronized boolean isBootstrapping() {
        return bootstrapping;
    }

    public boolean waitBoostrapFinished(long timeInSeconds) throws InterruptedException {
        return bootstrappingLatch.await(timeInSeconds, TimeUnit.SECONDS);
    }

    public synchronized void closeSession() {
        bootstrappingLatch = null;
        bootstrapping = false;
    }

    /**
     * @since 1.1
     */
    public BootstrapDiscoverResponse discover(ServerIdentity identity, BootstrapDiscoverRequest request) {
        if (!identity.isLwm2mBootstrapServer()) {
            return BootstrapDiscoverResponse.badRequest("not a bootstrap server");
        }

        LwM2mPath path = request.getPath();
        if (path.isRoot()) {
            // Manage discover on object
            Link[] ObjectLinks = LinkFormatHelper.getBootstrapClientDescription(objects.values());
            return BootstrapDiscoverResponse.success(ObjectLinks);
        }
        return BootstrapDiscoverResponse.badRequest("invalid path");
    }
}

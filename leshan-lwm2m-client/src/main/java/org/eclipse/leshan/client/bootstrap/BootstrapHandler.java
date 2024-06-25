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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.client.bootstrap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
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
    // last session state (null means no error)
    private volatile List<String> lastConsistencyError = null;

    private final Map<Integer, LwM2mObjectEnabler> objects;
    private final BootstrapConsistencyChecker checker;
    private final LinkFormatHelper linkFormatHelper;

    public BootstrapHandler(Map<Integer, LwM2mObjectEnabler> objectEnablers, BootstrapConsistencyChecker checker,
            LinkFormatHelper linkFormatHelper) {
        objects = objectEnablers;
        this.checker = checker;
        this.linkFormatHelper = linkFormatHelper;
    }

    public synchronized SendableResponse<BootstrapFinishResponse> finished(LwM2mServer server,
            BootstrapFinishRequest finishedRequest) {
        if (bootstrapping) {
            // only if the request is from the bootstrap server
            if (!server.isLwm2mBootstrapServer()) {
                return new SendableResponse<>(BootstrapFinishResponse.badRequest("not from a bootstrap server"));
            }

            Runnable whenSent = new Runnable() {
                @Override
                public void run() {
                    CountDownLatch countDownLatch = bootstrappingLatch;
                    // Latch can be null if we sent event is raised after bootstrap session is closed (e.g. if
                    // session timeout)
                    if (countDownLatch != null) {
                        bootstrappingLatch.countDown();
                    }
                }
            };

            // check consistency state of the client
            lastConsistencyError = checker.checkconfig(objects);
            if (lastConsistencyError == null) {
                return new SendableResponse<>(BootstrapFinishResponse.success(), whenSent);
            } else {
                // TODO rollback configuration.
                // see https://github.com/eclipse/leshan/issues/968
                return new SendableResponse<>(BootstrapFinishResponse.notAcceptable(lastConsistencyError.toString()),
                        whenSent);
            }

        } else {
            return new SendableResponse<>(BootstrapFinishResponse.badRequest("no pending bootstrap session"));
        }
    }

    public synchronized BootstrapDeleteResponse delete(LwM2mServer server, BootstrapDeleteRequest deleteRequest) {
        if (bootstrapping) {
            // Only if the request is from the bootstrap server
            if (!server.isLwm2mBootstrapServer()) {
                return BootstrapDeleteResponse.badRequest("not from a bootstrap server");
            }

            // Delete all device management server
            for (LwM2mObjectEnabler enabler : objects.values()) {
                enabler.delete(server, deleteRequest);
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
            lastConsistencyError = null;
            return true;
        }
        return false;
    }

    public synchronized boolean isBootstrapping() {
        return bootstrapping;
    }

    public boolean waitBootstrapFinished(long timeInSeconds) throws InterruptedException, InvalidStateException {
        boolean finished = bootstrappingLatch.await(timeInSeconds, TimeUnit.SECONDS);
        if (finished) {
            if (lastConsistencyError != null) {
                throw new InvalidStateException(
                        String.format("Invalid Bootstrap state : %s", lastConsistencyError.toString()));
            }
        }
        return finished;
    }

    public synchronized void closeSession() {
        bootstrappingLatch = null;
        bootstrapping = false;
    }

    /**
     * @since 1.1
     */
    public BootstrapDiscoverResponse discover(LwM2mServer server, BootstrapDiscoverRequest request) {
        if (!server.isLwm2mBootstrapServer()) {
            return BootstrapDiscoverResponse.badRequest("not a bootstrap server");
        }

        LwM2mPath path = request.getPath();
        if (path.isRoot()) {
            // Manage discover on object
            LwM2mLink[] ObjectLinks = linkFormatHelper.getBootstrapClientDescription(objects.values());
            return BootstrapDiscoverResponse.success(ObjectLinks);
        }
        return BootstrapDiscoverResponse.badRequest("invalid path");
    }
}

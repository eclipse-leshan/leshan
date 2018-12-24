/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *******************************************************************************/
package org.eclipse.leshan.client.servers;

import static org.eclipse.leshan.LwM2mId.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.SendableResponse;

/**
 * Handle bootstrap session state.
 */
public class BootstrapHandler {

    private boolean bootstrapping = false;
    private CountDownLatch bootstrappingLatch = new CountDownLatch(1);

    private ServerInfo bootstrapServerInfo;
    private final Map<Integer, LwM2mObjectEnabler> objects;

    public BootstrapHandler(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        objects = objectEnablers;
    }

    public synchronized SendableResponse<BootstrapFinishResponse> finished(ServerIdentity identity,
            BootstrapFinishRequest finishedRequest) {
        if (bootstrapping) {
            // only if the request is from the bootstrap server
            if (!isBootstrapServer(identity)) {
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

    public synchronized BootstrapDeleteResponse delete(ServerIdentity identity, BootstrapDeleteRequest deleteRequest) {
        if (bootstrapping) {
            // Only if the request is from the bootstrap server
            if (!isBootstrapServer(identity)) {
                return BootstrapDeleteResponse.badRequest("not from a bootstrap server");
            }

            // The spec say that delete on "/" should delete all the existing Object Instances - except LWM2M
            // Bootstrap Server Account, (see 5.2.5.2 Bootstrap Delete)
            // For now we only remove security and server object.

            // Delete all device management server
            LwM2mObjectEnabler serverObject = objects.get(SERVER);
            for (Integer instanceId : serverObject.getAvailableInstanceIds()) {
                serverObject.delete(identity, new DeleteRequest(SERVER, instanceId));
            }

            // Delete all security instance (except bootstrap one)
            // TODO do not delete bootstrap server (see 5.2.5.2 Bootstrap Delete)
            LwM2mObjectEnabler securityObject = objects.get(SECURITY);
            for (Integer instanceId : securityObject.getAvailableInstanceIds()) {
                securityObject.delete(identity, new DeleteRequest(SECURITY, instanceId));
            }

            return BootstrapDeleteResponse.success();
        } else {
            return BootstrapDeleteResponse.badRequest("no pending bootstrap session");
        }
    }

    public synchronized boolean tryToInitSession(ServerInfo bootstrapServerInfo) {
        if (!bootstrapping) {
            this.bootstrapServerInfo = bootstrapServerInfo;
            bootstrappingLatch = new CountDownLatch(1);
            bootstrapping = true;
            return true;
        }
        return false;
    }

    public boolean waitBoostrapFinished(long timeInSeconds) throws InterruptedException {
        return bootstrappingLatch.await(timeInSeconds, TimeUnit.SECONDS);
    }

    public synchronized void closeSession() {
        bootstrapServerInfo = null;
        bootstrappingLatch = null;
        bootstrapping = false;
    }

    /**
     * @return <code>true</code> if the given request sender identity is the bootstrap server (same IP address)
     */
    public synchronized boolean isBootstrapServer(Identity identity) {
        if (bootstrapServerInfo == null) {
            return false;
        }

        return bootstrapServerInfo.getAddress().getAddress() != null
                && bootstrapServerInfo.getAddress().getAddress().equals(identity.getPeerAddress().getAddress());
    }
}

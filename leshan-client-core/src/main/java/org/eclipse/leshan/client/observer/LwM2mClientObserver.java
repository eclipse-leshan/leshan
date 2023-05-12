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
 *******************************************************************************/
package org.eclipse.leshan.client.observer;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;

/**
 * Allow to observer the registration life cycle of a LwM2m client.
 */
public interface LwM2mClientObserver {
    // ============== Bootstrap =================

    void onBootstrapStarted(LwM2mServer bsserver, BootstrapRequest request);

    void onBootstrapSuccess(LwM2mServer bsserver, BootstrapRequest request);

    void onBootstrapFailure(LwM2mServer bsserver, BootstrapRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause);

    void onBootstrapTimeout(LwM2mServer bsserver, BootstrapRequest request);

    // ============== Registration =================

    void onRegistrationStarted(LwM2mServer server, RegisterRequest request);

    void onRegistrationSuccess(LwM2mServer server, RegisterRequest request, String registrationID);

    void onRegistrationFailure(LwM2mServer server, RegisterRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause);

    void onRegistrationTimeout(LwM2mServer server, RegisterRequest request);

    // ============== Registration Update =================

    void onUpdateStarted(LwM2mServer server, UpdateRequest request);

    void onUpdateSuccess(LwM2mServer server, UpdateRequest request);

    void onUpdateFailure(LwM2mServer server, UpdateRequest request, ResponseCode responseCode, String errorMessage,
            Exception cause);

    void onUpdateTimeout(LwM2mServer server, UpdateRequest request);

    // ============== Deregistration Update =================

    void onDeregistrationStarted(LwM2mServer server, DeregisterRequest request);

    void onDeregistrationSuccess(LwM2mServer server, DeregisterRequest request);

    void onDeregistrationFailure(LwM2mServer server, DeregisterRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause);

    void onDeregistrationTimeout(LwM2mServer server, DeregisterRequest request);

    // ============== Unexpected Error Handling =================

    void onUnexpectedError(Throwable unexpectedError);
}

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

import org.eclipse.leshan.client.servers.ServerIdentity;
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

    void onBootstrapStarted(ServerIdentity bsserver, BootstrapRequest request);

    void onBootstrapSuccess(ServerIdentity bsserver, BootstrapRequest request);

    void onBootstrapFailure(ServerIdentity bsserver, BootstrapRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause);

    void onBootstrapTimeout(ServerIdentity bsserver, BootstrapRequest request);

    // ============== Registration =================

    void onRegistrationStarted(ServerIdentity server, RegisterRequest request);

    void onRegistrationSuccess(ServerIdentity server, RegisterRequest request, String registrationID);

    void onRegistrationFailure(ServerIdentity server, RegisterRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause);

    void onRegistrationTimeout(ServerIdentity server, RegisterRequest request);

    // ============== Registration Update =================

    void onUpdateStarted(ServerIdentity server, UpdateRequest request);

    void onUpdateSuccess(ServerIdentity server, UpdateRequest request);

    void onUpdateFailure(ServerIdentity server, UpdateRequest request, ResponseCode responseCode, String errorMessage,
            Exception cause);

    void onUpdateTimeout(ServerIdentity server, UpdateRequest request);

    // ============== Deregistration Update =================

    void onDeregistrationStarted(ServerIdentity server, DeregisterRequest request);

    void onDeregistrationSuccess(ServerIdentity server, DeregisterRequest request);

    void onDeregistrationFailure(ServerIdentity server, DeregisterRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause);

    void onDeregistrationTimeout(ServerIdentity server, DeregisterRequest request);

    // ============== Unexpected Error Handling =================

    void onUnexpectedError(Throwable unexpectedError);
}

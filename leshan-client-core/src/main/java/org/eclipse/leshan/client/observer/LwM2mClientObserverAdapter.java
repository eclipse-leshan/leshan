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
 * An abstract adapter class for observing registration life cycle. The methods in this class are empty. This class
 * exists as convenience for creating client observer objects.
 */
public class LwM2mClientObserverAdapter implements LwM2mClientObserver {

    @Override
    public void onBootstrapStarted(ServerIdentity bsserver, BootstrapRequest request) {
    }

    @Override
    public void onBootstrapSuccess(ServerIdentity bsserver, BootstrapRequest request) {
    }

    @Override
    public void onBootstrapFailure(ServerIdentity bsserver, BootstrapRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause) {
    }

    @Override
    public void onBootstrapTimeout(ServerIdentity bsserver, BootstrapRequest request) {
    }

    @Override
    public void onRegistrationStarted(ServerIdentity server, RegisterRequest request) {
    }

    @Override
    public void onRegistrationSuccess(ServerIdentity server, RegisterRequest request, String registrationID) {
    }

    @Override
    public void onRegistrationFailure(ServerIdentity server, RegisterRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause) {
    }

    @Override
    public void onRegistrationTimeout(ServerIdentity server, RegisterRequest request) {
    }

    @Override
    public void onUpdateStarted(ServerIdentity server, UpdateRequest request) {
    }

    @Override
    public void onUpdateSuccess(ServerIdentity server, UpdateRequest request) {
    }

    @Override
    public void onUpdateFailure(ServerIdentity server, UpdateRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause) {
    }

    @Override
    public void onUpdateTimeout(ServerIdentity server, UpdateRequest request) {
    }

    @Override
    public void onDeregistrationStarted(ServerIdentity server, DeregisterRequest request) {
    }

    @Override
    public void onDeregistrationSuccess(ServerIdentity server, DeregisterRequest request) {
    }

    @Override
    public void onDeregistrationFailure(ServerIdentity server, DeregisterRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause) {
    }

    @Override
    public void onDeregistrationTimeout(ServerIdentity server, DeregisterRequest request) {
    }

    @Override
    public void onUnexpectedError(Throwable unexpectedError) {
    }
}

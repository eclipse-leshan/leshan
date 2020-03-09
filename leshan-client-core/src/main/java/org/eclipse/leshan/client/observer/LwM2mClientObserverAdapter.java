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
 *******************************************************************************/
package org.eclipse.leshan.client.observer;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.servers.Server;
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
    public void onBootstrapStarted(Server bsserver, BootstrapRequest request) {
    }

    @Override
    public void onBootstrapSuccess(Server bsserver, BootstrapRequest request) {
    }

    @Override
    public void onBootstrapFailure(Server bsserver, BootstrapRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause) {
    }

    @Override
    public void onBootstrapTimeout(Server bsserver, BootstrapRequest request) {
    }

    @Override
    public void onRegistrationStarted(Server server, RegisterRequest request) {
    }

    @Override
    public void onRegistrationSuccess(Server server, RegisterRequest request, String registrationID) {
    }

    @Override
    public void onRegistrationFailure(Server server, RegisterRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause) {
    }

    @Override
    public void onRegistrationTimeout(Server server, RegisterRequest request) {
    }

    @Override
    public void onUpdateStarted(Server server, UpdateRequest request) {
    }

    @Override
    public void onUpdateSuccess(Server server, UpdateRequest request) {
    }

    @Override
    public void onUpdateFailure(Server server, UpdateRequest request, ResponseCode responseCode, String errorMessage,
            Exception cause) {
    }

    @Override
    public void onUpdateTimeout(Server server, UpdateRequest request) {
    }

    @Override
    public void onDeregistrationStarted(Server server, DeregisterRequest request) {
    }

    @Override
    public void onDeregistrationSuccess(Server server, DeregisterRequest request) {
    }

    @Override
    public void onDeregistrationFailure(Server server, DeregisterRequest request, ResponseCode responseCode,
            String errorMessage, Exception cause) {
    }

    @Override
    public void onDeregistrationTimeout(Server server, DeregisterRequest request) {
    }
}
